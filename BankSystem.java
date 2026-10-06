import java.util.Scanner;

public class BankSystem {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        AccountManager manager = AccountManager.load("bank_data.ser");
        System.out.println("Starting Bank System CLI...");
    }
}
