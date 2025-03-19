// practice for registration/de-registration to the server. the console only reads that the message has been received and registered, and same thing when the message received is de-registered
// usage of DatagramSocket used to send/receive datagram packets
// Creation of DatagramPacket: creates the packet for sending/receiving data

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);
        DatagramSocket ds = new DatagramSocket();
        InetAddress ip = InetAddress.getByName(SERVER_ADDRESS);

        while (true) {

            System.out.print("Enter command (REGISTER, DE-REGISTER, LIST_ITEM, BID, or bye): ");
            String cmd = sc.nextLine();
            if (cmd.equalsIgnoreCase("bye")) {
                break;
            }

            if(cmd.equalsIgnoreCase("REGISTER")) {
                System.out.print("Enter RQ#: ");
                String rqNumber = sc.nextLine();
                System.out.print("Enter name: ");
                String name = sc.nextLine();
                System.out.print("Enter role: ");
                String role = sc.nextLine();
                String ipAddress = ip.getHostAddress();
                int udpPort = ds.getLocalPort();
                int tcpPort = udpPort + 1;

                String rMessage = "REGISTER | " + rqNumber + " | " + name + " | " + role + " | " + ipAddress + " | " + udpPort + " | " + tcpPort;
                sendUDPMessage(rMessage, ds, ip);

            } else if (cmd.equalsIgnoreCase("DE-REGISTER")) {
                System.out.print("Enter RQ#: ");
                String rqNumber = sc.nextLine();
                System.out.print("Enter name: ");
                String name = sc.nextLine();

                String drMessage = "DE-REGISTER | " + rqNumber + " | " + name;
                sendUDPMessage(drMessage, ds, ip);

            } else if (cmd.equalsIgnoreCase("LIST_ITEM")) {
                System.out.print("Enter RQ#: ");
                String rqNumber = sc.nextLine();
                System.out.print("Enter item name: ");
                String itemName = sc.nextLine();
                System.out.print("Enter item description: ");
                String itemDescrip = sc.nextLine();
                System.out.print("Enter starting price: ");
                String startPrice = sc.nextLine();
                System.out.print("Enter duration of auction (sec): ");
                String auctDuration = sc.nextLine();

                String liMessage = "LIST_ITEM | " + rqNumber + " | " + itemName + " | " + itemDescrip + " | " + startPrice + " | " + auctDuration;
                sendUDPMessage(liMessage, ds, ip);

            } else if (cmd.equalsIgnoreCase("BID")) {
                System.out.print("Enter RQ#: ");
                String rqNumber = sc.nextLine();
                System.out.print("Enter item name: ");
                String itemName = sc.nextLine();
                System.out.print("Enter bid amount: ");
                String bidAmount = sc.nextLine();
                System.out.print("Enter your name: ");
                String bidderName = sc.nextLine();

                String bidMessage = "BID | " + rqNumber + " | " + itemName + " | " + bidAmount + " | " + bidderName;
                sendUDPMessage(bidMessage, ds, ip);

                // Receive server response
                byte[] bufferReceiver = new byte[65535];
                DatagramPacket receiveDatagramPacket = new DatagramPacket(bufferReceiver, bufferReceiver.length);
                ds.receive(receiveDatagramPacket);

                String response = data(bufferReceiver).toString();
                System.out.println("Server response: " + response);
            }

        }

        ds.close();
    }

    // ✅ Method to send UDP messages
    private static void sendUDPMessage(String message, DatagramSocket ds, InetAddress ip) throws IOException {
        byte[] buf = message.getBytes();
        DatagramPacket sendDatagramPacket = new DatagramPacket(buf, buf.length, ip, SERVER_PORT);
        ds.send(sendDatagramPacket);
        System.out.println("Sent: " + message);
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
