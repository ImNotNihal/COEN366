import java.net.*;
import java.io.*;
import java.util.*;

public class AuctionMonitor implements Runnable {
    private final Map<String, AuctionItem> auctions;
    private final Map<String, ClientInfo> clients;

    public AuctionMonitor(Map<String, AuctionItem> auctions, Map<String, ClientInfo> clients) {
        this.auctions = auctions;
        this.clients = clients;
    }

    @Override
    public void run() {
        while (true) {
            long now = System.currentTimeMillis();
            List<String> endedAuctions = new ArrayList<>();

            for (Map.Entry<String, AuctionItem> entry : auctions.entrySet()) {
                AuctionItem item = entry.getValue();
                if (now >= item.endTime) {
                    endedAuctions.add(entry.getKey());
                    handleAuctionClosure(item);
                }}

            for (String itemName : endedAuctions) {
                auctions.remove(itemName);
            }
            try {
                Thread.sleep(1000); 
            } catch (InterruptedException e) {
                System.out.println("Auction monitor interrupted.");
            }}}

    private void handleAuctionClosure(AuctionItem item) {
        String rqNumber = item.sellerRqNumber;

        if (!clients.containsKey(item.sellerName)) return;
        ClientInfo seller = clients.get(item.sellerName);
        if (!item.hasReceivedBid || item.highestBidder.equals("None")) {
            sendTCPMessage(seller, "NON_OFFER | " + rqNumber + " | " + item.itemName);
            ServerLog.log("Auction ended with no bids: " + item.itemName);
        } else {
            ClientInfo buyer = clients.get(item.highestBidder);
            String winnerMsg = "WINNER | " + rqNumber + " | " + item.itemName + " | " + item.highestBid + " | " + seller.name;
            sendTCPMessage(buyer, winnerMsg);
            String soldMsg = "SOLD | " + rqNumber + " | " + item.itemName + " | " + item.highestBid + " | " + buyer.name;
            sendTCPMessage(seller, soldMsg);
            ServerLog.log("Auction won: " + buyer.name + " won " + item.itemName + " for $" + item.highestBid);
        }
    }

    private void sendTCPMessage(ClientInfo client, String message) {
        try (Socket socket = new Socket(client.ipAddress, client.tcpPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(message);
        } catch (IOException e) {
            System.out.println("Failed to send TCP message to " + client.name + ": " + e.getMessage());
        }
    }
}
