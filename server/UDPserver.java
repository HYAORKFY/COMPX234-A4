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
            System.out.println("Received request from client");
        }
    }
}