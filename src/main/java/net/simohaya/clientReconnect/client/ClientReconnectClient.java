package net.simohaya.clientReconnect.client;

import net.fabricmc.api.ClientModInitializer;
import net.simohaya.clientReconnect.ClientReconnect;

public class ClientReconnectClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        AutoReconnectHandler.register();
        ClientReconnect.LOGGER.info("ClientReconnect クライアント起動完了！");
    }
}