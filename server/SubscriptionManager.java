import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



// Manages auction subscriptions and notifications for clients 
public class SubscriptionManager {
    private static final Map<String, Set<ClientInfo>> subscribers = new HashMap<>();

    public static void subscribe(String itemName, ClientInfo client) {
        subscribers.computeIfAbsent(itemName, k -> new HashSet<>()).add(client);
        System.out.println(client.name + " subscribed to " + itemName);
    }

    public static void unsubscribe(String itemName, ClientInfo client) {
        if (subscribers.containsKey(itemName)) {
            subscribers.get(itemName).remove(client);
            System.out.println(client.name + " unsubscribed from " + itemName);
        }
    }

    public static void notifySubscribers(String itemName, AuctionItem item, DatagramSocket ds) {
        if (!subscribers.containsKey(itemName)) {
            return;
        }

      //Announcement of auction for the subscribed clients
        String message = "AUCTION_ANNOUNCE | " + System.currentTimeMillis() + " | " +
                itemName + " | " + item.itemDescrip + " | " + item.highestBid + " | " + (item.endTime - System.currentTimeMillis());

        for (ClientInfo client : subscribers.get(itemName)) {
            try {
                InetAddress clientAddress = InetAddress.getByName(client.ipAddress);
                DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), clientAddress, client.udpPort);
                ds.send(packet);
                System.out.println("Sent auction announcement to " + client.name + " for " + itemName);
            } catch (IOException e) {
                System.out.println("Failed to send auction announcement to " + client.name);
            }
        }
    }
}
