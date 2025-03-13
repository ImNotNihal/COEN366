// Server is running, gets the message from the client, takes the IP/Path address of the client and suggests that the client has been registered or not

package PeerToPeerProject366;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

public class Server {
    private static final int PORT = 5000;
    private static Map<String, ClientInfo> registeredClients = new HashMap<>();

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

    public static void main(String[] args) throws IOException {
        DatagramSocket ds = new DatagramSocket(PORT);
        byte[] receive = new byte[65535];
        DatagramPacket DpReceive = null;

        System.out.println("Server is running...");

        while (true) {
            DpReceive = new DatagramPacket(receive, receive.length);
            ds.receive(DpReceive);

            String message = data(receive).toString();
            InetAddress clientAddress = DpReceive.getAddress();
            int clientPort = DpReceive.getPort();

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
                registeredClients.remove(name);
                System.out.println("Deregistered: " + name);
            }

            // clear buffer
            receive = new byte[65535];

            if (message.equals("bye")) {
                System.out.println("Client sent bye.....EXITING");
                break;
            }
        }
    }

    // convert byte array data into a string representation
    public static StringBuilder data(byte[] a) {
        if (a == null) return null;
        StringBuilder ret = new StringBuilder();
        int i = 0;
        while (i < a.length && a[i] != 0) {
            ret.append((char) a[i]);
            i++;
        }
        return ret;
    }
}
