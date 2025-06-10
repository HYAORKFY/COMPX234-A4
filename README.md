# COMPX234-A4 Assignment

## Project Overview
The goal of this project is to develop a client/server network program based on UDP that implements reliable transmission mechanisms. This ensures that clients can correctly download files from the server even in an unreliable network environment.

## Code File Description
### UDPclient.java
- The client program, responsible for sending file download requests to the server and receiving the file data transmitted by the server.
- Supports receiving the server hostname, port number, and file list path from the command line.
- Downloads files synchronously, meaning it downloads one file at a time and exits after completion.
- Uses UDP sockets to communicate with the server and implements a timeout retransmission mechanism to handle data loss.
- Supports downloading files in chunks and uses Base64 encoding for receiving and decoding file data.

### UDPserver.java
- The server program, responsible for receiving file download requests from clients and creating a new thread for each request to handle file transfer.
- Supports handling requests from multiple clients simultaneously.
- Listens for client requests using a UDP socket and assigns a dedicated high port number for each file transfer.
- Reads file data and transmits it to the client in chunks, with Base64 encoding.
- Receives the client's close request and confirms the completion of file transfer.

## Running Guide
### Server Startup
1. Deploy the server program on the target host.
2. Start the server by running the following command in the command line:
   ```
   java UDPserver <port>
   ```
   - port: The port number on which the server listens, for example, `51234`.
3. Ensure that the server directory contains the files available for download.

### Client Startup
1. Deploy the client program on the target host.
2. Create a text file (e.g., `files.txt`) containing a list of filenames, with one filename per line.
3. Start the client by running the following command in the command line:
   ```
   java UDPclient <hostname> <port> <filelist>
   ```
   - hostname: The hostname or IP address of the server, for example, `localhost`.
   - port: The port number on which the server listens, for example, `51234`.
   - filelist: The path to the file containing the list of filenames, for example, `files.txt`.
4. The client will download the files listed in the file list one by one.

## Testing Steps
1. Prepare test files in the server directory.
2. Start the server program.
3. Create a file list file in the client directory and start the client program.
4. You can also try running multiple client instances on different machines to download files simultaneously and verify the multi-client concurrent download functionality.I have verified it and found that it can pass the verification.
