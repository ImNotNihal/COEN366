// Server is running, gets the message from the client, takes the IP/Path address of the client and suggests that the client has been registered or not

package PeerToPeerProject366;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class Server {
    private static final int PORT = 5000;
    private static Set<String> registeredClients = new HashSet<>();

    static class ClientInfo {
        String name;
        String role;
        String ipAddress;
        int udpPort;
        int tcpPort;

        ClientInfo(String name, String role, String ipAddress, int udpPort, int tcpPort) {
            this.name = name;
            this.role = role;
            ipAddress;
            this.udpPort = udpPort;
            this.tcpPort = tcpPort;
        }
    }

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            byte[] buffer = new byte[1024];
            System.out.println("Server is running...");

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();

                if (message.startsWith("REGISTER")) {
                    registeredClients.add(clientAddress.toString() + ":" + clientPort);
                    System.out.println("Registered: " + clientAddress + ":" + clientPort);
                } else if (message.startsWith("DEREGISTER")) {
                    registeredClients.remove(clientAddress.toString() + ":" + clientPort);
                    System.out.println("Deregistered: " + clientAddress + ":" + clientPort);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
