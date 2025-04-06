import java.net.Socket;

public class FinalizationSession {
    public final String rqNumber;
    public String buyerName, buyerCC, buyerExp, buyerAddress;
    public String sellerName, sellerCC, sellerExp, sellerAddress;
    public Socket buyerSocket, sellerSocket;
    public boolean buyerReady = false;
    public boolean sellerReady = false;
    public String itemName;
    public double finalPrice;

    public FinalizationSession(String rqNumber, String itemName, double finalPrice) {
        this.rqNumber = rqNumber;
        this.itemName = itemName;
        this.finalPrice = finalPrice;
    }

    public boolean isReady() {
        return buyerReady && sellerReady;
    }
}
