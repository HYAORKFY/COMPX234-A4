import java.io.*;
import java.net.*;
import java.util.Base64;

public class UDPclient {
    private static final int INITIAL_TIMEOUT = 1000;
    private static final int MAX_RETRY = 5;

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

    private static void downloadFile(String hostname, int serverPort, String filename) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(hostname);
            String request = "DOWNLOAD " + filename;
            byte[] sendData = request.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, serverPort);

            boolean receivedResponse = false;
            int attempt = 0;
            int timeout = INITIAL_TIMEOUT;
            while (!receivedResponse && attempt < MAX_RETRY) {
                try {
                    socket.send(sendPacket);
                    byte[] receiveData = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.setSoTimeout(timeout);
                    socket.receive(receivePacket);
                    String response = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                    receivedResponse = true;
                    System.out.println("Server response: " + response);

                    if (response.startsWith("ERR")) {
                        System.out.println("Error: " + response);
                        return;
                    }

                    String[] okParts = response.split(" ");
                    long fileSize = Long.parseLong(okParts[3]);
                    int dataPort = Integer.parseInt(okParts[5]);
                    System.out.println("Downloading " + filename + " (" + fileSize + " bytes)");

                    try (RandomAccessFile file = new RandomAccessFile(filename, "rw")) {
                        int current = 0;
                        while (current < fileSize) {
                            int end = (int) Math.min(current + 999, fileSize - 1);
                            String dataRequest = String.format("FILE %s GET START %d END %d", filename, current, end);
                            byte[] dataSend = dataRequest.getBytes();
                            DatagramPacket dataSendPacket = new DatagramPacket(dataSend, dataSend.length, address, dataPort);
                            socket.send(dataSendPacket);

                            byte[] dataReceive = new byte[2048];
                            DatagramPacket dataReceivePacket = new DatagramPacket(dataReceive, dataReceive.length);
                            socket.receive(dataReceivePacket);
                            String dataResponse = new String(dataReceivePacket.getData(), 0, dataReceivePacket.getLength()).trim();

                            String[] parts = dataResponse.split(" DATA ");
                            String[] header = parts[0].split(" ");
                            int startReceived = Integer.parseInt(header[4]);
                            int endReceived = Integer.parseInt(header[6]);
                            byte[] decodedData = Base64.getDecoder().decode(parts[1]);
                            file.seek(startReceived);
                            file.write(decodedData);
                            current = endReceived + 1;
                            System.out.print("*");
                        }

                        String closeRequest = "FILE " + filename + " CLOSE";
                        byte[] closeSend = closeRequest.getBytes();
                        DatagramPacket closeSendPacket = new DatagramPacket(closeSend, closeSend.length, address, dataPort);
                        socket.send(closeSendPacket);

                        byte[] closeReceive = new byte[1024];
                        DatagramPacket closeReceivePacket = new DatagramPacket(closeReceive, closeReceive.length);
                        socket.receive(closeReceivePacket);
                        String closeResponse = new String(closeReceivePacket.getData(), 0, closeReceivePacket.getLength()).trim();
                        System.out.println("\nClose response: " + closeResponse);
                    }
                } catch (SocketTimeoutException e) {
                    attempt++;
                    timeout *= 2; 
                    System.out.println("Timeout, retrying... (" + attempt + "/" + MAX_RETRY + ")");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}