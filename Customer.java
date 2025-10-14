
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

    private void printMiniStatement(Account acc) {
        System.out.println("Mini-statement for " + acc.getAccountNumber() + " (" + acc.getHolderName() + "):");
        List<Transaction> hist = acc.getHistory();
        int start = Math.max(0, hist.size() - 10);
        for (int i = hist.size() - 1; i >= start; i--) {
            System.out.println(hist.get(i));
        }
    }

    private void exportCSV(Account acc) {
        String filename = "statement_" + acc.getAccountNumber() + "_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".csv";
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("TransactionID,Timestamp,Type,Amount,Note");
            for (Transaction t : acc.getHistory()) pw.println(t.toCSV());
            System.out.println("CSV exported to: " + filename);
        } catch (IOException e) {
            System.out.println("Failed to export CSV: " + e.getMessage());
        }
    }

    private void changePasswordFlow(Account acc) {
        System.out.print("Enter current password (or 0 to cancel): ");
        String cur = sc.nextLine().trim();
        if (cur.equals("0")) { System.out.println("Password change cancelled."); return; }
        try {
            boolean ok = PasswordUtil.verifyPassword(cur.toCharArray(), acc.getSalt(), acc.getPasswordHash());
            if (!ok) { System.out.println("Current password incorrect."); return; }
        } catch (Exception e) { System.out.println("Authentication error."); return; }

        while (true) {
            System.out.print("Enter new password (min 8 chars, includes digit and letter) or 0 to cancel: ");
            String np = sc.nextLine().trim();
            if (np.equals("0")) { System.out.println("Password change cancelled."); return; }
            if (!isPasswordStrong(np)) {
                System.out.println("Password too weak. Use at least 8 chars including letters and digits.");
                continue;
            }
            System.out.print("Confirm new password: ");
            String cp = sc.nextLine().trim();
            if (!np.equals(cp)) {
                System.out.println("Passwords do not match. Try again.");
                continue;
            }
            try {
                byte[] newSalt = PasswordUtil.generateSalt();
                byte[] newHash = PasswordUtil.hashPassword(np.toCharArray(), newSalt);
                acc.setPasswordHashAndSalt(newHash, newSalt);
                manager.save(DATA_FILE);
                System.out.println("Password changed successfully.");
                return;
            } catch (Exception e) {
                System.out.println("Failed to set new password: " + e.getMessage());
                return;
            }
        }
    }

    private BigDecimal parseAmount(String s) throws NumberFormatException {
        if (s == null) throw new NumberFormatException("null");
        s = s.trim().replace(",", "").toLowerCase();
        if (s.endsWith("k") || s.endsWith("m")) {
            char suffix = s.charAt(s.length() - 1);
            String num = s.substring(0, s.length() - 1).trim();
            BigDecimal base = new BigDecimal(num);
            if (suffix == 'k') return base.multiply(new BigDecimal("1000"));
            else return base.multiply(new BigDecimal("1000000"));
        } else {
            return new BigDecimal(s);
        }
    }

    private boolean isPasswordStrong(String pwd) {
        if (pwd == null) return false;
        if (pwd.length() < 8) return false;
        boolean hasDigit = false, hasLetter = false;
        for (char c : pwd.toCharArray()) {
            if (Character.isDigit(c)) hasDigit = true;
            if (Character.isLetter(c)) hasLetter = true;
        }
        return hasDigit && hasLetter;
    }
}