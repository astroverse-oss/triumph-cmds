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
import dev.triumphteam.cmd.core.BaseCommand;
import dev.triumphteam.cmd.core.execution.ExecutionProvider;
import dev.triumphteam.cmd.core.processor.AbstractCommandProcessor;
import dev.triumphteam.cmd.core.registry.RegistryContainer;
import dev.triumphteam.cmd.core.sender.SenderMapper;
import dev.triumphteam.cmd.core.sender.SenderValidator;
import dev.triumphteam.cmd.velocity.annotation.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

final class VelocityCommandProcessor<S> extends AbstractCommandProcessor<CommandSource, S, VelocitySubCommand<S>, VelocitySubCommandProcessor<S>> {

    private final CommandPermission basePermission;

    public VelocityCommandProcessor(
            final @NotNull BaseCommand baseCommand,
            final @NotNull RegistryContainer<S> registryContainer,
            final @NotNull SenderMapper<CommandSource, S> senderMapper,
            final @NotNull SenderValidator<S> senderValidator,
            final @NotNull ExecutionProvider syncExecutionProvider,
            final @NotNull ExecutionProvider asyncExecutionProvider,
            final @Nullable CommandPermission globalBasePermission
    ) {
        super(baseCommand, registryContainer, senderMapper, senderValidator, syncExecutionProvider, asyncExecutionProvider);

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
    protected @NotNull VelocitySubCommandProcessor<S> createProcessor(final @NotNull Method method) {
        return new VelocitySubCommandProcessor<>(
                getBaseCommand(),
                getName(),
                method,
                getRegistryContainer(),
                getSenderValidator(),
                basePermission
        );
    }

    @Override
    protected @NotNull VelocitySubCommand<S> createSubCommand(
            final @NotNull VelocitySubCommandProcessor<S> processor,
            final @NotNull ExecutionProvider executionProvider
    ) {
        return new VelocitySubCommand<>(processor, getName(), executionProvider);
    }

    static CommandPermission createPermission(
            final @Nullable CommandPermission parent,
            final @NotNull List<@NotNull String> nodes
    ) {
        return parent == null
                ? new CommandPermission(nodes)
                : parent.child(nodes);
    }
}
