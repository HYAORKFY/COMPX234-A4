import java.io.*;
import java.net.*;

public class UDPclient {
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: java UDPclient <hostname> <port> <filelist>");
            return;
        }

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);
        String fileListPath = args[2];

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(hostname);
            String request = "DOWNLOAD " + fileListPath;
            byte[] sendData = request.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
            socket.send(sendPacket);

            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
            System.out.println("Server response: " + response);
        }
    }
}