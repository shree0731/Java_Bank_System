
import java.util.Scanner;

public class BankSystem {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        AccountManager manager = AccountManager.load("bank_data.ser");
        ensureSampleAccounts(manager);

        Customer customer = new Customer(manager, sc);
        Admin admin = new Admin(manager, sc);

        while (true) {
            System.out.println("\n1. Customer Login 2. Create Account 3. Admin Login 4. Exit");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1": customer.login(); break;
                case "2": customer.createAccountCLI(); break;
                case "3": admin.login(); break;
                case "4": manager.save("bank_data.ser"); System.out.println("Goodbye!"); return;
                default: System.out.println("Invalid choice."); break;
            }
        }
    }

    private static void ensureSampleAccounts(AccountManager manager) {
        if (manager.getAllAccounts().isEmpty()) {
            try {
                manager.createAccount("Alice", "alice123", new java.math.BigDecimal("5000"));
                manager.createAccount("Bob", "bob456", new java.math.BigDecimal("3000"));
                byte[] salt = PasswordUtil.generateSalt();
                byte[] hash = PasswordUtil.hashPassword("adminpass".toCharArray(), salt);
                manager.addAccount(new Account("admin", "Administrator", hash, salt, java.math.BigDecimal.ZERO));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}