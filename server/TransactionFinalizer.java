import java.io.*;
import java.net.Socket;

public class TransactionFinalizer {

    public void process(FinalizationSession session) {
        try {
            if (!isValid(session.buyerCC) || !isValid(session.sellerCC)) {
                send(session.buyerSocket, "CANCEL " + session.rqNumber + " Payment failed");
                send(session.sellerSocket, "CANCEL " + session.rqNumber + " Payment failed");
                ServerLog.log("Transaction failed for RQ# " + session.rqNumber + ": invalid credit card info.");
                return;
            }

            // ✅ Simulate payment processing
            double total = session.finalPrice;
            double fee = total * 0.10;
            double credited = total - fee;

            ServerLog.log("Charged " + session.buyerName + "'s card for $" + total);
            ServerLog.log("Credited " + session.sellerName + "'s card with $" + credited + " (after $"+fee+" fee)");

            // ✅ Send shipping info to seller
            String shipMsg = "Shipping_Info " + session.rqNumber + " " + session.buyerName + " " + session.buyerAddress;
            System.out.println("Sending Shipping_Info to seller: " + shipMsg);
            send(session.sellerSocket, shipMsg);
            ServerLog.log("Shipping_Info sent to seller for " + session.itemName);

            // ✅ Wait for seller ACK
            System.out.println("Waiting for Seller ACK...");
            BufferedReader in = new BufferedReader(new InputStreamReader(session.sellerSocket.getInputStream()));
            String ack = in.readLine();

            if (ack != null && ack.startsWith("ACK " + session.rqNumber)) {
                System.out.println("Received ACK from Seller.");
                ServerLog.log("ACK received from seller for RQ# " + session.rqNumber);
            } else {
                System.out.println("Did not receive valid ACK. Proceeding anyway.");
                ServerLog.log("Missing or invalid ACK from seller for RQ# " + session.rqNumber);
            }

            // ✅ Close sockets
            session.buyerSocket.close();  // Server closes buyer socket
            session.sellerSocket.close(); // Seller may also close, but safe here

            // ✅ Final transaction log
            TransactionManager.logTransaction(
                    session.itemName,
                    session.sellerName,
                    session.buyerName,
                    session.finalPrice
            );

        } catch (Exception e) {
            System.out.println("TransactionFinalizer error: " + e.getMessage());
            ServerLog.log("TransactionFinalizer failed for RQ# " + session.rqNumber + ": " + e.getMessage());
        }
    }

    private void send(Socket socket, String message) {
        try {
            PrintWriter out = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            out.println(message);
            out.flush();
        } catch (Exception e) {
            System.out.println("Error sending: " + message + " → " + e.getMessage());
        }
    }

    private boolean isValid(String cc) {
        return cc != null && cc.length() >= 8;
    }
}
