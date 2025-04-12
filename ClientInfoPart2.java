import java.io.Serializable;

public class ClientInfo implements Serializable {
    public final String name;
    public final String role;
    public final String ipAddress;
    public final int udpPort;
    public final int tcpPort;

    public ClientInfo(String name, String role, String ipAddress, int udpPort, int tcpPort) {
        this.name = name;
        this.role = role;
        this.ipAddress = ipAddress;
        this.udpPort = udpPort;
        this.tcpPort = tcpPort;
    }

    @Override
    public String toString() {
        return String.format("Client[name=%s, role=%s, ip=%s, udp=%d, tcp=%d]",
                name, role, ipAddress, udpPort, tcpPort);
    }
}
