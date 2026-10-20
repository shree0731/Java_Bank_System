
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
        if (name.isEmpty()) { System.out.println("Name cannot be empty."); return; }

        String pwd;
        while (true) {
            System.out.print("Enter password (min 8 chars incl. letters & digits) or '0' to cancel: ");
            pwd = sc.nextLine().trim();
            if (pwd.equals("0")) { System.out.println("Account creation cancelled."); return; }
            if (!isPasswordStrong(pwd)) {
                System.out.println("Password too weak. Use at least 8 characters with letters and digits.");
                continue;
            }
            System.out.print("Confirm password: ");
            String cp = sc.nextLine().trim();
            if (!pwd.equals(cp)) {
                System.out.println("Passwords do not match. Try again.");
                continue;
            }
            break;
        }

        while (true) {
            System.out.print("Initial deposit amount (supports 20k, 1.5M) or 0 to cancel: ");
            String s = sc.nextLine().trim();
            if (s.equalsIgnoreCase("0") || s.equalsIgnoreCase("c")) {
                System.out.println("Account creation cancelled.");
                return;
            }
            try {
                BigDecimal initial = parseAmount(s);
                if (initial.compareTo(BigDecimal.ZERO) < 0) { System.out.println("Initial deposit cannot be negative."); continue; }
                try {
                    String accNum = manager.createAccount(name, pwd, initial);
                    manager.save(DATA_FILE);
                    System.out.println("Account created successfully. Account Number: " + accNum);
                    return;
                } catch (Exception e) {
                    System.out.println("Account creation error: " + e.getMessage());
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number for initial deposit (supports k/M).");
            }
        }
    }

    private void deleteAccountCLI() {
        System.out.print("Enter account number to delete: ");
        String acc = sc.nextLine().trim();
        if (acc.equals("admin")) { System.out.println("Cannot delete admin."); return; }
        boolean ok = manager.deleteAccount(acc);
        if (ok) { System.out.println("Account deleted."); manager.save(DATA_FILE); }
        else System.out.println("Account not found.");
    }

    private void unlockAccountCLI() {
        System.out.print("Enter account number to unlock: ");
        String acc = sc.nextLine().trim();
        Account a = manager.getAccount(acc);
        if (a == null) { System.out.println("Account not found."); return; }
        a.unlock();
        manager.save(DATA_FILE);
        System.out.println("Account unlocked.");
    }

    private void viewAccountStatementCLI() {
        System.out.print("Enter account number to view statement: ");
        String acc = sc.nextLine().trim();
        Account a = manager.getAccount(acc);
        if (a == null) { System.out.println("Account not found."); return; }
        System.out.println("Full statement for " + a.getAccountNumber() + " (" + a.getHolderName() + "):");
    }
}