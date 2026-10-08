import java.math.BigDecimal;
import java.util.Scanner;

public class Customer {
    private final AccountManager manager;
    private final Scanner sc;
    private static final String DATA_FILE = "bank_data.ser";

    public Customer(AccountManager manager, Scanner sc) {
        this.manager = manager;
        this.sc = sc;
    }
}
