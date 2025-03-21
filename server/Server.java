// Server is running, gets the message from the client, takes the IP/Path address of the client and suggests that the client has been registered or not

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private static final int PORT = 5000;
    private static Map<String, ClientInfo> registeredClients = new HashMap<>();
    private static Map<String, AuctionItem> currentAuctions = new HashMap<>();

    static class ClientInfo {
        String name;
        String role;
        String ipAddress;
        int udpPort;
        int tcpPort;

        ClientInfo(String name, String role, String ipAddress, int udpPort, int tcpPort) {
            this.name = name;
            this.role = role;
            this.ipAddress = ipAddress;
            this.udpPort = udpPort;
            this.tcpPort = tcpPort;
        }
    }

    static class AuctionItem {
        String itemName;
        String itemDescrip;
        double startPrice;
        int auctDuration;
        double highestBid;
        String highestBidder;
        long endTime;

        AuctionItem(String itemName, String itemDescrip, double startPrice, int auctDuration) {
            this.itemName = itemName;
            this.itemDescrip = itemDescrip;
            this.startPrice = startPrice;
            this.auctDuration = auctDuration;
            this.highestBid = startPrice;
            this.highestBidder = "None";
            this.endTime = System.currentTimeMillis() + (auctDuration * 1000L); // Convert seconds to milliseconds
        }
    }

    public static void main(String[] args) throws IOException {
        DatagramSocket ds = new DatagramSocket(PORT);
        byte[] receive = new byte[65535];
        DatagramPacket receiveDatagramPacket = null;

        System.out.println("Server is running...");

        while (true) {
            receiveDatagramPacket = new DatagramPacket(receive, receive.length);
            ds.receive(receiveDatagramPacket);

            String message = data(receive).toString();
            InetAddress clientAddress = receiveDatagramPacket.getAddress();
            int clientPort = receiveDatagramPacket.getPort();

            System.out.println("Received message: " + message);

            String[] parts = message.split(" \\| ");
            String command = parts[0];
            String rqNumber = parts[1];

            if (command.equals("REGISTER")) {
                String name = parts[2];
                String role = parts[3];
                String ipAddress = parts[4];
                int udpPort = Integer.parseInt(parts[5]);
                int tcpPort = Integer.parseInt(parts[6]);

                if (registeredClients.containsKey(name)) {
                    String response = "REGISTER-DENIED | " + rqNumber + " | Name already in use";
                    sendUDPMessage(response, clientAddress, clientPort, ds);
                    System.out.println("Registration denied for: " + name);
                } else {
                    registeredClients.put(name, new ClientInfo(name, role, ipAddress, udpPort, tcpPort));
                    String response = "REGISTERED | " + rqNumber;
                    sendUDPMessage(response, clientAddress, clientPort, ds);
                    System.out.println("Registered: " + name);
                }
            } else if (command.equals("DE-REGISTER")) {
                String name = parts[2];
                if (registeredClients.containsKey(name)) {
                    registeredClients.remove(name);
                    System.out.println("Deregistered: " + name);
                } else {
                    System.out.println("Attempted to deregister non-existent user: " + name);
                }

            } else if (command.equals("LIST_ITEM")) {
                String itemName = parts[2];
                String itemDescrip = parts[3];
                double startPrice;
                int auctDuration;

                try {
                    startPrice = Double.parseDouble(parts[4]);
                    auctDuration = Integer.parseInt(parts[5]);
                } catch (NumberFormatException e) {
                    String response = "LIST-DENIED | " + rqNumber + " | Invalid price or auction duration";
                    sendUDPMessage(response, clientAddress, clientPort, ds);
                    System.out.println("Invalid LIST_ITEM request: " + message);
                    continue;
                }
                if (currentAuctions.containsKey(itemName)) {
                    String response = "LIST-DENIED | " + rqNumber + " | Item already listed";
                    sendUDPMessage(response, clientAddress, clientPort, ds);
                    System.out.println("Item already listed: " + itemName);
                } else {
                    currentAuctions.put(itemName, new AuctionItem(itemName, itemDescrip, startPrice, auctDuration));
                    String response = "ITEM_LISTED | " + rqNumber;
                    sendUDPMessage(response, clientAddress, clientPort, ds);
                    System.out.println("Item listed: " + itemName);

                    
                    // Notify all subscribed clients about the new auction
                    SubscriptionManager.notifySubscribers(itemName, currentAuctions.get(itemName), ds);

                }

            } else if (command.equals("BID")) {
                String itemName = parts[2];
                double bidAmount;
                String bidderName = parts[4];

                try {
                    bidAmount = Double.parseDouble(parts[3]);
                } catch (NumberFormatException e) {
                    String response = "BID_REJECTED | " + rqNumber + " | Invalid bid amount";
                    sendUDPMessage(response, clientAddress, clientPort, ds);
                    System.out.println("Invalid bid amount received: " + message);
                    continue;
                }

                if (!currentAuctions.containsKey(itemName)) {
                    String response = "BID_REJECTED | " + rqNumber + " | No active auction for this item";
                    sendUDPMessage(response, clientAddress, clientPort, ds);
                    System.out.println("Bid rejected: No auction for item " + itemName);
                    continue;
                }

                AuctionItem auction = currentAuctions.get(itemName);

                if (System.currentTimeMillis() > auction.endTime) {
                    String response = "BID_REJECTED | " + rqNumber + " | Auction has ended";
                    sendUDPMessage(response, clientAddress, clientPort, ds);
                    System.out.println("Bid rejected: Auction ended for " + itemName);
                    continue;
                }

                if (bidAmount <= auction.highestBid) {
                    String response = "BID_REJECTED | " + rqNumber + " | Bid too low";
                    sendUDPMessage(response, clientAddress, clientPort, ds);
                    System.out.println("Bid rejected: Too low for " + itemName);
                    continue;
                }

                auction.highestBid = bidAmount;
                auction.highestBidder = bidderName;
                currentAuctions.put(itemName, auction);

                ServerLog.log("Bid received: " + bidderName + " bid $" + bidAmount + " for " + itemName);

                String response = "BID_ACCEPTED | " + rqNumber;
                sendUDPMessage(response, clientAddress, clientPort, ds);
                System.out.println("Bid accepted: " + bidderName + " bid $" + bidAmount + " for " + itemName);

                broadcastBidUpdate(itemName, auction, ds);
            } else {
                System.out.println("Invalid command received.");
            }

            // checks if item available then subscribes client to auction, or unsubscribe from updates

            else if (command.equals("SUBSCRIBE")) {
                    String itemName = parts[2];
                if (!currentAuctions.containsKey(itemName)) {
                    String response = "SUBSCRIPTION-DENIED | " + rqNumber + " | Item not found";
                    sendUDPMessage(response, clientAddress, clientPort, ds);
                System.out.println("Subscription denied: No auction for " + itemName);
            } else {
                SubscriptionManager.subscribe(itemName, registeredClients.get(parts[2]));
                    String response = "SUBSCRIBED | " + rqNumber;
                    sendUDPMessage(response, clientAddress, clientPort, ds);
                System.out.println("Subscription successful: " + parts[2] + " subscribed to " + itemName);
            }
            } else if (command.equals("DE-SUBSCRIBE")) {
                    String itemName = parts[2];
                SubscriptionManager.unsubscribe(itemName, registeredClients.get(parts[2]));
                System.out.println("Unsubscribed: " + parts[2] + " from " + itemName);
        }


            receive = new byte[65535];
        }
    }

    private static void sendUDPMessage(String message, InetAddress address, int port, DatagramSocket socket) throws IOException {
        DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), address, port);
        socket.send(packet);
    }

    private static void broadcastBidUpdate(String itemName, AuctionItem auction, DatagramSocket ds) {
        String message = "BID_UPDATE | " + System.currentTimeMillis() + " | " + itemName + " | "
                + auction.highestBid + " | " + auction.highestBidder + " | " + (auction.endTime - System.currentTimeMillis());


        for (ClientInfo client : registeredClients.values()) {
            try {
                InetAddress clientAddress = InetAddress.getByName(client.ipAddress);
                sendUDPMessage(message, clientAddress, client.udpPort, ds);
            } catch (IOException e) {
                System.out.println("Failed to send bid update to " + client.name);
            }
        }
        System.out.println("Bid update broadcasted for item: " + itemName);
    }

    public static StringBuilder data(byte[] arr) {
        if (arr == null) return null;
        StringBuilder ret = new StringBuilder();
        int i = 0;
        while (i < arr.length && arr[i] != 0) {
            ret.append((char) arr[i]);
            i++;
        }
        return ret;
    }
}
