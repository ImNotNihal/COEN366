// practice for registration/de-registration to the server. the console only reads that the message has been received and reigstered, and same thing when the message received is de-registered
// usage of DatagramSocket used to send/receive datagram packets
// Creation of DatagramPacket: creates the packet for sending/receiving data

package PeerToPeerProject366;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;
import java.io.IOException;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);
        DatagramSocket ds = new DatagramSocket();
        InetAddress ip = InetAddress.getByName(SERVER_ADDRESS);
        byte[] buf = null;

        // registration details
        String rqNumber = "1";
        String name = "JohnDoe";
        String role = "buyer";
        String ipAddress = ip.getHostAddress();
        int udpPort = ds.getLocalPort();
        int tcpPort = 6000;

        // register
        String registerMessage = "REGISTER | " + rqNumber + " | " + name + " | " + role + " | " + ipAddress + " | " + udpPort + " | " + tcpPort;
        buf = registerMessage.getBytes();
        DatagramPacket DpSend = new DatagramPacket(buf, buf.length, ip, SERVER_PORT);
        ds.send(DpSend);
        System.out.println("Sent registration request");

        // de-register
        String deregisterMessage = "DE-REGISTER | " + rqNumber + " | " + name;
        buf = deregisterMessage.getBytes();
        DpSend = new DatagramPacket(buf, buf.length, ip, SERVER_PORT);
        ds.send(DpSend);
        System.out.println("Sent deregistration request");

        while (true) {
            String inp = sc.nextLine();

            // convert string input into byte array
            buf = inp.getBytes();

            DpSend = new DatagramPacket(buf, buf.length, ip, SERVER_PORT);
            ds.send(DpSend);

            // break loop 
            if (inp.equals("bye")) {
                break;
            }
        }
    }
}
