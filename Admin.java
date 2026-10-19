
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
            System.out.println("\nAdmin Menu:");
            System.out.println("1. List Accounts");
            System.out.println("2. Create Account (admin)");
            System.out.println("3. Delete Account");
            System.out.println("4. Unlock Account");
            System.out.println("5. View Account Statement");
            System.out.println("6. Admin Summary");
            System.out.println("7. Logout");
            System.out.print("Choice: ");
            String ch = sc.nextLine().trim();
            switch (ch) {
                case "1": listAccounts(); break;
                case "2": createAccountCLI(); break;
                case "3": deleteAccountCLI(); break;
                case "4": unlockAccountCLI(); break;
                case "5": viewAccountStatementCLI(); break;
                case "6": adminSummary(); break;
                case "7":
                    System.out.println("Admin logout.");
                    manager.save(DATA_FILE);
                    return;
                default:
                    System.out.println("Invalid choice."); break;
            }
        }
    }

    private void listAccounts() {
        System.out.println("Accounts:");
        Map<String, Account> all = manager.getAllAccounts();
        for (Account a : all.values()) {
            if ("admin".equals(a.getAccountNumber())) continue;
            System.out.printf("- %s | %s | Balance: %s | Locked: %s\n",
                    a.getAccountNumber(), a.getHolderName(), a.getBalance().toPlainString(), a.isLocked());
        }
    }

    private void createAccountCLI() {
        System.out.print("Enter holder name: ");
        String name = sc.nextLine().trim();
    }
}