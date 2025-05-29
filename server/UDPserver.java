import java.io.*;
import java.net.*;

public class UDPserver {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java UDPserver <port>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        DatagramSocket welcomeSocket = new DatagramSocket(port);
        System.out.println("Server started on port " + port);

        while (true) {
            byte[] buffer = new byte[1024];
            DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
            welcomeSocket.receive(requestPacket);

            String request = new String(requestPacket.getData(), 0, requestPacket.getLength()).trim();
            String[] tokens = request.split(" ");
            if (tokens.length != 2 || !tokens[0].equals("DOWNLOAD")) {
                continue; 
            }

            String filename = tokens[1];
            File file = new File(filename);
            InetAddress clientAddress = requestPacket.getAddress();
            int clientPort = requestPacket.getPort();

            new Thread(() -> {
                try {
                    if (!file.exists()) {
                        String response = "ERR " + filename + " NOT_FOUND";
                        sendResponse(welcomeSocket, clientAddress, clientPort, response);
                    } else {
                        int dataPort = 50000 + (int) (Math.random() * 1000);
                        String okResponse = "OK " + filename + " SIZE " + file.length() + " PORT " + dataPort;
                        sendResponse(welcomeSocket, clientAddress, clientPort, okResponse);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private static void sendResponse(DatagramSocket socket, InetAddress address, int port, String message) throws IOException {
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }
}