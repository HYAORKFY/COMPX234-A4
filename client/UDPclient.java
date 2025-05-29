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

        try (BufferedReader br = new BufferedReader(new FileReader(fileListPath))) {
            String filename;
            while ((filename = br.readLine()) != null) {
                downloadFile(hostname, port, filename.trim());
            }
        }
    }

    private static void downloadFile(String hostname, int port, String filename) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(hostname);
            String request = "DOWNLOAD " + filename;
            byte[] sendData = request.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
            socket.send(sendPacket);

            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();

            if (response.startsWith("ERR")) {
                System.out.println("Error: " + response);
            } else {
                System.out.println("Server response: " + response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}