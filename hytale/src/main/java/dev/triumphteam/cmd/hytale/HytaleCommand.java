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

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import dev.triumphteam.cmd.core.Command;
import dev.triumphteam.cmd.core.SubCommand;
import dev.triumphteam.cmd.core.annotation.Default;
import dev.triumphteam.cmd.core.exceptions.CommandExecutionException;
import dev.triumphteam.cmd.core.message.MessageKey;
import dev.triumphteam.cmd.core.message.MessageRegistry;
import dev.triumphteam.cmd.core.message.context.DefaultMessageContext;
import dev.triumphteam.cmd.core.sender.SenderMapper;
import dev.triumphteam.cmd.hytale.message.HytaleMessageKey;
import dev.triumphteam.cmd.hytale.message.NoPermissionMessageContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Synchronous Hytale command that extends Hytale's CommandBase.
 * Executes on the main server thread - suitable for quick operations.
 */
public final class HytaleCommand<S> extends CommandBase implements Command<S, HytaleSubCommand<S>> {

    private final MessageRegistry<S> messageRegistry;
    private final SenderMapper<CommandSender, S> senderMapper;
    private final CommandPermission permission;
    private final HytaleCommandProcessor<S> processor;

    private final Map<String, HytaleSubCommand<S>> subCommands = new HashMap<>();
    private final Map<String, HytaleSubCommand<S>> subCommandAliases = new HashMap<>();

    public HytaleCommand(
            final @NotNull String name, 
            final @NotNull HytaleCommandProcessor<S> processor
    ) {
        super(name, processor.getDescription());
        this.messageRegistry = processor.getRegistryContainer().getMessageRegistry();
        this.senderMapper = processor.getSenderMapper();
        this.permission = processor.getPermission();
        this.processor = processor;
        
        // Set permission if available
        if (permission != null && !permission.getNodes().isEmpty()) {
            requirePermission(permission.getNodes().get(0));
        }

        // Get arguments
        setAllowsExtraArguments(true);

        // Get default subcommand and register its aliases to the Hytale command
        final HytaleSubCommand<S> defaultSubCommand = getDefaultSubCommand();

        if (defaultSubCommand != null) {
            for (final String alias : defaultSubCommand.getHytaleCommand().getAliases()) {
                addAliases(alias);
            }
        }
    }

    @Override
    protected void executeSync(@NotNull CommandContext context) {
        final CommandSender sender = context.sender();
        final String[] args = parseArgumentsFromInput(context.getInputString());

        // Check permissions
        if (permission != null && !CommandPermission.hasPermission(sender, permission)) {
            final S mappedSender = senderMapper.map(sender);
            messageRegistry.sendMessage(HytaleMessageKey.NO_PERMISSION, mappedSender, 
                new NoPermissionMessageContext(getName(), "", permission));
            return;
        }

        try {
            final S mappedSender = senderMapper.map(sender);
            
            // Determine subcommand
            final String subCommandName = args.length > 0 ? args[0] : "";
            HytaleSubCommand<S> subCommand = subCommands.get(subCommandName);
            
            if (subCommand == null) {
                subCommand = subCommandAliases.get(subCommandName);
            }
            
            if (subCommand == null) {
                subCommand = getDefaultSubCommand();
            }

            if (subCommand == null) {
                messageRegistry.sendMessage(MessageKey.UNKNOWN_COMMAND, mappedSender,
                    new DefaultMessageContext(getName(), subCommandName));
                return;
            }

            final List<String> commandArgs = Arrays.asList(
                !subCommand.isDefault() ? Arrays.copyOfRange(args, 1, args.length) : args
            );

            subCommand.execute(mappedSender, commandArgs);
        } catch (final CommandExecutionException exception) {
            // Handle command execution exception
            throw new RuntimeException("Command execution failed", exception);
        }
    }

    @Override
    public void addSubCommand(final @NotNull String name, final @NotNull HytaleSubCommand<S> subCommand) {
        if (subCommands.containsKey(name)) {
            return;
        }

        subCommands.put(name, subCommand);
        
        // Register the subcommand in Hytale's command system
        super.addSubCommand(subCommand.getHytaleCommand());
    }

    @Override
    public void addSubCommandAlias(final @NotNull String alias, final @NotNull HytaleSubCommand<S> subCommand) {
        subCommandAliases.put(alias, subCommand);
        subCommand.getHytaleCommand().addAliases(alias);
    }

    /**
     * Gets the default subcommand (one annotated with @Default).
     */
    public @Nullable HytaleSubCommand<S> getDefaultSubCommand() {
        return subCommands.values().stream()
                .filter(SubCommand::isDefault)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets all registered subcommands.
     */
    public @NotNull Map<String, HytaleSubCommand<S>> getAllSubCommands() {
        return Collections.unmodifiableMap(subCommands);
    }

    /**
     * Gets all subcommand aliases.
     */
    public @NotNull Map<String, HytaleSubCommand<S>> getAllSubCommandAliases() {
        return Collections.unmodifiableMap(subCommandAliases);
    }

    /**
     * Gets all subcommands that were discovered and created from the BaseCommand class.
     * This method returns all subcommands including their names and aliases.
     * 
     * @return A map containing all subcommand names and their corresponding HytaleSubCommand instances
     */
    public @NotNull Map<String, HytaleSubCommand<S>> getAllSubCommandsFromConstructor() {
        final Map<String, HytaleSubCommand<S>> allSubCommands = new HashMap<>();
        
        // Add all main subcommands
        allSubCommands.putAll(subCommands);
        
        // Add all aliases (with a reference to which subcommand they point to)
        allSubCommands.putAll(subCommandAliases);
        
        return Collections.unmodifiableMap(allSubCommands);
    }

    /**
     * Gets detailed information about all subcommands including their method names,
     * permissions, and other metadata from the processor.
     */
    public @NotNull List<SubCommandInfo> getDetailedSubCommandInfo() {
        final List<SubCommandInfo> subCommandInfos = new ArrayList<>();
        
        for (final Map.Entry<String, HytaleSubCommand<S>> entry : subCommands.entrySet()) {
            final HytaleSubCommand<S> subCommand = entry.getValue();
            final SubCommandInfo info = new SubCommandInfo(
                entry.getKey(),
                subCommand.isDefault(),
                subCommand.getHytaleCommand().getName(),
                subCommand.getHytaleCommand().getDescription(),
                new ArrayList<>(subCommand.getHytaleCommand().getAliases())
            );
            subCommandInfos.add(info);
        }
        
        return subCommandInfos;
    }

    /**
     * Gets the command permission for this command.
     */
    public @Nullable CommandPermission getCommandPermission() {
        return permission;
    }

    /**
     * Parses arguments from Hytale's raw input string.
     */
    private String[] parseArgumentsFromInput(final String input) {
        if (input == null || input.trim().isEmpty()) {
            return new String[0];
        }

        // Remove the command name from the input
        String[] parts = input.trim().split("\\s+");
        if (parts.length <= 1) {
            return new String[0];
        }
        
        return Arrays.copyOfRange(parts, 1, parts.length);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof HytaleCommand)) return false;
        HytaleCommand<?> that = (HytaleCommand<?>) obj;
        return getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public String toString() {
        return "HytaleCommand{sync, name='" + getName() + "'}";
    }

    /**
     * Information about a subcommand including its metadata.
     */
    public static class SubCommandInfo {
        private final String name;
        private final boolean isDefault;
        private final String hytaleCommandName;
        private final String description;
        private final List<String> aliases;

        public SubCommandInfo(
                final @NotNull String name,
                final boolean isDefault,
                final @NotNull String hytaleCommandName,
                final @NotNull String description,
                final @NotNull List<String> aliases
        ) {
            this.name = name;
            this.isDefault = isDefault;
            this.hytaleCommandName = hytaleCommandName;
            this.description = description;
            this.aliases = Collections.unmodifiableList(new ArrayList<>(aliases));
        }

        public @NotNull String getName() {
            return name;
        }

        public boolean isDefault() {
            return isDefault;
        }

        public @NotNull String getHytaleCommandName() {
            return hytaleCommandName;
        }

        public @NotNull String getDescription() {
            return description;
        }

        public @NotNull List<String> getAliases() {
            return aliases;
        }

        @Override
        public String toString() {
            return "SubCommandInfo{" +
                    "name='" + name + '\'' +
                    ", isDefault=" + isDefault +
                    ", hytaleCommandName='" + hytaleCommandName + '\'' +
                    ", description='" + description + '\'' +
                    ", aliases=" + aliases +
                    '}';
        }
    }
}