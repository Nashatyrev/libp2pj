package org.libp2pj;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Created by Anton Nashatyrev on 10.12.2018.
 */
public interface Transport extends Connector<Transport.Listener, Multiaddress> {


    @Override
    void dial(Multiaddress multiaddress,
              StreamHandler<Multiaddress> dialHandler);

    @Override
    CompletableFuture<Listener> listen(Multiaddress multiaddress,
                                       Supplier<StreamHandler<Multiaddress>> handlerFactory);

    interface Listener extends Closeable {

        @Override
        void close();

        Multiaddress getLocalMultiaddress();

        default CompletableFuture<Multiaddress> getPublicMultiaddress() {
            return CompletableFuture.completedFuture(getLocalMultiaddress());
        }
    }

}
