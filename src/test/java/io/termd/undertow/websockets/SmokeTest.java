package io.termd.undertow.websockets;

import io.termd.core.util.ObjectWrapper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.websocket.CloseReason;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class SmokeTest {

    private static final String HOST = "localhost";
    private static final String WEB_SOCKET_PATH = "/term";
    private static final int PORT = 8080;

    @BeforeClass
    public static void setUP() throws Exception {
        TermdServer.startServer(HOST, PORT);
    }

    @AfterClass
    public static void tearDown() {
        TermdServer.stopServer();
    }

    @Test
    public void serverShouldBeUpAndRunning() throws Exception {
        String content = readUrl(HOST, PORT, "/");
        Assert.assertTrue("Cannot read response from serverThread.", content.length() > 0);
    }

    @Test
    public void clinetShouldBeAbleToOpenWebsocketConnection() throws Exception {
        String websocketUrl = "http://" + HOST + ":" + PORT + WEB_SOCKET_PATH;
        System.out.println("websocketUrl:" + websocketUrl);

        ObjectWrapper<Boolean> pwdExecuted = new ObjectWrapper<>(false);
        ObjectWrapper<List<String>> remoteResponseWrapper = new ObjectWrapper<>(new ArrayList<>());

        Client client = setUpClient();
        Consumer<byte[]> responseConsumer = (bytes) -> {
            String response = new String(bytes);
            if ("% ".equals(response)) {
                if (!pwdExecuted.get()) {
                    pwdExecuted.set(true);
                    executeRemoteCommand(client, "pwd");
                }
            } else {
                remoteResponseWrapper.get().add(response);
            }
        };
        client.onBinaryMessage(responseConsumer);

        Semaphore semaphore = new Semaphore(1);
        semaphore.acquire();

        client.onClose(closeReason -> {
            System.out.println("Releasing client ...");
            semaphore.release();
        });

        try {
            client.connect(websocketUrl);
        } catch (Exception e) {
            System.out.println("FAILED to connect!");
            e.printStackTrace(); //TODO log
        }

        Thread.sleep(2000); //TODO use semaphore
        client.close();
        semaphore.acquire();

        List<String> strings = remoteResponseWrapper.get();
        String remoteResponses = strings.stream().collect(Collectors.joining());
        File pwd = new File("");
        System.out.println("Remote response list:" + remoteResponseWrapper.get());
        System.out.println("Remote responses:" + remoteResponses);
        Assert.assertTrue("Response should contain current working dir.", remoteResponses.contains(pwd.getAbsolutePath()));
    }

    private void executeRemoteCommand(Client client, String command) {
        System.out.println("Executing remote command ...");
        RemoteEndpoint.Basic remoteEndpoint = client.getRemoteEndpoint();
        String data = "{\"action\":\"read\",\"data\":\"" + command + "\\r\\n\"}";
        try {
            remoteEndpoint.sendBinary(ByteBuffer.wrap(data.getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
//        remoteEndpoint.sendText(data);
//        for (char aChar : data.toCharArray()) {
//            remoteEndpoint.sendText(String.valueOf(aChar));
//        }
    }

    private Client setUpClient() {
        Client client = new Client();

        Consumer<Session> onOpen = (session) -> {
            try {
                System.out.println("Client connection opened.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        Consumer<CloseReason> onClose = (closeReason) -> {
            System.out.println("Client connection closed. " + closeReason);
        };

        client.onOpen(onOpen);
        client.onClose(onClose);

        return client;
    }

    private String readUrl(String host, int port, String path) throws IOException {
        URL url = new URL("http://" + host + ":" + port + path);
        URLConnection connection = url.openConnection();
        connection.connect();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String inputLine;
        StringBuilder stringBuilder = new StringBuilder();
        while ((inputLine = bufferedReader.readLine()) != null) {
            stringBuilder.append(inputLine);
        }
        bufferedReader.close();
        return stringBuilder.toString();
    }

}
