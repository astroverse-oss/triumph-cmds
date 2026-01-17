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

import com.velocitypowered.api.proxy.ProxyServer;
import dev.triumphteam.cmd.core.execution.ExecutionProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementation of asynchronous execution, not necessarily used in all platforms.
 */
public final class VelocityAsyncExecutionProvider implements ExecutionProvider {

    private final ProxyServer proxyServer;
    private final ExecutorService executorService;

    public VelocityAsyncExecutionProvider(final @NotNull ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                runnable -> new Thread(runnable, "TriumphAsyncExecutor")
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(final @NotNull Runnable command) {
        executorService.submit(() -> {
            try {
                proxyServer.getScheduler().buildTask(proxyServer, command).schedule();
            } catch (Exception e) {
                // Handle exceptions that may occur during task scheduling
                e.printStackTrace();
            }
        });
    }
}
