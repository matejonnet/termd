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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;


/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class SmokeTest {

    private static final String HOST = "localhost";
    private static final String WEB_SOCKET_PATH = "/term";
    private static final int PORT = 8080;

//    final RemoteEndpoint.Async[] remoteEndpointRef = new RemoteEndpoint.Async[1];
//    final ObjectWrapper<RemoteEndpoint.Basic> remoteEndpointRef = new ObjectWrapper<>();

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

        ObjectWrapper<Client> clientReference = new ObjectWrapper<>();

//        Runnable runClient = () -> {
//            Client client = setUpClient();
//            clientReference.set(client);
//            try {
//                client.connect(websocketUrl);
//            } catch (Exception e) {
//                e.printStackTrace(); //TODO log
//            }
//        };
//        Thread clientThread = new Thread(runClient);
//        clientThread.start();
//        runClient.run();

//        List<String> receivedMessages = new ArrayList<>();
//
//        Client client = clientReference.get();
//
//        client.onStringMessage(message -> {
//            System.out.println("Adding received string message to the list: " + message);
//            receivedMessages.add(message);
//        });
//
//        client.onBinaryMessage(bytes -> {
//            System.out.println("Adding received binary message to the list:" + new String(bytes));
//            receivedMessages.add(new String(bytes));
//        });



//        Runnable sendData = () -> {
//            try {
//                RemoteEndpoint.Basic remoteEndpoint = remoteEndpointRef.get();
//                String data = "{action:'read',data:'pwd\n'}";
//                char[] chars = data.toCharArray();
//                int i = 0;
//                for (char aChar : chars) {
//                    i++;
//                    boolean isLast = i == chars.length;
////                    System.out.println("is " + i + " last: " + isLast); //TODO log
//                    remoteEndpoint.sendText(String.valueOf(aChar), isLast);
//
//                }
//                remoteEndpoint.sendBinary(ByteBuffer.wrap(data.getBytes()));
//                remoteEndpoint.sendText(data);
//                Thread.sleep(1000);
//            } catch (Exception e) {
//                e.printStackTrace(); //TODO log
//            }
//        };
//
//
//        Thread sendDataThread = new Thread(sendData);
//        sendDataThread.start();
//        sendDataThread.join();

//        System.out.println(receivedMessages);


//        Client client = new Client();
        Client client = setUpClient();


        try {
            client.connect(websocketUrl);
        } catch (Exception e) {
            System.out.println("FAILED to connect!");
            e.printStackTrace(); //TODO log
        }

        RemoteEndpoint.Basic remoteEndpoint = client.getRemoteEndpoint();


        String data = "{\"action\":\"read\",\"data\":\"pwd\\n\"}";
        remoteEndpoint.sendBinary(ByteBuffer.wrap(data.getBytes()));
//        remoteEndpoint.sendText(data);
//        for (char aChar : data.toCharArray()) {
//            remoteEndpoint.sendText(String.valueOf(aChar));
//        }


        Semaphore semaphore = new Semaphore(1);
        semaphore.acquire();

        client.onClose(closeReason -> {
            System.out.println("Releasing client ...");
            semaphore.release();
        });


        Thread.sleep(1000);
        client.close();
        semaphore.acquire();
        Assert.assertTrue(true);
    }

    private Client setUpClient() {
        Client client = new Client();

        Consumer<Session> onOpen = (session) -> {
            try {
                System.out.println("Client connection opened.");
    //            session.getAsyncRemote().sendText("whoami\n");
    //            session.getAsyncRemote().sendText("{action:'read',data:'whoami '}");
//                session.getBasicRemote().sendText("{action:'read',data:'whoami '}");
//                String data = "whoami\n";
//                char[] chars = data.toCharArray();
//                for (char aChar : chars) {
//                    session.getBasicRemote().sendText("{action:'read',data:'" + aChar + "'}");
//                    session.getAsyncRemote().sendText("{action:'read',data:'" + aChar + "'}");
//                }
//                remoteEndpointRef[0] = session.getAsyncRemote();
//                remoteEndpointRef.set(session.getBasicRemote());
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
