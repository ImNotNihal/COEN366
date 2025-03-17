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

    static class AuctionItem{

        String itemName;
        String itemDescrip;
        double startPrice;
        int auctDuration;

        AuctionItem(String itemName, String itemDescrip, double startPrice, int auctDuration){

            this.itemName = itemName;
            this.itemDescrip = itemDescrip;
            this.startPrice = startPrice;
            this.auctDuration = auctDuration;

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
                    DatagramPacket responsePacket = new DatagramPacket(response.getBytes(), response.length(), clientAddress, clientPort);
                    ds.send(responsePacket);
                    System.out.println("Registration denied for: " + name);
                } else {
                    registeredClients.put(name, new ClientInfo(name, role, ipAddress, udpPort, tcpPort));
                    String response = "REGISTERED | " + rqNumber;
                    DatagramPacket responsePacket = new DatagramPacket(response.getBytes(), response.length(), clientAddress, clientPort);
                    ds.send(responsePacket);
                    System.out.println("Registered: " + name);
                }
            } else if (command.equals("DE-REGISTER")) {
                String name = parts[2];
                if(registeredClients.containsKey(name)){
                registeredClients.remove(name);
                System.out.println("Deregistered: " + name);
                } else {
                    System.out.println("Attempted to deregister non-existent user: " + name);
                }

            } else if (command.equals("LIST_ITEM")){

                String itemName = parts[2];
                String itemDescrip = parts[3];
                double startPrice;
                int auctDuration;

                try{

                    startPrice = Double.parseDouble(parts[4]);
                    auctDuration = Integer.parseInt(parts[5]);

                } catch (NumberFormatException e){

                    String response = "LIST-DENIED | " + rqNumber + " | Invalid price or auction duration";
                    DatagramPacket responsePacket = new DatagramPacket(response.getBytes(), response.length(),clientAddress, clientPort);
                    ds.send(responsePacket);
                    System.out.println("Invalid LIST_ITEM request: " + message);
                    continue;

                } if(currentAuctions.containsKey(itemName)){

                    String response = "LIST-DENIED | " + rqNumber + " | Item already listed";
                    DatagramPacket responsePacket = new DatagramPacket(response.getBytes(), response.length(), clientAddress, clientPort);
                    ds.send(responsePacket);
                    System.out.println("Item already listed: " + itemName);

                } else {

                    currentAuctions.put(itemName, new AuctionItem(itemName, itemDescrip, startPrice, auctDuration));
                    String response = "ITEM_LISTED | " + rqNumber;
                    DatagramPacket responsePacket = new DatagramPacket(response.getBytes(), response.length(), clientAddress, clientPort);
                    ds.send(responsePacket);
                    System.out.println("Item listed: " + itemName);

                }

            }
            
            else {
                System.out.println("Invalid command received.");
            }

            receive = new byte[65535];

        }
    }

    // convert byte array data into a string representation
    public static StringBuilder data(byte[] arr){

        if(arr==null) return null;
        StringBuilder ret = new StringBuilder();
        int i =0;

        while(i < arr.length && arr[i] != 0){

            ret.append((char)arr[i]);
            i++;

        }

        return ret;

    }
}
