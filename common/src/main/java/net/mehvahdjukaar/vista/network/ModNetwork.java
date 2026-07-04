package net.mehvahdjukaar.vista.network;

import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.vista.integration.create.SyncContraptionViewFinderPacket;

public class ModNetwork {

    public static void init() {
        NetworkHelper.addNetworkRegistration(ModNetwork::registerMessages, 2);
    }

    private static void registerMessages(NetworkHelper.RegisterMessagesEvent event) {
        event.registerBidirectional(SyncViewFinderPacket.CODEC);
        event.registerBidirectional(SyncContraptionViewFinderPacket.CODEC);
        event.registerServerBound(ServerBoundSyncWaveGatePacket.CODEC);
        event.registerClientBound(ClientBoundControlViewFinderPacket.CODEC);
        event.registerClientBound(ClientBoundSyncExtraChunksPacket.CODEC);
    }
}
