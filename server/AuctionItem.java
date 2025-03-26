public class AuctionItem {
    String itemName, itemDescrip, highestBidder, sellerName, sellerRqNumber;
    double startPrice, highestBid;
    int auctDuration;
    long endTime;
    public boolean hasReceivedBid;

    AuctionItem(String itemName, String itemDescrip, double startPrice, int auctDuration, String sellerName, String sellerRqNumber) {
        this.itemName = itemName;
        this.itemDescrip = itemDescrip;
        this.startPrice = startPrice;
        this.auctDuration = auctDuration;
        this.highestBid = startPrice;
        this.highestBidder = "None";
        this.endTime = System.currentTimeMillis() + (auctDuration * 1000L); // Convert seconds to milliseconds
        this.sellerName = sellerName;
        this.sellerRqNumber = sellerRqNumber;
        this.hasReceivedBid = false;
    }
}
