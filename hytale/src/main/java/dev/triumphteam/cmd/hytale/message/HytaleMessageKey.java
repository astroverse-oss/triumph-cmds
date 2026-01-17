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
package dev.triumphteam.cmd.hytale.message;

import dev.triumphteam.cmd.core.message.ContextualKey;
import dev.triumphteam.cmd.core.message.context.MessageContext;
import dev.triumphteam.cmd.core.message.context.DefaultMessageContext;
import dev.triumphteam.cmd.core.message.context.InvalidArgumentContext;
import org.jetbrains.annotations.NotNull;

/**
 * Default message keys for Hytale commands.
 */
public final class HytaleMessageKey<C extends MessageContext> extends ContextualKey<C> {

    // Default keys
    public static final HytaleMessageKey<NoPermissionMessageContext> NO_PERMISSION = of("no.permission", NoPermissionMessageContext.class);
    public static final HytaleMessageKey<MessageContext> PLAYER_ONLY = of("player.only", MessageContext.class);
    public static final HytaleMessageKey<MessageContext> CONSOLE_ONLY = of("console.only", MessageContext.class);

    private HytaleMessageKey(final @NotNull String key, final @NotNull Class<C> type) {
        super(key, type);
    }

    /**
     * Creates a new message key.
     */
    public static <C extends MessageContext> @NotNull HytaleMessageKey<C> of(
            final @NotNull String key, 
            final @NotNull Class<C> type
    ) {
        return new HytaleMessageKey<>(key, type);
    }
}