/**
 * MIT License
 *
 * Copyright (c) 2019-2021 Matt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.triumphteam.cmd.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.CommandManager;
import dev.triumphteam.cmd.core.execution.ExecutionProvider;
import dev.triumphteam.cmd.core.execution.SyncExecutionProvider;
import dev.triumphteam.cmd.core.exceptions.CommandRegistrationException;
import dev.triumphteam.cmd.core.message.MessageKey;
import dev.triumphteam.cmd.core.registry.RegistryContainer;
import dev.triumphteam.cmd.core.sender.SenderMapper;
import dev.triumphteam.cmd.core.sender.SenderValidator;
import dev.triumphteam.cmd.velocity.message.VelocityMessageKey;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Velocity implementation of the command manager, adapting BukkitCommandManager to Velocity's API.
 */
public final class VelocityCommandManager<S> extends CommandManager<CommandSource, S> {

    private final ProxyServer proxy;
    private final RegistryContainer<S> registryContainer = new RegistryContainer<>();
    private final Map<String, VelocityCommand<S>> commands = new HashMap<>();

    private final ExecutionProvider syncExecutionProvider;
    private final ExecutionProvider asyncExecutionProvider;

    private final CommandPermission basePermission = null;

    private VelocityCommandManager(
            final @NotNull ProxyServer proxy,
            final @NotNull SenderMapper<CommandSource, S> senderMapper,
            final @NotNull SenderValidator<S> senderValidator
    ) {
        super(senderMapper, senderValidator);
        this.proxy = proxy;
        this.syncExecutionProvider = new SyncExecutionProvider();
        this.asyncExecutionProvider = new VelocityAsyncExecutionProvider(proxy);

        // Default argument resolvers
        registerArgument(Player.class,
                (source, arg) -> proxy.getPlayer(arg).orElse(null)
        );
        // Suggestion for online players
        registerSuggestion(Player.class,
                (source, context) -> proxy.getAllPlayers().stream()
                        .map(com.velocitypowered.api.proxy.Player::getUsername)
                        .collect(Collectors.toList())
        );
    }

    /**
     * Creates a manager with default sender (CommandSource).
     */
    public static @NotNull VelocityCommandManager<CommandSource> create(final @NotNull ProxyServer proxy) {
        VelocityCommandManager<CommandSource> manager = new VelocityCommandManager<>(
                proxy,
                SenderMapper.defaultMapper(),
                new VelocitySenderValidator()
        );
        setUpDefaults(manager);
        return manager;
    }

    /**
     * Creates a manager with custom sender mapper and validator.
     */
    public static <S> @NotNull VelocityCommandManager<S> create(
            final @NotNull ProxyServer proxy,
            final @NotNull SenderMapper<CommandSource, S> senderMapper,
            final @NotNull SenderValidator<S> senderValidator
    ) {
        return new VelocityCommandManager<>(proxy, senderMapper, senderValidator);
    }

    @Override
    public void registerCommand(final @NotNull BaseCommand baseCommand) {
        VelocityCommandProcessor<S> processor = new VelocityCommandProcessor<>(
                baseCommand,
                registryContainer,
                getSenderMapper(),
                getSenderValidator(),
                syncExecutionProvider,
                asyncExecutionProvider,
                basePermission
        );

        final VelocityCommand<S> command = commands.computeIfAbsent(processor.getName(), ignored -> createAndRegisterCommand(processor.getName(), processor));
        // Adding sub commands.
        processor.addSubCommands(command);

        processor.getAlias().forEach(it -> {
            final VelocityCommand<S> aliasCommand = commands.computeIfAbsent(it, ignored -> createAndRegisterCommand(it, processor));
            // Adding sub commands.
            processor.addSubCommands(aliasCommand);
        });
    }

    private @NotNull VelocityCommand<S> createAndRegisterCommand(final @NotNull String name, final @NotNull VelocityCommandProcessor<S> processor) {
        final VelocityCommand<S> command = new VelocityCommand<>(name, processor);

        // register with Velocity
        try {
            proxy.getCommandManager().register(processor.getName(), command, processor.getAlias().toArray(new String[0]));
        } catch (Exception ex) {
            throw new CommandRegistrationException(
                    "Failed to register Velocity command `/"+ command +"`");
        }

        return command;
    }

    @Override
    public void unregisterCommand(final @NotNull BaseCommand baseCommand) {
        // Velocity currently does not support unregistering commands at runtime
    }

    @Override
    protected @NotNull RegistryContainer<S> getRegistryContainer() {
        return registryContainer;
    }

    private static <S> void setUpDefaults(final @NotNull VelocityCommandManager<CommandSource> manager) {
        manager.registerMessage(MessageKey.UNKNOWN_COMMAND, (sender, context) ->
                sender.sendMessage(Component.text(
                        "Unknown command: '" + context.getCommand() + "'."
                ))
        );
        manager.registerMessage(MessageKey.TOO_MANY_ARGUMENTS, (sender, context) ->
                sender.sendMessage(Component.text("Too many arguments."))
        );
        manager.registerMessage(MessageKey.NOT_ENOUGH_ARGUMENTS, (sender, context) ->
                sender.sendMessage(Component.text("Not enough arguments."))
        );
        manager.registerMessage(MessageKey.INVALID_ARGUMENT, (sender, context) ->
                sender.sendMessage(Component.text(
                        "Invalid argument '" + context.getTypedArgument() + "' for type '" +
                                context.getArgumentType().getSimpleName() + "'."
                ))
        );
        manager.registerMessage(VelocityMessageKey.NO_PERMISSION, (sender, context) ->
                sender.sendMessage(Component.text(
                        "You do not have permission to perform this command. Required: '" +
                                context.getNodes() + "'."
                ))
        );
        manager.registerMessage(VelocityMessageKey.PLAYER_ONLY, (sender, context) ->
                sender.sendMessage(Component.text("This command can only be used by players."))
        );
        manager.registerMessage(VelocityMessageKey.CONSOLE_ONLY, (sender, context) ->
                sender.sendMessage(Component.text("This command can only be used by the console."))
        );
    }
}
