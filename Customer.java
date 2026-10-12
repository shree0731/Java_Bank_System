
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Customer {
    private final AccountManager manager;
    private final Scanner sc;
    private static final String DATA_FILE = "bank_data.ser";

    public Customer(AccountManager manager, Scanner sc) {
        this.manager = manager;
        this.sc = sc;
    }

    /* ---------------- Public flows invoked by BankSystem ---------------- */

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
                customerMenu(acc);
            }
        } catch (Exception e) {
            System.err.println("Authentication error: " + e.getMessage());
        }
    }

    // Called from BankSystem when user chooses "Create Account"
    public void createAccountCLI() {
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

        // initial deposit loop
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

    /* ---------------- Customer menu and actions ---------------- */

    private void customerMenu(Account acc) {
        while (true) {
            System.out.println("\nCustomer Menu:");
            System.out.println("1. Balance Inquiry");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Mini-Statement");
            System.out.println("6. Export Statement to CSV");
            System.out.println("7. Change Password");
            System.out.println("8. Logout");
            System.out.print("Choice: ");
            String ch = sc.nextLine().trim();
            switch (ch) {
                case "1":
                    System.out.println("Balance: " + acc.getBalance().toPlainString());
                    break;
                case "2":
                    depositFlow(acc);
                    break;
                case "3":
                    withdrawFlow(acc);
                    break;
                case "4":
                    transferFlow(acc);
                    break;
                case "5":
                    printMiniStatement(acc);
                    break;
                case "6":
                    exportCSV(acc);
                    break;
                case "7":
                    changePasswordFlow(acc);
                    break;
                case "8":
                    System.out.println("Logging out.");
                    manager.save(DATA_FILE);
                    return;
                default:
                    System.out.println("Invalid choice.");
                    break;
            }
        }
    }

    private void depositFlow(Account acc) {
        while (true) {
            System.out.print("Enter deposit amount (or 0 to cancel): ");
            String s = sc.nextLine().trim();
            if (s.equalsIgnoreCase("0") || s.equalsIgnoreCase("c")) {
                System.out.println("Deposit cancelled.");
                return;
            }
            try {
                BigDecimal amt = parseAmount(s);
                if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println("Amount must be positive.");
                    continue;
                }
                if (acc.deposit(amt)) {
                    System.out.println("Deposit successful. New balance: " + acc.getBalance().toPlainString());
                    manager.save(DATA_FILE);
                    return;
                } else {
                    System.out.println("Invalid deposit amount.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number (supports suffix k/K/m/M).");
            }
        }
    }

    private void withdrawFlow(Account acc) {
        while (true) {
            System.out.print("Enter withdrawal amount (or 0 to cancel): ");
            String s = sc.nextLine().trim();
            if (s.equalsIgnoreCase("0") || s.equalsIgnoreCase("c")) {
                System.out.println("Withdrawal cancelled.");
                return;
            }
            try {
                BigDecimal amt = parseAmount(s);
                if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println("Amount must be positive.");
                    continue;
                }
                if (acc.withdraw(amt)) {
                    System.out.println("Withdrawal successful. New balance: " + acc.getBalance().toPlainString());
                    manager.save(DATA_FILE);
                    return;
                } else {
                    System.out.println("Insufficient funds or invalid amount.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number (supports suffix k/K/m/M).");
            }
        }
    }

    private void transferFlow(Account acc) {
        System.out.print("Enter target account number (or 0 to cancel): ");
        String target = sc.nextLine().trim();
        if (target.equalsIgnoreCase("0") || target.equalsIgnoreCase("c")) {
            System.out.println("Transfer cancelled.");
            return;
        }
        if (manager.getAccount(target) == null) {
            System.out.println("Target account not found.");
            return;
        }
        while (true) {
            System.out.print("Enter amount to transfer (or 0 to cancel): ");
            String s = sc.nextLine().trim();
            if (s.equalsIgnoreCase("0") || s.equalsIgnoreCase("c")) {
                System.out.println("Transfer cancelled.");
                return;
            }
            try {
                BigDecimal amt = parseAmount(s);
                if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println("Amount must be positive.");
                    continue;
                }
                System.out.print("Confirm transfer of " + amt.toPlainString() + " to " + target + " (Y/N): ");
                String conf = sc.nextLine().trim();
                if (!conf.equalsIgnoreCase("Y")) { System.out.println("Transfer cancelled."); return; }
                boolean ok = manager.transfer(acc.getAccountNumber(), target, amt);
                if (ok) {
                    System.out.println("Transfer successful. New balance: " + acc.getBalance().toPlainString());
                    manager.save(DATA_FILE);
                    return;
                } else {
                    System.out.println("Transfer failed (insufficient funds or invalid amount).");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number (supports suffix k/K/m/M).");
            }
        }
}