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

    public Account(String accountNumber, String holderName, byte[] passwordHash, byte[] salt, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.balance = balance;
    }

    public String getAccountNumber() { return accountNumber; }
    public String getHolderName() { return holderName; }
    public BigDecimal getBalance() { return balance; }
    public byte[] getSalt() { return salt; }
    public byte[] getPasswordHash() { return passwordHash; }

    public boolean deposit(BigDecimal amt) {
        if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) return false;
        balance = balance.add(amt);
        return true;
    }

    public boolean withdraw(BigDecimal amt) {
        if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) return false;
        if (balance.compareTo(amt) < 0) return false;
        balance = balance.subtract(amt);
        return true;
    }

    public boolean transferOut(BigDecimal amt, String toAcc) {
        if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) return false;
        if (balance.compareTo(amt) < 0) return false;
        balance = balance.subtract(amt);
        return true;
    }

    public void addTransferIn(BigDecimal amt, String fromAcc) {
        balance = balance.add(amt);
    }
}
