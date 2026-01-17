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
package dev.triumphteam.cmd.hytale;

import com.hypixel.hytale.builtin.buildertools.utils.Material;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.Command;
import dev.triumphteam.cmd.core.CommandManager;
import dev.triumphteam.cmd.core.execution.ExecutionProvider;
import dev.triumphteam.cmd.core.execution.SyncExecutionProvider;
import dev.triumphteam.cmd.core.message.MessageKey;
import dev.triumphteam.cmd.core.registry.RegistryContainer;
import dev.triumphteam.cmd.core.sender.SenderMapper;
import dev.triumphteam.cmd.core.sender.SenderValidator;
import dev.triumphteam.cmd.hytale.message.HytaleMessageKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hytale implementation of the command manager.
 * This module provides command handling support for Hytale servers.
 */
public final class HytaleCommandManager<S> extends CommandManager<CommandSender, S> {

    private final JavaPlugin plugin;
    private final RegistryContainer<S> registryContainer = new RegistryContainer<>();
    private final Map<String, Command<S, HytaleSubCommand<S>>> commands = new HashMap<>();

    private final ExecutionProvider syncExecutionProvider;
    private final ExecutionProvider asyncExecutionProvider;

    private final CommandPermission basePermission;

    private HytaleCommandManager(
            final @NotNull JavaPlugin plugin,
            final @NotNull SenderMapper<CommandSender, S> senderMapper,
            final @NotNull SenderValidator<S> senderValidator
    ) {
        super(senderMapper, senderValidator);
        this.plugin = plugin;
        this.syncExecutionProvider = new SyncExecutionProvider();
        this.asyncExecutionProvider = new HytaleAsyncExecutionProvider(plugin);

        this.basePermission = new CommandPermission(List.of(Universe.get().getBasePermission()));

        // Register basic argument resolvers for common types
        registerArgument(PlayerRef.class,
                (source, arg) -> Universe.get().getPlayerByUsername(arg, NameMatching.EXACT_IGNORE_CASE)
        );
        registerArgument(World.class,
                (source, arg) -> Universe.get().getWorld(arg)
        );
        registerArgument(Material.class,
                (source, arg) -> Material.fromKey(arg)
        );
        registerArgument(Material.class,
                (source, arg) -> Material.fromKey(arg)
        );
    }

    /**
     * Creates a manager with default sender (CommandSender).
     */
    @Contract("_ -> new")
    public static @NotNull HytaleCommandManager<CommandSender> create(final @NotNull JavaPlugin plugin) {
        HytaleCommandManager<CommandSender> manager = new HytaleCommandManager<>(
                plugin,
                SenderMapper.defaultMapper(),
                new HytaleSenderValidator()
        );
        setUpDefaults(manager);
        return manager;
    }

    public void pushCommands() {
        this.commands.forEach((name, command) -> {
            if (command instanceof HytaleCommand) {
                plugin.getCommandRegistry().registerCommand((HytaleCommand<S>) command);
            } else if (command instanceof HytaleAsyncCommand) {
                plugin.getCommandRegistry().registerCommand((HytaleAsyncCommand<S>) command);
            }
        });
    }

    /**
     * Creates a manager with custom sender mapper and validator.
     */
    @Contract("_, _, _ -> new")
    public static <S> @NotNull HytaleCommandManager<S> create(
            final @NotNull JavaPlugin plugin,
            final @NotNull SenderMapper<CommandSender, S> senderMapper,
            final @NotNull SenderValidator<S> senderValidator
    ) {
        HytaleCommandManager<S> manager = new HytaleCommandManager<>(plugin, senderMapper, senderValidator);
        manager.pushCommands();
        return manager;
    }

    @Override
    public void registerCommand(final @NotNull BaseCommand baseCommand) {
        HytaleCommandProcessor<S> processor = new HytaleCommandProcessor<>(
                baseCommand,
                registryContainer,
                getSenderMapper(),
                getSenderValidator(),
                syncExecutionProvider,
                asyncExecutionProvider,
                plugin,
                basePermission
        );

        final Command<S, HytaleSubCommand<S>> command = commands.computeIfAbsent(processor.getName(), ignored ->
            createAndRegisterCommand(processor.getName(), processor));

        // Adding sub commands
        processor.addSubCommands(command);

        // Register aliases
        processor.getAlias().forEach(alias -> {
            final Command<S, HytaleSubCommand<S>> aliasCommand = commands.computeIfAbsent(alias, ignored -> 
                createAndRegisterCommand(alias, processor));
            processor.addSubCommands(aliasCommand);
        });
    }

    private @NotNull Command<S, HytaleSubCommand<S>> createAndRegisterCommand(
            final @NotNull String name, 
            final @NotNull HytaleCommandProcessor<S> processor
    ) {
        return processor.createCommand(name);
    }

    @Override
    public void unregisterCommand(final @NotNull BaseCommand baseCommand) {
        // Unregistering commands is not supported in Hytale's API as of now.
    }

    @Override
    protected @NotNull RegistryContainer<S> getRegistryContainer() {
        return registryContainer;
    }

    /**
     * Sets up all the default values for the Bukkit implementation.
     *
     * @param manager The {@link HytaleCommandManager} instance to set up.
     */
    private static void setUpDefaults(final @NotNull HytaleCommandManager<CommandSender> manager) {
        manager.registerMessage(MessageKey.UNKNOWN_COMMAND, (sender, context) -> sender.sendMessage(Message.raw("Unknown command: `" + context.getCommand() + "`.")));
        manager.registerMessage(MessageKey.TOO_MANY_ARGUMENTS, (sender, context) -> sender.sendMessage(Message.raw("Invalid usage.")));
        manager.registerMessage(MessageKey.NOT_ENOUGH_ARGUMENTS, (sender, context) -> sender.sendMessage(Message.raw("Invalid usage.")));
        manager.registerMessage(MessageKey.INVALID_ARGUMENT, (sender, context) -> sender.sendMessage(Message.raw("Invalid argument `" + context.getTypedArgument() + "` for type `" + context.getArgumentType().getSimpleName() + "`.")));

        manager.registerMessage(HytaleMessageKey.NO_PERMISSION, (sender, context) -> sender.sendMessage(Message.raw("You do not have permission to perform this command. Permission needed: `" + context.getNodes() + "`.")));
        manager.registerMessage(HytaleMessageKey.PLAYER_ONLY, (sender, context) -> sender.sendMessage(Message.raw("This command can only be used by players.")));
        manager.registerMessage(HytaleMessageKey.CONSOLE_ONLY, (sender, context) -> sender.sendMessage(Message.raw("This command can only be used by the console.")));
    }
}