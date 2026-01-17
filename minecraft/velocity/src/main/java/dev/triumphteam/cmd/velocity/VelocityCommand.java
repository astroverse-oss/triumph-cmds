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
import com.velocitypowered.api.command.SimpleCommand;
import dev.triumphteam.cmd.core.Command;
import dev.triumphteam.cmd.core.SubCommand;
import dev.triumphteam.cmd.core.annotation.Default;
import dev.triumphteam.cmd.core.exceptions.CommandExecutionException;
import dev.triumphteam.cmd.core.message.MessageKey;
import dev.triumphteam.cmd.core.message.MessageRegistry;
import dev.triumphteam.cmd.core.message.context.DefaultMessageContext;
import dev.triumphteam.cmd.core.sender.SenderMapper;
import dev.triumphteam.cmd.velocity.message.NoPermissionMessageContext;
import dev.triumphteam.cmd.velocity.message.VelocityMessageKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public final class VelocityCommand<S> implements SimpleCommand, Command<S, VelocitySubCommand<S>> {

    private final String name;
    private final MessageRegistry<S> messageRegistry;

    private final SenderMapper<CommandSource, S> senderMapper;

    private final Map<String, VelocitySubCommand<S>> subCommands = new HashMap<>();
    private final Map<String, VelocitySubCommand<S>> subCommandAliases = new HashMap<>();

    public VelocityCommand(final @NotNull String name, final @NotNull VelocityCommandProcessor<S> processor) {
        this.name = name;
        this.messageRegistry = processor.getRegistryContainer().getMessageRegistry();
        this.senderMapper = processor.getSenderMapper();
    }

    @Override
    public void addSubCommand(final @NotNull String name, final @NotNull VelocitySubCommand<S> subCommand) {
        subCommands.putIfAbsent(name, subCommand);
    }

    @Override
    public void addSubCommandAlias(final @NotNull String alias, final @NotNull VelocitySubCommand<S> subCommand) {
        subCommandAliases.putIfAbsent(alias, subCommand);
    }

    @Override
    public void execute(Invocation invocation) {
        final CommandSource sender = invocation.source();
        final String[] args = invocation.arguments();

        VelocitySubCommand<S> subCommand = getDefaultSubCommand();

        String subCommandName = "";
        if (args.length > 0) subCommandName = args[0].toLowerCase();
        if (subCommand == null || subCommandExists(subCommandName)) {
            subCommand = getSubCommand(subCommandName);
        }

        final S mappedSender = senderMapper.map(sender);
        if (mappedSender == null) {
            throw new CommandExecutionException("Invalid sender. Sender mapper returned null");
        }

        if (subCommand == null || (args.length > 0 && subCommand.isDefault() && !subCommand.hasArguments())) {
            messageRegistry.sendMessage(MessageKey.UNKNOWN_COMMAND, mappedSender, new DefaultMessageContext(getName(), subCommandName));
            return;
        }

        final CommandPermission permission = subCommand.getPermission();
        if (!CommandPermission.hasPermission(sender, permission)) {
            messageRegistry.sendMessage(VelocityMessageKey.NO_PERMISSION, mappedSender, new NoPermissionMessageContext(getName(), subCommand.getName(), permission));
            return;
        }

        final List<String> commandArgs = Arrays.asList(!subCommand.isDefault() ? Arrays.copyOfRange(args, 1, args.length) : args);

        subCommand.execute(mappedSender, commandArgs);
    }

    @Override
    public List<String> suggest(@NotNull Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        // Si no hay argumentos, sugerir todos los subcomandos disponibles
        if (args.length == 0) {
            return subCommands.entrySet().stream()
                    .filter(e -> !e.getValue().isDefault())
                    .filter(it -> {
                        final CommandPermission permission = it.getValue().getPermission();
                        return CommandPermission.hasPermission(sender, permission);
                    })
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        VelocitySubCommand<S> subCommand = getDefaultSubCommand();
        final String arg = args[0].toLowerCase();

        // Si solo estamos completando el primer argumento y no hay args para default
        if (args.length == 1 && (subCommand == null || !subCommand.hasArguments())) {
            return subCommands.entrySet().stream()
                    .filter(it -> !it.getValue().isDefault())
                    .filter(it -> it.getKey().startsWith(arg))
                    .filter(it -> {
                        final CommandPermission permission = it.getValue().getPermission();
                        return CommandPermission.hasPermission(sender, permission);
                    })
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        if (subCommandExists(arg)) subCommand = getSubCommand(arg);
        if (subCommand == null) return emptyList();

        final CommandPermission permission = subCommand.getPermission();
        if (!CommandPermission.hasPermission(sender, permission)) return emptyList();

        final S mappedSender = senderMapper.map(sender);
        if (mappedSender == null) {
            return emptyList();
        }

        final List<String> commandArgs = Arrays.asList(args);
        return subCommand.getSuggestions(mappedSender, !subCommand.isDefault() ? commandArgs.subList(1, commandArgs.size()) : commandArgs);
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(suggest(invocation));
    }

    /**
     * Gets a default command if present.
     *
     * @return A default SubCommand.
     */
    private @Nullable VelocitySubCommand<S> getDefaultSubCommand() {
        return subCommands.get(Default.DEFAULT_CMD_NAME);
    }

    /**
     * Used in order to search for the given {@link SubCommand <CommandSource>} in the {@link #subCommandAliases}
     *
     * @param key the String to look for the {@link SubCommand<CommandSource>}
     * @return the {@link SubCommand<CommandSource>} for the particular key or NULL
     */
    private @Nullable VelocitySubCommand<S> getSubCommand(final @NotNull String key) {
        final VelocitySubCommand<S> subCommand = subCommands.get(key);
        if (subCommand != null) return subCommand;
        return subCommandAliases.get(key);
    }

    /**
     * Checks if a SubCommand with the specified key exists.
     *
     * @param key the Key to check for
     * @return whether a SubCommand with that key exists
     */
    private boolean subCommandExists(final @NotNull String key) {
        return subCommands.containsKey(key) || subCommandAliases.containsKey(key);
    }

    /**
     * Returns the name of this command.
     *
     * @return the command name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the message registry used by this command.
     *
     * @return the message registry
     */
    @Override
    public boolean hasPermission(Invocation invocation) {
        final CommandSource sender = invocation.source();
        final String[] args = invocation.arguments();

        VelocitySubCommand<S> subCommand = getDefaultSubCommand();
        String subCommandName = "";
        if (args.length > 0) subCommandName = args[0].toLowerCase();
        if (subCommand == null || subCommandExists(subCommandName)) {
            subCommand = getSubCommand(subCommandName);
        }

        if (subCommand == null) return true;

        final CommandPermission permission = subCommand.getPermission();
        return CommandPermission.hasPermission(sender, permission);
    }
}
