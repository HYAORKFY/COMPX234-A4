import java.io.*;
import java.net.*;
import java.util.Base64;

public class UDPclient {
    // Initial timeout in milliseconds
    private static final int INITIAL_TIMEOUT = 1000; 
    // Maximum number of retries for timeout
    private static final int MAX_RETRY = 5; 

    public static void main(String[] args) throws IOException {
        // Check if the correct number of arguments is provided
        if (args.length != 3) {
            System.err.println("Usage: java UDPclient <hostname> <port> <filelist>");
            return;
        }

        // Extract command-line arguments
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);
        String fileListPath = args[2];

        // Read the list of files to download from the specified file
        try (BufferedReader br = new BufferedReader(new FileReader(fileListPath))) {
            String filename;
            while ((filename = br.readLine()) != null) {
                // Download each file listed
                downloadFile(hostname, port, filename.trim()); 
            }
        }
    }

    private static void downloadFile(String hostname, int serverPort, String filename) {
        // Create a UDP socket to communicate with the server
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(hostname);
            String request = "DOWNLOAD " + filename;
            byte[] sendData = request.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, serverPort);

            // Attempt to download the file with retries on timeout
            boolean receivedResponse = false;
            int attempt = 0;
            int timeout = INITIAL_TIMEOUT;
            while (!receivedResponse && attempt < MAX_RETRY) {
                try {
                    // Send download request to server
                    socket.send(sendPacket);

                    // Prepare to receive response from server
                    byte[] receiveData = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    // Set timeout for waiting response
                    socket.setSoTimeout(timeout); 
                    // Wait for server response
                    socket.receive(receivePacket); 

                    // Process the server response
                    String response = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                    // Mark as received if no exception
                    receivedResponse = true; 
                    System.out.println("Server response: " + response);

                    // Handle error response
                    if (response.startsWith("ERR")) {
                        System.out.println("Error: " + response); 
                        return;
                    }

                    // Parse file size and data port from server response
                    String[] okParts = response.split(" ");
                    long fileSize = Long.parseLong(okParts[3]);
                    int dataPort = Integer.parseInt(okParts[5]);
                    System.out.println("Downloading " + filename + " (" + fileSize + " bytes)");

                    // Download the file in chunks
                    try (RandomAccessFile file = new RandomAccessFile(filename, "rw")) {
                        int current = 0;
                        while (current < fileSize) {
                            // Calculate chunk range
                            int end = (int) Math.min(current + 999, fileSize - 1);

                            // Request file chunk from server
                            String dataRequest = String.format("FILE %s GET START %d END %d", filename, current, end);
                            byte[] dataSend = dataRequest.getBytes();
                            DatagramPacket dataSendPacket = new DatagramPacket(dataSend, dataSend.length, address, dataPort);
                            socket.send(dataSendPacket);

                            // Receive chunk from server
                            byte[] dataReceive = new byte[2048];
                            DatagramPacket dataReceivePacket = new DatagramPacket(dataReceive, dataReceive.length);
                            socket.receive(dataReceivePacket);
                            String dataResponse = new String(dataReceivePacket.getData(), 0, dataReceivePacket.getLength()).trim();

                            // Decode and write the received chunk to the file
                            String[] parts = dataResponse.split(" DATA ");
                            String[] header = parts[0].split(" ");
                            int startReceived = Integer.parseInt(header[4]);
                            int endReceived = Integer.parseInt(header[6]);
                            byte[] decodedData = Base64.getDecoder().decode(parts[1]);
                            file.seek(startReceived);
                            file.write(decodedData);
                            // Update current position
                            current = endReceived + 1; 
                            // Print progress
                            System.out.print("*"); 
                        }

                        // Notify server of completion
                        String closeRequest = "FILE " + filename + " CLOSE";
                        byte[] closeSend = closeRequest.getBytes();
                        DatagramPacket closeSendPacket = new DatagramPacket(closeSend, closeSend.length, address, dataPort);
                        socket.send(closeSendPacket);

                        // Wait for server's close response
                        byte[] closeReceive = new byte[1024];
                        DatagramPacket closeReceivePacket = new DatagramPacket(closeReceive, closeReceive.length);
                        socket.receive(closeReceivePacket);
                        String closeResponse = new String(closeReceivePacket.getData(), 0, closeReceivePacket.getLength()).trim();
                        System.out.println("\nClose response: " + closeResponse);
                    }
                } catch (SocketTimeoutException e) {
                    // Handle timeout and retry
                    attempt++;
                    // Exponential backoff for timeout
                    timeout *= 2; 
                    System.out.println("Timeout, retrying... (" + attempt + "/" + MAX_RETRY + ")");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}