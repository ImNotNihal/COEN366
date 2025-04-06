import java.io.*;
import java.net.Socket;

public class TransactionFinalizer {

    public void process(FinalizationSession session) {
        try {
            if (!isValid(session.buyerCC) || !isValid(session.sellerCC)) {
                send(session.buyerSocket, "CANCEL " + session.rqNumber + " Payment failed");
                send(session.sellerSocket, "CANCEL " + session.rqNumber + " Payment failed");
                ServerLog.log("Transaction failed for RQ# " + session.rqNumber);
                return;
            }

            String shipMsg = "Shipping_Info " + session.rqNumber + " " + session.buyerName + " " + session.buyerAddress;
            System.out.println("Sending Shipping_Info to seller: " + shipMsg);
            send(session.sellerSocket, shipMsg);
            ServerLog.log("Shipping_Info sent to seller for " + session.itemName);
            System.out.println("Waiting for SellerClient to read shipping info...");

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {}

            System.out.println("Closing buyer socket after delay.");
            session.buyerSocket.close();

            TransactionManager.logTransaction(
                    session.itemName,
                    session.sellerName,
                    session.buyerName,
                    session.finalPrice
            );

        } catch (Exception e) {
            System.out.println("TransactionFinalizer error: " + e.getMessage());
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
