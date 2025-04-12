import java.io.Serializable;

public class AuctionItem implements Serializable {
    public final String itemName;
    public final String itemDescrip;
    public final String sellerName;
    public final String sellerRqNumber;

    public String highestBidder;
    public double startPrice;
    public double highestBid;
    public int auctDuration;
    public long endTime;
    public boolean hasReceivedBid;

    public AuctionItem(String itemName, String itemDescrip, double startPrice, int auctDuration,
                       String sellerName, String sellerRqNumber) {
        this.itemName = itemName;
        this.itemDescrip = itemDescrip;
        this.startPrice = startPrice;
        this.auctDuration = auctDuration;
        this.highestBid = startPrice;
        this.highestBidder = "None";
        this.endTime = System.currentTimeMillis() + (auctDuration * 1000L);
        this.sellerName = sellerName;
        this.sellerRqNumber = sellerRqNumber;
        this.hasReceivedBid = false;
    }

    @Override
    public String toString() {
        return String.format("AuctionItem[name=%s, desc=%s, seller=%s, price=%.2f, bid=%.2f, highest=%s, endTime=%d]",
                itemName, itemDescrip, sellerName, startPrice, highestBid, highestBidder, endTime);
    }
}
