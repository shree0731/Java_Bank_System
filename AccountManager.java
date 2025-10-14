
import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AccountManager implements Serializable {
    private static final long serialVersionUID = 1L;
    private Map<String, Account> accounts = new HashMap<>();
    public AtomicInteger nextAccountNumber = new AtomicInteger(1003);

    public synchronized Account getAccount(String accNum) {
        return accounts.get(accNum);
    }

    public synchronized Map<String, Account> getAllAccounts() {
        return new HashMap<>(accounts);
    }

    public synchronized String createAccount(String name, String password, BigDecimal balance)
            throws Exception {
        String accNum = String.valueOf(nextAccountNumber.getAndIncrement());
        byte[] salt = PasswordUtil.generateSalt();
        byte[] hash = PasswordUtil.hashPassword(password.toCharArray(), salt);
        Account acc = new Account(accNum, name, hash, salt, balance);
        accounts.put(accNum, acc);
        return accNum;
    }

    public synchronized boolean deleteAccount(String accNum) {
        return accounts.remove(accNum) != null;
    }

    public synchronized void addAccount(Account acc) {
        accounts.put(acc.getAccountNumber(), acc);
    }

    public boolean transfer(String fromAccNum, String toAccNum, BigDecimal amount) {
        Account a1 = getAccount(fromAccNum);
        Account a2 = getAccount(toAccNum);
        if (a1 == null || a2 == null) return false;

        Account first = (a1.getAccountNumber().compareTo(a2.getAccountNumber()) <= 0) ? a1 : a2;
        Account second = (first == a1) ? a2 : a1;

        synchronized (first) {
            synchronized (second) {
                if (!a1.transferOut(amount, toAccNum)) return false;
                a2.addTransferIn(amount, fromAccNum);
                return true;
            }
        }
    }

    public synchronized int totalAccounts() {
        int count = accounts.size();
        if (accounts.containsKey("admin")) count--;
        return count;
    }

    public synchronized BigDecimal totalFunds() {
        BigDecimal sum = BigDecimal.ZERO;
        for (Account a : accounts.values()) {
            if ("admin".equals(a.getAccountNumber())) continue;
            sum = sum.add(a.getBalance());
        }
        return sum;
    }

    public synchronized String nextAccountPeek() {
        return String.valueOf(nextAccountNumber.get());
    }

    public synchronized void save(String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(this);
        } catch (IOException e) {
            System.err.println("Could not save data: " + e.getMessage());
        }
    }

    public static AccountManager load(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            return (AccountManager) ois.readObject();
        } catch (Exception e) {
            return new AccountManager();
        }
    }
}