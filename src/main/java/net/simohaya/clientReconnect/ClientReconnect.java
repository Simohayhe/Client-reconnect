package net.simohaya.clientReconnect;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientReconnect implements ModInitializer {

    public static final String MOD_ID = "client-reconnect";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("ClientReconnect 起動！");
    }
}