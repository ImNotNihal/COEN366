public class ClientInfo {
    String name, role, ipAddress;
    int udpPort, tcpPort;

    ClientInfo(String name, String role, String ipAddress, int udpPort, int tcpPort) {
        this.name = name;
        this.role = role;
        this.ipAddress = ipAddress;
        this.udpPort = udpPort;
        this.tcpPort = tcpPort;
    }
}
