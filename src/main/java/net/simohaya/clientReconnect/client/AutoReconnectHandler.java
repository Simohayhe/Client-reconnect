package net.simohaya.clientReconnect.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AutoReconnectHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("client-reconnect");
    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private static ScheduledFuture<?> retryTask  = null;
    private static String lastServerHost          = null;
    private static int    lastServerPort          = 25565;
    private static int    retryCount              = 0;
    private static final int RETRY_INTERVAL_SECONDS = 5;

    public static void register() {
        // 接続時にサーバー情報を記録
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(() -> {
                if (client.getCurrentServerEntry() != null) {
                    ServerAddress addr = ServerAddress.parse(
                            client.getCurrentServerEntry().address);
                    lastServerHost = addr.getAddress();
                    lastServerPort = addr.getPort();
                    retryCount     = 0;
                    cancelRetry();
                    LOGGER.info("サーバー接続記録: {}:{}", lastServerHost, lastServerPort);
                }
            });
        });

        // 切断時に自動リトライ開始
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            client.execute(() -> {
                if (lastServerHost == null) return;

                // scheduleRetryより先にsetScreenする
                scheduleRetry(client);

                client.setScreen(new net.minecraft.client.gui.screen.DisconnectedScreen(
                        new TitleScreen(),
                        net.minecraft.text.Text.literal("切断されました"),
                        new net.minecraft.network.DisconnectionInfo(
                                net.minecraft.text.Text.literal("§e自動再接続します... 5秒後に再試行"),
                                java.util.Optional.empty(),
                                java.util.Optional.empty()
                        )
                ));
            });
        });
    }

    private static void scheduleRetry(MinecraftClient client) {
        cancelRetry();

        retryTask = scheduler.scheduleAtFixedRate(() -> {
            client.execute(() -> {
                // すでにゲームに入っていたらリトライ不要
                if (client.world != null) {
                    cancelRetry();
                    return;
                }

                retryCount++;
                LOGGER.info("再接続試行中... {}回目 ({}:{})",
                        retryCount, lastServerHost, lastServerPort);

                ServerInfo serverInfo = new ServerInfo(
                        "AutoReconnect",
                        lastServerHost + ":" + lastServerPort,
                        ServerInfo.ServerType.OTHER
                );
                ServerAddress address = new ServerAddress(lastServerHost, lastServerPort);

                try {
                    ConnectScreen.connect(
                            new TitleScreen(), client, address, serverInfo, false, null);
                    cancelRetry();
                } catch (Exception e) {
                    LOGGER.info("再接続失敗、次のリトライまで待機: {}", e.getMessage());
                }
            });
        }, RETRY_INTERVAL_SECONDS, RETRY_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private static void cancelRetry() {
        if (retryTask != null && !retryTask.isCancelled()) {
            retryTask.cancel(false);
            retryTask = null;
        }
    }
}