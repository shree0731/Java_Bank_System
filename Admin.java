
import java.math.BigDecimal;
//import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Admin {
    private final AccountManager manager;
    private final Scanner sc;
    private static final String DATA_FILE = "bank_data.ser";
    //private static final int LOCKOUT_THRESHOLD = 3;

    public Admin(AccountManager manager, Scanner sc) {
        this.manager = manager;
        this.sc = sc;
    }

    // Admin login (verifies against pseudo-account "admin")
    public void login() {
        System.out.print("Enter admin username: ");
        String user = sc.nextLine().trim();
        if (!"admin".equalsIgnoreCase(user)) {
            System.out.println("Unknown admin username.");
            return;
        }

        Account adminAcc = manager.getAccount("admin");
        if (adminAcc == null) {
            System.out.println("Admin not configured.");
            return;
        }

        System.out.print("Enter admin password: ");
        String pwd = sc.nextLine();

        try {
            boolean ok = PasswordUtil.verifyPassword(pwd.toCharArray(), adminAcc.getSalt(), adminAcc.getPasswordHash());
            if (!ok) {
                System.out.println("Invalid admin password.");
                return;
            }
            System.out.println("Admin login successful.");
            adminMenu();
        } catch (Exception e) {
            System.err.println("Authentication error: " + e.getMessage());
        }
    }

    private void adminMenu() {
        while (true) {
    }
}