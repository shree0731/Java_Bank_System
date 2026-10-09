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

    public void login() {
        System.out.print("Enter account number: ");
        String accNum = sc.nextLine().trim();
        Account acc = manager.getAccount(accNum);
        if (acc == null) {
            System.out.println("Account not found.");
            return;
        }
        if (acc.isLocked()) {
            System.out.println("Account is locked due to multiple failed login attempts.");
            return;
        }

        System.out.print("Enter password: ");
        String pwd = sc.nextLine();

        try {
            boolean ok = PasswordUtil.verifyPassword(pwd.toCharArray(), acc.getSalt(), acc.getPasswordHash());
            if (!ok) {
                acc.incrementFailedLogins();
                int remaining = Math.max(0, 3 - acc.getFailedLogins());
                manager.save(DATA_FILE);
                System.out.println("Invalid password. Remaining attempts before lock: " + remaining);
                if (acc.isLocked()) System.out.println("Account locked after too many failed attempts.");
                return;
            } else {
                acc.resetFailedLogins();
                manager.save(DATA_FILE);
                System.out.println("Login successful. Hello, " + acc.getHolderName());
            }
        } catch (Exception e) {
            System.err.println("Authentication error: " + e.getMessage());
        }
    }
}
