package io.grpc.netty;

import io.grpc.InternalLogId;
import io.grpc.internal.ServerListener;
import io.grpc.internal.ServerTransport;
import io.grpc.internal.ServerTransportListener;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class LogIdServerListenerDelegator implements ServerListenerDelegator {
    final Logger logger = LogManager.getLogger(this.getClass());

    @Override
    public ServerListener wrapServerListener(final ServerListener serverListener) {

        logger.info("ServerListener serverListener:{}", serverListener);

        final ServerListener delegate = new ServerListener() {
            @Override
            public ServerTransportListener transportCreated(ServerTransport transport) {

                final InternalLogId logId = transport.getLogId();
                if (logger.isDebugEnabled()) {
                    logger.debug("transportCreated:{} {}", transport, logId);
                }

                final ServerTransportListener serverTransportListener = serverListener.transportCreated(transport);

                return new LogIdAttachListener(serverTransportListener, logId.getId());
            }

            @Override
            public void serverShutdown() {
                serverListener.serverShutdown();
            }
        };
        return delegate;
    }
}
