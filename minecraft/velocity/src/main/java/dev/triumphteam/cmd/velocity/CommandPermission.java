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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Data holder for a command's permission,
 * including its default state and a description.
 *
 * Adapted for Velocity.
 */
public final class CommandPermission {

    private final List<String> nodes;
    private final boolean defaultAllow;

    /**
     * Constructs a new CommandPermission.
     *
     * @param nodes         the list of permission nodes
     */
    public CommandPermission(
            final @NotNull List<String> nodes
    ) {
        this.nodes = nodes;
        this.defaultAllow = nodes.isEmpty();
    }

    /**
     * Checks whether the given CommandSource has this permission.
     *
     * @param sender      the source executing the command
     * @param permission  the CommandPermission to check (nullable)
     * @return true if sender has permission or if permission is null
     */
    public static boolean hasPermission(
            final @NotNull CommandSource sender,
            final @Nullable CommandPermission permission
    ) {
        return permission == null || permission.hasPermission(sender);
    }

    /**
     * Creates a child permission by appending child node segments
     * to each parent node.
     *
     * @param childNodes    the list of child node suffixes
     * @return a new CommandPermission instance representing the child permission
     */
    public @NotNull CommandPermission child(
            final @NotNull List<String> childNodes
    ) {
        List<String> newNodes = this.nodes.stream()
                .flatMap(parent -> childNodes.stream().map(node -> parent + "." + node))
                .collect(Collectors.toList());

        return new CommandPermission(newNodes);
    }

    /**
     * Returns the list of permission nodes.
     *
     * @return the permission nodes
     */
    public @NotNull List<String> getNodes() {
        return nodes;
    }

    /**
     * Checks if the given CommandSource has at least one of the permission nodes.
     *
     * @param sender  the source executing the command
     * @return true if sender has any of the nodes or if nodes list is empty and defaultAllow is true
     */
    public boolean hasPermission(final @NotNull CommandSource sender) {
        if (nodes.isEmpty()) {
            return defaultAllow;
        }
        return nodes.stream().anyMatch(sender::hasPermission);
    }

    @Override
    public String toString() {
        return "CommandPermission{" +
                "nodes=" + nodes +
                ", defaultAllow=" + defaultAllow +
                '}';
    }
}
