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

import dev.triumphteam.cmd.core.message.context.AbstractMessageContext;
import dev.triumphteam.cmd.hytale.CommandPermission;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Message context for no permission errors in Hytale commands.
 */
public final class NoPermissionMessageContext extends AbstractMessageContext {

    private final CommandPermission permission;

    public NoPermissionMessageContext(
            final @NotNull String command,
            final @NotNull String subCommand,
            final @NotNull CommandPermission permission
    ) {
        super(command, subCommand);
        this.permission = permission;
    }

    public @NotNull List<@NotNull String> getNodes() {
        return permission.getNodes();
    }
}