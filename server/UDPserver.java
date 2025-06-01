import java.io.*;
import java.net.*;
import java.util.Base64;

public class UDPserver {

    // Maximum number of retries for timeout
    private static final int MAX_RETRY = 5; 

    public static void main(String[] args) throws IOException {
        // Check if the correct number of arguments is provided
        if (args.length != 1) {
            System.err.println("Usage: java UDPserver <port>");
            return;
        }

        // Extract the port number from the command-line argument
        int port = Integer.parseInt(args[0]);
        DatagramSocket welcomeSocket = new DatagramSocket(port);
        System.out.println("Server started on port " + port);

        // Main server loop to handle incoming client requests
        while (true) {
            byte[] buffer = new byte[1024];
            DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
            welcomeSocket.receive(requestPacket);

            // Extract the request message from the client
            String request = new String(requestPacket.getData(), 0, requestPacket.getLength()).trim();
            String[] tokens = request.split(" ");
            
            // Log the client request
            System.out.println("[Client " + requestPacket.getAddress().getHostAddress() + ":" + 
                             requestPacket.getPort() + "] Requested: " + request);

            // Validate the client request
            if (tokens.length != 2 || !tokens[0].equals("DOWNLOAD")) {
                System.out.println("[Server] Ignored invalid request: " + request);
                continue;
            }

            // Extract the filename and check if the file exists
            String filename = tokens[1];
            File file = new File(filename);
            InetAddress clientAddress = requestPacket.getAddress();
            int clientPort = requestPacket.getPort();

            // Handle the file download request in a new thread
            new Thread(() -> {
                try {
                    // Check if the requested file exists
                    if (!file.exists()) {
                        String response = "ERR " + filename + " NOT_FOUND";
                        System.out.println("[Client " + clientAddress.getHostAddress() + ":" + 
                                         clientPort + "] File not found: " + filename);
                        sendResponse(welcomeSocket, clientAddress, clientPort, response);
                        return;
                    }

                    // Generate a random data port for file transfer
                    int dataPort = 50000 + (int) (Math.random() * 1000);
                    String okResponse = "OK " + filename + " SIZE " + file.length() + " PORT " + dataPort;
                    
                    // Log the assigned data port
                    System.out.println("[Client " + clientAddress.getHostAddress() + ":" + 
                                     clientPort + "] Assigned data port " + dataPort + 
                                     " for file: " + filename);

                    // Send the initial OK response to the client
                    sendResponse(welcomeSocket, clientAddress, clientPort, okResponse);

                    // Create a new UDP socket for data transfer
                    DatagramSocket dataSocket = new DatagramSocket(dataPort);
                    handleFileTransfer(dataSocket, file, clientAddress, clientPort);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    // Handle file transfer over a dedicated UDP socket
    private static void handleFileTransfer(DatagramSocket dataSocket, File file, InetAddress clientAddress, int clientPort) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] buffer = new byte[1024];
            while (true) {
                // Receive request from the client
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                dataSocket.receive(packet);
                String request = new String(packet.getData(), 0, packet.getLength()).trim();
                String[] tokens = request.split(" ");

                // Handle the client's close request
                if (tokens[0].equals("FILE") && tokens[2].equals("CLOSE")) {
                    String response = "FILE " + tokens[1] + " CLOSE_OK";
                    sendResponse(dataSocket, clientAddress, clientPort, response);
                    System.out.println("[Client " + clientAddress.getHostAddress() + ":" + 
                                     clientPort + "] File " + tokens[1] + " sent successfully");
                    break;
                }

                // Extract the requested byte range
                int start = Integer.parseInt(tokens[4]);
                int end = Integer.parseInt(tokens[6]);
                int length = end - start + 1;
                raf.seek(start);
                byte[] fileData = new byte[length];
                raf.read(fileData);
                String base64Data = Base64.getEncoder().encodeToString(fileData);
                String response = String.format("FILE %s OK START %d END %d DATA %s", tokens[1], start, end, base64Data);
                sendResponse(dataSocket, clientAddress, clientPort, response);
            }
            dataSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Send a response message to the client
    private static void sendResponse(DatagramSocket socket, InetAddress address, int port, String message) throws IOException {
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }
}