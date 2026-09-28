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

    public synchronized String nextAccountPeek() {
        return String.valueOf(nextAccountNumber.get());
    }
}
