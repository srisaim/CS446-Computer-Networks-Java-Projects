package client;

import java.io.*;
import java.net.*;
import java.nio.file.Paths;

public class HW1Client {
    public static void main(String[] args) throws IOException {
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try (
                Socket socket = new Socket(host, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))
        ) {
            String input = stdIn.readLine();
            if (input != null) {
                out.println(input);

                String path = new URL(input.split(" ")[1]).getPath();
                String fileName = Paths.get(path).getFileName().toString();
                if (fileName.isEmpty()) {
                    fileName = "Summary.html";
                }

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                    String serverOutput;
                    while ((serverOutput = in.readLine()) != null) {
                        writer.write(serverOutput);
                        writer.newLine();
                    }
                }
                System.out.println("File saved as: " + fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

