package com.Fenrro.JESP_Core;

import com.Fenrro.JESP_Core.service.DeviceRegistry;
import com.Fenrro.JESP_Core.service.Esp32WebSocketServer;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "jesp.ws-port=0",
        "jesp.rules-file=target/test-rules-ws.conf",
        "spring.datasource.url=jdbc:sqlite:target/test-db-ws.db"
})
@DirtiesContext
class Esp32WebSocketServerTest {

    @Autowired
    private Esp32WebSocketServer wsServer;

    @Autowired
    private DeviceRegistry deviceRegistry;

    @Test
    void esp32CanSendSensorsAndReceiveRelayBroadcast() throws Exception {
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        WebSocketClient client = queueClient(messages);
        assertTrue(client.connectBlocking(5000, TimeUnit.MILLISECONDS));

        // Esperamos a que el servidor registre la conexión y enviamos los datos
        // ya fuera del handshake, con reintento hasta confirmación.
        awaitTrue(() -> wsServer.isDeviceConnected(), 5000);
        sendUntil(client, "{\"temp\": 25.5, \"hum\": 40.0}",
                () -> deviceRegistry.getOrCreate(DeviceRegistry.DEFAULT_DEVICE_ID).getCurrentTemp() == 25.5f);

        assertEquals(25.5f,
                deviceRegistry.getOrCreate(DeviceRegistry.DEFAULT_DEVICE_ID).getCurrentTemp());
        assertEquals(40.0f,
                deviceRegistry.getOrCreate(DeviceRegistry.DEFAULT_DEVICE_ID).getCurrentHum());

        messages.clear();
        deviceRegistry.getOrCreate(DeviceRegistry.DEFAULT_DEVICE_ID).setRelay(3, true);
        deviceRegistry.fireRelayChanged(DeviceRegistry.DEFAULT_DEVICE_ID);

        String msg = messages.poll(5, TimeUnit.SECONDS);
        assertNotNull(msg, "El cliente debe recibir el broadcast");
        assertTrue(msg.contains("\"reles\""));
        assertTrue(msg.contains("true"));

        client.close();
    }

    @Test
    void devicesWithIdsGetIndependentSessions() throws Exception {
        BlockingQueue<String> messagesA = new LinkedBlockingQueue<>();
        BlockingQueue<String> messagesB = new LinkedBlockingQueue<>();

        WebSocketClient clientA = queueClient(messagesA);
        WebSocketClient clientB = queueClient(messagesB);

        assertTrue(clientA.connectBlocking(5000, TimeUnit.MILLISECONDS));
        assertTrue(clientB.connectBlocking(5000, TimeUnit.MILLISECONDS));

        // Identificación de cada dispositivo con reintentos hasta confirmación en el server
        long deadline = System.currentTimeMillis() + 10_000;
        while (!(wsServer.isDeviceConnected("device-a") && wsServer.isDeviceConnected("device-b"))) {
            if (System.currentTimeMillis() > deadline) break;
            if (!wsServer.isDeviceConnected("device-a")) {
                clientA.send("{\"id\":\"device-a\",\"temp\":20.0,\"hum\":30.0}");
            }
            if (!wsServer.isDeviceConnected("device-b")) {
                clientB.send("{\"id\":\"device-b\",\"temp\":22.0,\"hum\":35.0}");
            }
            Thread.sleep(100);
        }

        awaitTrue(() -> !wsServer.isDeviceConnected(), 5000);
        awaitTrue(() -> deviceRegistry.getOrCreate("device-a").getCurrentTemp() == 20.0f, 5000);
        awaitTrue(() -> deviceRegistry.getOrCreate("device-b").getCurrentTemp() == 22.0f, 5000);

        assertEquals(20.0f, deviceRegistry.getOrCreate("device-a").getCurrentTemp());
        assertEquals(22.0f, deviceRegistry.getOrCreate("device-b").getCurrentTemp());

        // Drenamos la configuración inicial enviada al conectar
        messagesA.clear();
        messagesB.clear();

        // Cambiar el relé del dispositivo A solo emite a A
        deviceRegistry.getOrCreate("device-a").setRelay(0, true);
        deviceRegistry.fireRelayChanged("device-a");

        String msgA = messagesA.poll(5, TimeUnit.SECONDS);
        assertNotNull(msgA, "El dispositivo A debe recibir su broadcast con el relé ON");
        assertTrue(msgA.contains("true"));
        Thread.sleep(200);
        String unexpected = messagesB.poll();
        assertTrue(unexpected == null || !unexpected.contains("true"),
                "El dispositivo B no debe recibir el broadcast de A");

        clientA.close();
        clientB.close();
    }

    private void sendUntil(WebSocketClient client, String message, java.util.function.BooleanSupplier done)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (!done.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) return; // la aserción informará
            client.send(message);
            Thread.sleep(100);
        }
    }

    private void awaitTrue(java.util.function.BooleanSupplier condition, long timeoutMillis)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                return; // las aserciones posteriores informarán del fallo
            }
            Thread.sleep(50);
        }
    }

    private WebSocketClient queueClient(BlockingQueue<String> queue) throws java.net.URISyntaxException {
        return new WebSocketClient(new URI("ws://localhost:" + wsServer.getPort() + "/")) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                // No se envía nada aquí: bajo carga los frames durante el handshake pueden perderse
            }

            @Override
            public void onMessage(String message) {
                queue.add(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
            }

            @Override
            public void onError(Exception ex) {
            }
        };
    }
}
