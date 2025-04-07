import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class MessageRouter implements Runnable {
    private final int tcpPort;
    private final Map<String, FinalizationSession> sessions;
    private final TransactionFinalizer finalizer;

    public MessageRouter(int tcpPort, Map<String, FinalizationSession> sessions, TransactionFinalizer finalizer) {
        this.tcpPort = tcpPort;
        this.sessions = sessions;
        this.finalizer = finalizer;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(tcpPort)) {
            System.out.println("MessageRouter listening on TCP port " + tcpPort);
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleMessage(socket)).start();
            }
        } catch (IOException e) {
            System.out.println("MessageRouter error: " + e.getMessage());
        }
    }

    private void handleMessage(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String msg = in.readLine();
            ServerLog.log("Router received: " + msg);

            if (msg == null || !msg.startsWith("INFORM_Res")) {
                out.println("CANCEL UNKNOWN Invalid message format");
                return;
            }

            String[] parts = msg.split(" ", 6);
            if (parts.length < 6) {
                out.println("CANCEL UNKNOWN Incomplete INFORM_Res");
                return;
            }

            String rq = parts[1];
            String name = parts[2];
            String cc = parts[3];
            String exp = parts[4];
            String address = parts[5];

            FinalizationSession session = sessions.get(rq);
            if (session == null) {
                out.println("CANCEL " + rq + " Invalid RQ#");
                return;
            }

            synchronized (session) {
                if (!session.buyerReady && session.buyerName == null) {
                    session.buyerName = name;
                    session.buyerCC = cc;
                    session.buyerExp = exp;
                    session.buyerAddress = address;
                    session.buyerSocket = socket;
                    session.buyerReady = true;
                    System.out.println("Buyer INFORM_Res stored for RQ#: " + rq);
                } else if (!session.sellerReady && session.sellerName == null) {
                    session.sellerName = name;
                    session.sellerCC = cc;
                    session.sellerExp = exp;
                    session.sellerAddress = address;
                    session.sellerSocket = socket;
                    session.sellerReady = true;
                    System.out.println("Seller INFORM_Res stored for RQ#: " + rq);
                } else {
                    out.println("CANCEL " + rq + " Unexpected duplicate or extra response");
                    socket.close();
                    return;
                }

                if (session.isReady()) {
                    System.out.println("Both buyer and seller are ready. Starting finalization.");
                    finalizer.process(session);
                }
            }

        } catch (IOException e) {
            System.out.println("Error handling INFORM_Res: " + e.getMessage());
        }
    }
}
