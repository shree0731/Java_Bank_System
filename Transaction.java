
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private final String timestamp;
    private final String type; // OPEN, DEPOSIT, WITHDRAW, TRANSFER_OUT, TRANSFER_IN, PW_CHANGE
    private final BigDecimal amount; 
    private final String note;

    public Transaction(String type, BigDecimal amount, String note) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.type = type;
        this.amount = amount;
        this.note = note;
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s | %s | %s",
                id, timestamp, type, (amount == null ? "-" : amount.toPlainString()), note);
    }

    public String toCSV() {
        String amt = (amount == null ? "" : amount.toPlainString());
        return String.join(",", id, timestamp, type, amt, "\"" + note.replace("\"", "\"\"") + "\"");
    }
}