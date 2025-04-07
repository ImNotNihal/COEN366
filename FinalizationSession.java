import java.net.Socket;

public class FinalizationSession {
    public final String rqNumber;
    public final String itemName;
    public final double finalPrice;

    // Buyer information
    public String buyerName;
    public String buyerCC;
    public String buyerExp;
    public String buyerAddress;
    public Socket buyerSocket;
    public boolean buyerReady = false;

    // Seller information
    public String sellerName;
    public String sellerCC;
    public String sellerExp;
    public String sellerAddress;
    public Socket sellerSocket;
    public boolean sellerReady = false;

    public FinalizationSession(String rqNumber, String itemName, double finalPrice) {
        this.rqNumber = rqNumber;
        this.itemName = itemName;
        this.finalPrice = finalPrice;
    }

    public boolean isReady() {
        return buyerReady && sellerReady;
    }

    public double sellerNetPayout() {
        return finalPrice * 0.90; // 90% goes to seller
    }

    public double serverFee() {
        return finalPrice * 0.10; // 10% retained by server
    }
}
