import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ActivityLog {
    private static final String LOG_FILE = "activity_log.txt";

    public static void log(String message) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(message);
        } catch (IOException e) {
            System.out.println("Error writing to activity log.");
        }
    }
}
