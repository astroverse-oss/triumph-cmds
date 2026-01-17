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
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.Command;
import dev.triumphteam.cmd.core.annotation.Async;
import dev.triumphteam.cmd.core.execution.ExecutionProvider;
import dev.triumphteam.cmd.core.message.MessageRegistry;
import dev.triumphteam.cmd.core.processor.AbstractCommandProcessor;
import dev.triumphteam.cmd.core.registry.RegistryContainer;
import dev.triumphteam.cmd.core.sender.SenderMapper;
import dev.triumphteam.cmd.core.sender.SenderValidator;

import dev.triumphteam.cmd.hytale.annotation.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Processes BaseCommand classes for Hytale and extracts command metadata.
 */
public final class HytaleCommandProcessor<S> extends AbstractCommandProcessor<CommandSender, S, HytaleSubCommand<S>, HytaleSubCommandProcessor<S>> {

    private final JavaPlugin plugin;
    private final CommandPermission basePermission;
    private final ExecutionProvider asyncExecutionProvider;

    public HytaleCommandProcessor(
            final @NotNull BaseCommand baseCommand,
            final @NotNull RegistryContainer<S> registryContainer,
            final @NotNull SenderMapper<CommandSender, S> senderMapper,
            final @NotNull SenderValidator<S> senderValidator,
            final @NotNull ExecutionProvider syncExecutionProvider,
            final @NotNull ExecutionProvider asyncExecutionProvider,
            final @NotNull JavaPlugin plugin,
            final @Nullable CommandPermission globalBasePermission
    ) {
        super(baseCommand, registryContainer, senderMapper, senderValidator, syncExecutionProvider, asyncExecutionProvider);
        this.plugin = plugin;
        this.asyncExecutionProvider = asyncExecutionProvider;

        final Permission annotation = getBaseCommand().getClass().getAnnotation(Permission.class);
        if (annotation == null) {
            this.basePermission = null;
            return;
        }

        this.basePermission = createPermission(
                globalBasePermission,
                Arrays.stream(annotation.value()).collect(Collectors.toList())
        );
    }

    @Override
    protected @NotNull HytaleSubCommandProcessor<S> createProcessor(final @NotNull Method method) {
        return new HytaleSubCommandProcessor<S>(
                getBaseCommand(),
                getName(),
                method,
                getRegistryContainer(),
                getSenderValidator(),
                basePermission
        );
    }

    @Override
    protected @NotNull HytaleSubCommand<S> createSubCommand(
            final @NotNull HytaleSubCommandProcessor<S> processor,
            final @NotNull ExecutionProvider executionProvider
    ) {
        return new HytaleSubCommand<>(processor, getName(), executionProvider);
    }

    static CommandPermission createPermission(
            final @Nullable CommandPermission parent,
            final @NotNull List<@NotNull String> nodes
    ) {
        return parent == null
                ? new CommandPermission(nodes)
                : parent.child(nodes);
    }

    /**
     * Gets the plugin instance.
     */
    public @NotNull JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Creates a Hytale command instance based on whether the command class contains async subcommands.
     * 
     * @return Either a HytaleCommand (sync) or HytaleAsyncCommand (async)
     */
    public Command<S, HytaleSubCommand<S>> createCommand(final @NotNull String name) {
        // Check if any subcommand methods have @Async annotation
        boolean hasAsyncSubCommands = Arrays.stream(getBaseCommand().getClass().getDeclaredMethods())
                .anyMatch(method -> method.isAnnotationPresent(Async.class));

        if (hasAsyncSubCommands) {
            return new HytaleAsyncCommand<>(name, this);
        } else {
            return new HytaleCommand<>(name, this);
        }
    }

    /**
     * Gets the base permission for this command.
     */
    public @Nullable CommandPermission getPermission() {
        return basePermission;
    }
}