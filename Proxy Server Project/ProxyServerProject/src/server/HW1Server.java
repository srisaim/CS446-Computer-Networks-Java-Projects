package server;

import java.io.*;
import java.net.*;

public class HW1Server {
    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(args[0]);
        ServerSocket serverSocket = new ServerSocket(port);

        while (true) {
            new ClientHandler(serverSocket.accept()).start();
        }
    }
}

class ClientHandler extends Thread {
    private Socket clientSocket;

    ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    public void run() {
        try {
            System.out.println("Client connection established: " + clientSocket.getInetAddress());
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            String command = in.readLine();
            if (command != null && command.startsWith("GET")) {
                String[] parts = command.split(" ", 2);

                if (parts.length > 1) {
                    URL url = new URL(parts[1].trim());
                    String host = url.getHost();
                    String path = url.getPath().isEmpty() ? "/" : url.getPath();

                    try {

                        try (Socket webSocket = new Socket(host, 80);
                             BufferedWriter webOut = new BufferedWriter(new OutputStreamWriter(webSocket.getOutputStream()));
                             BufferedReader webIn = new BufferedReader(new InputStreamReader(webSocket.getInputStream()))) {

                            webOut.write("GET " + path + " HTTP/1.1\r\n");
                            webOut.write("Host: " + host + "\r\n");
                            webOut.write("Connection: close\r\n");
                            webOut.write("\r\n");
                            webOut.flush();

                            String responseLine;
                            while ((responseLine = webIn.readLine()) != null) {
                                out.println(responseLine);
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("An error occurred while trying to connect to " + host);
                        e.printStackTrace();
                        out.println("Error fetching URL: " + e.getMessage());
                    }
                } else {
                    out.println("Invalid command format. Usage: GET <URL>");
                }
            }
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

