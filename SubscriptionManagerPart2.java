import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.*;

// Manages auction subscriptions and notifications for clients
public class SubscriptionManager {
    private static final Map<String, Set<ClientInfo>> subscribers = new HashMap<>();

    public static void subscribe(String itemName, ClientInfo client) {
        if (itemName == null || client == null) {
            System.out.println("Invalid subscription request: itemName or client is null");
            return;
        }
        subscribers.computeIfAbsent(itemName, k -> new HashSet<>()).add(client);
        System.out.println(client.name + " subscribed to " + itemName);
    }

    public static void unsubscribe(String itemName, ClientInfo client) {
        if (itemName == null || client == null) {
            System.out.println("Invalid unsubscription request: itemName or client is null");
            return;
        }
        if (subscribers.containsKey(itemName)) {
            subscribers.get(itemName).remove(client);
            System.out.println(client.name + " unsubscribed from " + itemName);
        }
    }

    public static void notifySubscribers(String itemName, AuctionItem item, DatagramSocket ds) {
        if (!subscribers.containsKey(itemName)) return;

        String message = "AUCTION_ANNOUNCE | " + System.currentTimeMillis() + " | " +
                item.itemName + " | " + item.itemDescrip + " | " + item.highestBid + " | " +
                (item.endTime - System.currentTimeMillis()) + " | " + item.sellerName;

        for (ClientInfo client : subscribers.get(itemName)) {
            try {
                InetAddress address = InetAddress.getByName(client.ipAddress);
                DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), address, client.udpPort);
                ds.send(packet);
                System.out.println("Sent auction announcement to " + client.name + " for " + itemName);
            } catch (IOException e) {
                System.out.println("Failed to send auction announcement to " + client.name + ": " + e.getMessage());
            }
        }
    }

    public static void notifyNewBid(String itemName, String bidder, double amount, DatagramSocket ds) {
        String message = "NEW_BID | " + System.currentTimeMillis() + " | " + itemName + " | " + bidder + " | " + amount;
        for (ClientInfo client : getSubscribers(itemName)) {
            try {
                InetAddress address = InetAddress.getByName(client.ipAddress);
                DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), address, client.udpPort);
                ds.send(packet);
                System.out.println("Notified " + client.name + " of new bid on " + itemName);
            } catch (IOException e) {
                System.out.println("Failed to notify " + client.name + " of new bid: " + e.getMessage());
            }
        }
    }

    public static Set<ClientInfo> getSubscribers(String itemName) {
        return subscribers.getOrDefault(itemName, new HashSet<>());
    }

    public static void broadcastToAllBuyers(String message, Collection<ClientInfo> clients, DatagramSocket ds) {
        for (ClientInfo client : clients) {
            if ("buyer".equals(client.role)) {
                try {
                    InetAddress address = InetAddress.getByName(client.ipAddress);
                    DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), address, client.udpPort);
                    ds.send(packet);
                    System.out.println("Broadcasted to buyer: " + client.name);
                } catch (IOException e) {
                    System.out.println("Broadcast failed to " + client.name);
                }
            }
        }
    }

    // methods for subscription state
    public static void saveState() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("subscriptions.dat"))) {
            out.writeObject(subscribers);
            System.out.println("Subscriptions saved.");
        } catch (IOException e) {
            System.out.println("Failed to save subscriptions: " + e.getMessage());
        }
    }

    public static void loadState() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("subscriptions.dat"))) {
            Map<String, Set<ClientInfo>> loaded = (Map<String, Set<ClientInfo>>) in.readObject();
            subscribers.clear();
            subscribers.putAll(loaded);
            System.out.println("Subscriptions loaded.");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No previous subscriptions to load or failed to load: " + e.getMessage());
        }
    }
}


