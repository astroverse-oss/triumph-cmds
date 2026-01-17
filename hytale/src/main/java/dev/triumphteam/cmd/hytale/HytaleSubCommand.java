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

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import dev.triumphteam.cmd.core.AbstractSubCommand;
import dev.triumphteam.cmd.core.execution.ExecutionProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Hytale implementation of a sub-command that integrates with Hytale's native command system.
 */
public final class HytaleSubCommand<S> extends AbstractSubCommand<S> {

    private final CommandPermission permission;
    private final boolean isAsync;
    private AbstractCommand hytaleCommand;

    public HytaleSubCommand(
            final @NotNull HytaleSubCommandProcessor<S> processor,
            final @NotNull String parentName,
            final @NotNull ExecutionProvider executionProvider
    ) {
        super(processor, parentName, executionProvider);
        this.permission = processor.getPermission();
        this.isAsync = processor.isAsync();
    }

    public CommandPermission getPermission() {
        return permission;
    }

    /**
     * Creates a native Hytale command for this subcommand.
     * This allows the subcommand to be registered directly with Hytale's command system.
     */
    public AbstractCommand getHytaleCommand() {
        if (hytaleCommand != null) {
            return hytaleCommand;
        }

        if (isAsync) {
            hytaleCommand = new AsyncHytaleSubCommandWrapper();
        } else {
            hytaleCommand = new SyncHytaleSubCommandWrapper();
        }

        return hytaleCommand;
    }

    /**
     * Synchronous wrapper for Hytale commands.
     */
    private class SyncHytaleSubCommandWrapper extends CommandBase {
        
        public SyncHytaleSubCommandWrapper() {
            super(HytaleSubCommand.this.getName(), "No description");
            
            // Set permission if available
            if (permission != null && !permission.getNodes().isEmpty()) {
                requirePermission(permission.getNodes().get(0));
            }

            setAllowsExtraArguments(true);
        }

        @Override
        protected void executeSync(@NotNull CommandContext context) {
            executeHytaleSubCommand(context);
        }
    }

    /**
     * Asynchronous wrapper for Hytale commands.
     */
    private class AsyncHytaleSubCommandWrapper extends AbstractAsyncCommand {
        
        public AsyncHytaleSubCommandWrapper() {
            super(HytaleSubCommand.this.getName(), "No description");
            
            // Set permission if available
            if (permission != null && !permission.getNodes().isEmpty()) {
                requirePermission(permission.getNodes().get(0));
            }

            setAllowsExtraArguments(true);
        }

        @Override
        protected @NotNull CompletableFuture<Void> executeAsync(@NotNull CommandContext context) {
            return CompletableFuture.runAsync(() -> executeHytaleSubCommand(context));
        }
    }

    /**
     * Common execution logic for both sync and async wrappers.
     */
    private void executeHytaleSubCommand(CommandContext context) {
        final CommandSender sender = context.sender();
        final String[] args = parseArgumentsFromInput(context.getInputString());
        
        // Remove the subcommand name from args
        String[] subArgs = args.length > 0 ? 
            Arrays.copyOfRange(args, 1, args.length) : new String[0];
        
        // Execute using framework's logic (this calls the annotated method)
        execute((S) sender, List.of(subArgs));
    }

    /**
     * Parses arguments from Hytale's raw input string.
     */
    private String[] parseArgumentsFromInput(final String input) {
        if (input == null || input.trim().isEmpty()) {
            return new String[0];
        }
        
        String[] parts = input.trim().split("\\s+");
        if (parts.length <= 1) {
            return new String[0];
        }
        
        // Remove command name, keep everything else
        return Arrays.copyOfRange(parts, 1, parts.length);
    }
}