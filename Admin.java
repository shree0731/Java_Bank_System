import java.math.BigDecimal;
import java.util.Map;
import java.util.Scanner;

public class Admin {
    private final AccountManager manager;
    private final Scanner sc;
    private static final String DATA_FILE = "bank_data.ser";

    public Admin(AccountManager manager, Scanner sc) {
        this.manager = manager;
        this.sc = sc;
    }
}
