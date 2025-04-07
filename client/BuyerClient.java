import java.io.*;
import java.net.*;
import java.util.Scanner;

public class BuyerClient {
    public static void main(String[] args) {
        int tcpPort = 6001;
        try (ServerSocket serverSocket = new ServerSocket(tcpPort)) {
            System.out.println("Buyer TCP listener started on port " + tcpPort);
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleFinalization(socket)).start();
            }
        } catch (IOException e) {
            System.out.println("BuyerClient error: " + e.getMessage());
        }
    }

    private static void handleFinalization(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true)
        ) {
            String request = in.readLine();
            ActivityLog.log("Received: " + request);

            if (request != null && request.startsWith("INFORM_Req")) {
                String[] parts = request.split(" ");
                String rqNumber = parts[1];
                String itemName = parts[2];
                String finalPrice = parts[3];

                Scanner sc = new Scanner(System.in);
                System.out.println("Finalizing purchase for item: " + itemName + ", price: $" + finalPrice);

                System.out.print("Enter name: ");
                String name = sc.nextLine();
                System.out.print("Enter credit card #: ");
                String cc = sc.nextLine();
                System.out.print("Enter CC expiration date (MM/YY): ");
                String exp = sc.nextLine();
                System.out.print("Enter shipping address: ");
                String address = sc.nextLine();

                String response = "INFORM_Res " + rqNumber + " " + name + " " + cc + " " + exp + " " + address;
                out.println(response);
                out.flush();
                ActivityLog.log("Sent: " + response);

                // ✅ You may optionally wait for a confirmation or CANCEL, but the server closes this socket
                System.out.println("Waiting for final server response (if any)...");
                String serverResponse = in.readLine();
                if (serverResponse != null) {
                    System.out.println("Server response: " + serverResponse);
                    ActivityLog.log("Received: " + serverResponse);
                } else {
                    System.out.println("No final message received from server.");
                }
            }

        } catch (IOException e) {
            System.out.println("Error during buyer finalization: " + e.getMessage());
        } finally {
            try {
                socket.close();
                System.out.println("Buyer socket closed cleanly.");
            } catch (IOException e) {
                System.out.println("Failed to close buyer socket: " + e.getMessage());
            }
        }
    }
}
