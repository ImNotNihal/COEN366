// practice for registration/de-registration to the server. the console only reads that the message has been received and reigstered, and same thing when the message received is de-registered
// usage of DatagramSocket used to send/receive datagram packets
// Creation of DatagramPacket: creates the packet for sending/receiving data

package PeerToPeerProject366;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName(SERVER_ADDRESS);

            // Register
            String registerMessage = "REGISTER";
            byte[] registerBuffer = registerMessage.getBytes();
            DatagramPacket registerPacket = new DatagramPacket(registerBuffer, registerBuffer.length, serverAddress, SERVER_PORT);
            socket.send(registerPacket);
            System.out.println("Sent registration request");

            // Deregister
            String deregisterMessage = "DEREGISTER";
            byte[] deregisterBuffer = deregisterMessage.getBytes();
            DatagramPacket deregisterPacket = new DatagramPacket(deregisterBuffer, deregisterBuffer.length, serverAddress, SERVER_PORT);
            socket.send(deregisterPacket);
            System.out.println("Sent deregistration request");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
