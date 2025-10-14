
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Account implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String accountNumber;
    private final String holderName;
    private byte[] passwordHash;
    private byte[] salt;
    private BigDecimal balance;
    private final List<Transaction> history = new ArrayList<>();
    private int failedLogins = 0;
    private boolean locked = false;

    public Account(String accountNumber, String holderName, byte[] passwordHash, byte[] salt, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.balance = balance;
        history.add(new Transaction("OPEN", balance, "Account opened"));
    }

    public String getAccountNumber() { return accountNumber; }
    public String getHolderName() { return holderName; }
    public synchronized BigDecimal getBalance() { return balance; }
    public synchronized byte[] getSalt() { return salt; }
    public synchronized byte[] getPasswordHash() { return passwordHash; }

    public synchronized boolean deposit(BigDecimal amt) {
        if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) return false;
        balance = balance.add(amt);
        history.add(new Transaction("DEPOSIT", amt, "Deposit"));
        return true;
    }

    public synchronized boolean withdraw(BigDecimal amt) {
        if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) return false;
        if (balance.compareTo(amt) < 0) return false;
        balance = balance.subtract(amt);
        history.add(new Transaction("WITHDRAW", amt, "Withdraw"));
        return true;
    }

    public synchronized boolean transferOut(BigDecimal amt, String toAcc) {
        if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) return false;
        if (balance.compareTo(amt) < 0) return false;
        balance = balance.subtract(amt);
        history.add(new Transaction("TRANSFER_OUT", amt, "To: " + toAcc));
        return true;
    }

    public synchronized void addTransferIn(BigDecimal amt, String fromAcc) {
        balance = balance.add(amt);
        history.add(new Transaction("TRANSFER_IN", amt, "From: " + fromAcc));
    }

    public synchronized List<Transaction> getHistory() {
        return new ArrayList<>(history);
    }

    public synchronized int getFailedLogins() { return failedLogins; }
    public synchronized void resetFailedLogins() { failedLogins = 0; }
    public synchronized void incrementFailedLogins() {
        failedLogins++;
        if (failedLogins >= 3) locked = true;
    }
    public synchronized boolean isLocked() { return locked; }
    public synchronized void unlock() { locked = false; failedLogins = 0; }

    public synchronized void setPasswordHashAndSalt(byte[] hash, byte[] salt) {
        this.passwordHash = hash;
        this.salt = salt;
        history.add(new Transaction("PW_CHANGE", null, "Password changed"));
    }
}