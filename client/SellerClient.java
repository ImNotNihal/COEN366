import java.io.*;
import java.net.*;
import java.util.Scanner;

public class SellerClient {
    public static void main(String[] args) {
        int tcpPort = 6000;
        try (ServerSocket serverSocket = new ServerSocket(tcpPort)) {
            System.out.println("Seller TCP listener started on port " + tcpPort);
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleFinalization(socket)).start();
            }
        } catch (IOException e) {
            System.out.println("SellerClient error: " + e.getMessage());
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
                System.out.println("Finalizing sale for item: " + itemName + ", price: $" + finalPrice);

                System.out.print("Enter name: ");
                String name = sc.nextLine();
                System.out.print("Enter credit card #: ");
                String cc = sc.nextLine();
                System.out.print("Enter CC expiration date (MM/YY): ");
                String exp = sc.nextLine();
                System.out.print("Enter address (for receiving payment): ");
                String address = sc.nextLine();

                String response = "INFORM_Res " + rqNumber + " " + name + " " + cc + " " + exp + " " + address;
                out.println(response);
                out.flush();
                ActivityLog.log("Sent: " + response);

                // Wait for Shipping_Info
                System.out.println("Waiting for Shipping_Info...");
                String shippingInfo = in.readLine();

                if (shippingInfo != null) {
                    ActivityLog.log("Received: " + shippingInfo);
                    System.out.println("Shipping Info: " + shippingInfo);
                } else {
                    System.out.println("No Shipping_Info received. Server may have closed connection.");
                }
            }

        } catch (IOException e) {
            System.out.println("Error during seller finalization: " + e.getMessage());
        } finally {
            try {
                socket.close();
                System.out.println("Seller socket closed cleanly.");
            } catch (IOException e) {
                System.out.println("Failed to close seller socket: " + e.getMessage());
            }
        }
    }
}
