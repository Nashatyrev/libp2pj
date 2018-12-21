package org.libp2pj.p2pd;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Created by Anton Nashatyrev on 20.12.2018.
 */
public class AsyncDaemonExecutor {
    private final String domainSocketPath;

    public AsyncDaemonExecutor(String domainSocketPath) {
        this.domainSocketPath = domainSocketPath;
    }

    public <TRet> CompletableFuture<TRet> executeWithDaemon(
            Function<DaemonChannelHandler, CompletableFuture<TRet>> executor) {
        CompletableFuture<DaemonChannelHandler> daemonFut = getDaemon();
        return daemonFut
                .thenCompose(executor)
                .whenComplete((r, t) -> {
                    if (!daemonFut.isCompletedExceptionally()) {
                        try {
                            daemonFut.get().close();
                        } catch (Exception e) {}
                    }
                });
    }

    public CompletableFuture<DaemonChannelHandler> getDaemon() {
        return new ControlConnector().connect(domainSocketPath);
    }
}
