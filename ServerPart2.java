import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 5000;
    private static Map<String, ClientInfo> registeredClients = new HashMap<>();
    private static Map<String, AuctionItem> currentAuctions = new HashMap<>();

    public static void main(String[] args) throws IOException {
        DatagramSocket ds = new DatagramSocket(PORT);
        byte[] receive = new byte[65535];
        DatagramPacket receiveDatagramPacket;

        System.out.println("Server is running...");

        // Load previous state
        loadState();
        SubscriptionManager.loadState();

        // Save state on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            saveState();
            SubscriptionManager.saveState();
        }));

        // Auction monitor (Task 2.6)
        Map<String, FinalizationSession> sessions = new ConcurrentHashMap<>();
        Thread monitor = new Thread(new AuctionMonitor(currentAuctions, registeredClients, sessions));
        monitor.start();

        // Message router (Task 2.7)
        TransactionFinalizer finalizer = new TransactionFinalizer();
        Thread router = new Thread(new MessageRouter(5002, sessions, finalizer));
        router.start();

        while (true) {
            receiveDatagramPacket = new DatagramPacket(receive, receive.length);
            ds.receive(receiveDatagramPacket);

            String message = data(receive).toString();
            InetAddress clientAddress = receiveDatagramPacket.getAddress();
            int clientPort = receiveDatagramPacket.getPort();
            System.out.println("Received message: " + message);

            String[] parts = message.split(" \\| ");
            if (parts.length < 2) {
                continue;
            }

            String command = parts[0];
            String rqNumber = parts[1];

            switch (command) {
                case "REGISTER":
                    if (parts.length < 7) continue;
                    String name = parts[2];
                    String role = parts[3];
                    String ip = parts[4];
                    int udp = Integer.parseInt(parts[5]);
                    int tcp = Integer.parseInt(parts[6]);

                    if (registeredClients.containsKey(name)) {
                        sendUDPMessage("REGISTER-DENIED | " + rqNumber + " | Name already in use", clientAddress, clientPort, ds);
                    } else {
                        registeredClients.put(name, new ClientInfo(name, role, ip, udp, tcp));
                        sendUDPMessage("REGISTERED | " + rqNumber, clientAddress, clientPort, ds);
                    }
                    break;

                case "DE-REGISTER":
                    registeredClients.remove(parts[2]);
                    break;

                case "LIST_ITEM":
                    if (parts.length < 7) continue;
                    String item = parts[2];
                    String desc = parts[3];
                    double price = Double.parseDouble(parts[4]);
                    int duration = Integer.parseInt(parts[5]);
                    String seller = parts[6];

                    if (currentAuctions.containsKey(item)) {
                        sendUDPMessage("LIST-DENIED | " + rqNumber + " | Item already listed", clientAddress, clientPort, ds);
                    } else {
                        AuctionItem ai = new AuctionItem(item, desc, price, duration, seller, rqNumber);
                        currentAuctions.put(item, ai);
                        sendUDPMessage("ITEM_LISTED | " + rqNumber, clientAddress, clientPort, ds);
                        SubscriptionManager.notifySubscribers(item, ai, ds);

                        String announce = "AUCTION_ANNOUNCE | " + System.currentTimeMillis() + " | " + item + " | " + desc + " | " + price + " | " + duration + " | " + seller;
                        SubscriptionManager.broadcastToAllBuyers(announce, registeredClients.values(), ds);
                    }
                    break;

                case "SUBSCRIBE":
                    if (parts.length < 4) continue;
                    String subItem = parts[2];
                    String client = parts[3];
                    if (!registeredClients.containsKey(client)) {
                        sendUDPMessage("SUBSCRIPTION-DENIED | " + rqNumber + " | Client not registered", clientAddress, clientPort, ds);
                    } else {
                        SubscriptionManager.subscribe(subItem, registeredClients.get(client));
                        sendUDPMessage("SUBSCRIBED | " + rqNumber, clientAddress, clientPort, ds);
                    }
                    break;

                case "DE-SUBSCRIBE":
                    if (parts.length < 4) continue;
                    SubscriptionManager.unsubscribe(parts[2], registeredClients.get(parts[3]));
                    break;

                case "BID":
                    if (parts.length < 5) continue;
                    String bidItem = parts[2];
                    double amount = Double.parseDouble(parts[3]);
                    String bidder = parts[4];

                    if (!registeredClients.containsKey(bidder)) {
                        sendUDPMessage("BID_REJECTED | " + rqNumber + " | Bidder not registered", clientAddress, clientPort, ds);
                        break;
                    }
                    if (!currentAuctions.containsKey(bidItem)) {
                        sendUDPMessage("BID_REJECTED | " + rqNumber + " | No active auction", clientAddress, clientPort, ds);
                        break;
                    }

                    AuctionItem a = currentAuctions.get(bidItem);
                    if (System.currentTimeMillis() > a.endTime || amount <= a.highestBid) {
                        sendUDPMessage("BID_REJECTED | " + rqNumber + " | Invalid bid", clientAddress, clientPort, ds);
                        break;
                    }

                    a.highestBid = amount;
                    a.highestBidder = bidder;
                    a.hasReceivedBid = true;
                    currentAuctions.put(bidItem, a);

                    sendUDPMessage("BID_ACCEPTED | " + rqNumber, clientAddress, clientPort, ds);
                    broadcastBidUpdate(bidItem, a, ds);
                    SubscriptionManager.notifyNewBid(bidItem, bidder, amount, ds);
                    break;
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
    }

    public static StringBuilder data(byte[] arr) {
        StringBuilder ret = new StringBuilder();
        int i = 0;
        while (i < arr.length && arr[i] != 0) {
            ret.append((char) arr[i]);
            i++;
        }
        return ret;
    }
    @SuppressWarnings("unchecked")
    private static void loadState() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("server_state.dat"))) {
            registeredClients = (Map<String, ClientInfo>) in.readObject();
            currentAuctions = (Map<String, AuctionItem>) in.readObject();
            System.out.println("Server state loaded.");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No server state to load or failed to load: " + e.getMessage());
        }
    }

    private static void saveState() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("server_state.dat"))) {
            out.writeObject(registeredClients);
            out.writeObject(currentAuctions);
            System.out.println("Server state saved.");
        } catch (IOException e) {
            System.out.println("Failed to save server state: " + e.getMessage());
        }
    }
}
