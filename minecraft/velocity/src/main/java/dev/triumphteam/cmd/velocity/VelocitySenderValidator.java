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

import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.triumphteam.cmd.core.SubCommand;
import dev.triumphteam.cmd.core.message.MessageRegistry;
import dev.triumphteam.cmd.core.message.context.DefaultMessageContext;
import dev.triumphteam.cmd.core.sender.SenderValidator;
import dev.triumphteam.cmd.velocity.message.VelocityMessageKey;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Simple mapper than returns itself.
 */
class VelocitySenderValidator implements SenderValidator<CommandSource> {

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Set<@NotNull Class<? extends @NotNull CommandSource>> getAllowedSenders() {
        return ImmutableSet.of(CommandSource.class, ConsoleCommandSource.class, Player.class);
    }

    @Override
    public boolean validate(
            final @NotNull MessageRegistry<CommandSource> messageRegistry,
            final @NotNull SubCommand<CommandSource> subCommand,
            final @NotNull CommandSource sender
    ) {
        final Class<? extends CommandSource> senderClass = subCommand.getSenderType();

        if (Player.class.isAssignableFrom(senderClass) && !(sender instanceof Player)) {
            messageRegistry.sendMessage(
                    VelocityMessageKey.PLAYER_ONLY,
                    sender,
                    new DefaultMessageContext(subCommand.getParentName(), subCommand.getName())
            );
            return false;
        }

        if (ConsoleCommandSource.class.isAssignableFrom(senderClass) && !(sender instanceof ConsoleCommandSource)) {
            messageRegistry.sendMessage(
                    VelocityMessageKey.CONSOLE_ONLY,
                    sender,
                    new DefaultMessageContext(subCommand.getParentName(), subCommand.getName())
            );
            return false;
        }

        return true;
    }
}
