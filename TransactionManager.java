import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TransactionManager {
    private static final String TRANSACTION_LOG_FILE = "transactions.txt";

    public static void logTransaction(String itemName, String seller, String winner, double finalPrice) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String entry = String.format("Item: %s | Seller: %s | Winner: %s | Price: %.2f | Time: %s",
                itemName, seller, winner, finalPrice, timestamp);
        try (FileWriter writer = new FileWriter(TRANSACTION_LOG_FILE, true)) {
            writer.write(entry + System.lineSeparator());
            System.out.println("Transaction logged: " + entry);
        } catch (IOException e) {
            System.err.println("Failed to log transaction: " + e.getMessage());
        }
    }
}
