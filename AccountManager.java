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

    public synchronized String nextAccountPeek() {
        return String.valueOf(nextAccountNumber.get());
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
}
