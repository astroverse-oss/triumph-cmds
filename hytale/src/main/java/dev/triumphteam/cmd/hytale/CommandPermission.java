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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Data holder for the command's permission in Hytale.
 * Provides a simple permission system that can be expanded as the Hytale API evolves.
 */
public final class CommandPermission {

    private final List<String> nodes;
    private final boolean defaultAllow;

    public CommandPermission(
            final @NotNull List<@NotNull String> nodes
    ) {
        this.nodes = nodes;
        this.defaultAllow = nodes.isEmpty();
    }

    /**
     * Checks whether the {@link CommandSender} has the (nullable) {@link CommandPermission}.
     * <p>
     * The method simply checks if any of the following two things are {@code true}:
     * <ul>
     *     <li>The permission is {@code null}</li>
     *     <li>The sender has the permission</li>
     * </ul>
     *
     * @param sender The main command sender.
     * @param permission The permission.
     * @return Whether the sender has permission to run the command.
     */
    public static boolean hasPermission(
            final @NotNull CommandSender sender,
            final @Nullable CommandPermission permission
    ) {
        return permission == null || permission.hasPermission(sender);
    }

    public @NotNull CommandPermission child(
            final @NotNull List<@NotNull String> nodes
    ) {
        final List<String> newNodes = this.nodes.stream()
                .flatMap(parent -> nodes.stream().map(node -> parent + "." + node))
                .collect(Collectors.toList());

        return new CommandPermission(newNodes);
    }

    /**
     * Gets the permission nodes.
     *
     * @return The permission nodes.
     */
    public @NotNull List<@NotNull String> getNodes() {
        return nodes;
    }

    /**
     * Checks if the {@link CommandSender} has the permission to run the command.
     * <p>
     * Note: This implementation assumes CommandSender has a hasPermission method.
     * If not available, this may need to be implemented differently based on the
     * actual Hytale permission API.
     *
     * @param sender The main command sender.
     * @return Whether the sender has permission to run the command.
     */
    /**
     * Checks if the given CommandSource has at least one of the permission nodes.
     *
     * @param sender  the source executing the command
     * @return true if sender has any of the nodes or if nodes list is empty and defaultAllow is true
     */
    public boolean hasPermission(final @NotNull CommandSender sender) {
        if (nodes.isEmpty()) {
            return defaultAllow;
        }
        return nodes.stream().anyMatch(sender::hasPermission);
    }
}