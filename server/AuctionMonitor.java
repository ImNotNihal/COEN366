import java.net.*;
import java.io.*;
import java.util.*;

public class AuctionMonitor implements Runnable {
    private final Map<String, AuctionItem> auctions;
    private final Map<String, ClientInfo> clients;
    private final Map<String, FinalizationSession> sessions;  // added for 2.7

    public AuctionMonitor(Map<String, AuctionItem> auctions, 
                          Map<String, ClientInfo> clients, 
                          Map<String, FinalizationSession> sessions) // added for 2.7
    {
        this.auctions = auctions;
        this.clients = clients;
        this.sessions = sessions;  // added for 2.7
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
                }
            }

            for (String itemName : endedAuctions) {
                auctions.remove(itemName);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Auction monitor interrupted.");
            }
        }
    }

    private void handleAuctionClosure(AuctionItem item) {
        String rqNumber = item.sellerRqNumber;

        if (!clients.containsKey(item.sellerName)) return;
        ClientInfo seller = clients.get(item.sellerName);

        if (!item.hasReceivedBid || item.highestBidder.equals("None")) {
            sendTCPMessage(seller, "NON_OFFER | " + rqNumber + " | " + item.itemName);
            ServerLog.log("Auction ended with no bids: " + item.itemName);
        } else {
            // check for missing buyer
            if (!clients.containsKey(item.highestBidder)) {
                ServerLog.log("Auction ended but winner not found: " + item.highestBidder);
                return;
            }

            ClientInfo buyer = clients.get(item.highestBidder);
            String winnerMsg = "WINNER | " + rqNumber + " | " + item.itemName + " | " + item.highestBid + " | " + seller.name;
            sendTCPMessage(buyer, winnerMsg);
            String soldMsg = "SOLD | " + rqNumber + " | " + item.itemName + " | " + item.highestBid + " | " + buyer.name;
            sendTCPMessage(seller, soldMsg);
            ServerLog.log("Auction won: " + buyer.name + " won " + item.itemName + " for $" + item.highestBid);

            // Log for Task 2.7
            TransactionManager.logTransaction(item.itemName, item.sellerName, item.highestBidder, item.highestBid);

            // added for 2.7
            // generate a new RQ# for transaction finalization 
            String finalRq = "FINAL_" + System.currentTimeMillis();

            // create and register session
            FinalizationSession session = new FinalizationSession(finalRq, item.itemName, item.highestBid);
            sessions.put(finalRq, session);

            // send INFORM_req to both buyer and seller via TCP
            sendTCPMessage(buyer, "INFORM_Req " + finalRq + " " + item.itemName + " " + item.highestBid);
            sendTCPMessage(seller, "INFORM_Req " + finalRq + " " + item.itemName + " " + item.highestBid);
            ServerLog.log("INFORM_Req sent to both parties for " + item.itemName);
            // added until here for 2.7
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
