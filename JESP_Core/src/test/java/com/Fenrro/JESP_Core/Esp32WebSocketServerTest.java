package com.Fenrro.JESP_Core;

import com.Fenrro.JESP_Core.service.DeviceState;
import com.Fenrro.JESP_Core.service.Esp32WebSocketServer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {"jesp.ws-port=0", "jesp.rules-file=target/test-rules.conf"})
@DirtiesContext
class Esp32WebSocketServerTest {

    @Autowired
    private Esp32WebSocketServer wsServer;

    @Autowired
    private DeviceState deviceState;

    @Test
    void esp32CanSendSensorsAndReceiveRelayBroadcast() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        WebSocketClient client = new WebSocketClient(new URI("ws://localhost:" + wsServer.getPort() + "/")) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                send("{\"temp\": 25.5, \"hum\": 40.0}");
            }

            @Override
            public void onMessage(String message) {
                received.set(message);
                latch.countDown();
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
            }

            @Override
            public void onError(Exception ex) {
            }
        };
        assertTrue(client.connectBlocking(5000, TimeUnit.MILLISECONDS));

        Thread.sleep(300);

        deviceState.setRelay(3, true);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(25.5f, deviceState.getCurrentTemp());
        assertEquals(40.0f, deviceState.getCurrentHum());
        assertNotNull(received.get());
        assertTrue(received.get().contains("\"reles\""));
        assertTrue(received.get().contains("true"));

        client.close();
    }
}