// BankApp.java
// Single-file Swing-based online banking simulation.
// Paste into BankApp.java, compile and run: javac BankApp.java && java BankApp

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.*;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.UUID;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/* ---------------------------
   MAIN APPLICATION (Swing)
   --------------------------- */
public class BankApp {
    private static final String DATA_FILE = "bank_data.ser";

    private JFrame frame;
    private CardLayout cards;
    private JPanel mainPanel;

    private AccountManager manager;

    // Login/create fields
    private JTextField loginAccField;
    private JPasswordField loginPwdField;

    // Create account fields
    private JTextField createNameField;
    private JPasswordField createPwdField;
    private JPasswordField createPwdConfirmField;
    private JTextField createInitialField;

    // Admin login
    private JTextField adminUserField;
    private JPasswordField adminPwdField;

    // Current logged account
    private Account currentAccount;

    // Constructor
    public BankApp() {
        manager = AccountManager.load(DATA_FILE);
        ensureSampleAccounts(manager);
        initUI();
    }

    private void initUI() {
        frame = new JFrame("JBank — Swing Online Banking Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);

        cards = new CardLayout();
        mainPanel = new JPanel(cards);

        mainPanel.add(buildWelcomePanel(), "welcome");
        mainPanel.add(buildCustomerPanel(), "customer");
        mainPanel.add(buildCreatePanel(), "create");
        mainPanel.add(buildAdminLoginPanel(), "admin_login");
        mainPanel.add(buildAdminPanel(), "admin");

        frame.setContentPane(mainPanel);
        frame.setVisible(true);
    }

    private JPanel buildWelcomePanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel title = new JLabel("JBank");
        title.setFont(new Font("SansSerif", Font.BOLD, 36));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subtitle = new JLabel("An online  banking simulation");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 16));
        subtitle.setHorizontalAlignment(SwingConstants.CENTER);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.insets = new Insets(10,10,25,10);
        p.add(title, gbc);
        gbc.gridy = 1; gbc.insets = new Insets(0,10,20,10);
        p.add(subtitle, gbc);

        JButton btnLogin = new JButton("Customer Login");
        JButton btnCreate = new JButton("Create Account");
        JButton btnAdmin = new JButton("Admin Login");
        JButton btnExit = new JButton("Exit");

        btnLogin.addActionListener(e -> showCustomerLoginDialog());
        btnCreate.addActionListener(e -> cards.show(mainPanel, "create"));
        btnAdmin.addActionListener(e -> cards.show(mainPanel, "admin_login"));
        btnExit.addActionListener(e -> {
            manager.save(DATA_FILE);
            System.exit(0);
        });

        JPanel btns = new JPanel(new GridLayout(2,2,10,10));
        btns.add(btnLogin); btns.add(btnCreate); btns.add(btnAdmin); btns.add(btnExit);

        gbc.gridy = 2; gbc.gridwidth = 2; gbc.insets = new Insets(10,10,10,10); gbc.fill = GridBagConstraints.HORIZONTAL;
        p.add(btns, gbc);

        return p;
    }

    /* ------------------------------
       CUSTOMER: Login & Dashboard
       ------------------------------ */
    private void showCustomerLoginDialog() {
        JDialog d = new JDialog(frame, "Customer Login", true);
        d.setSize(360,220);
        d.setLocationRelativeTo(frame);
        d.setLayout(new BorderLayout());
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(10,10,10,10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        form.add(new JLabel("Account Number:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        loginAccField = new JTextField(12);
        form.add(loginAccField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST;
        form.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        loginPwdField = new JPasswordField(12);
        form.add(loginPwdField, gbc);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("Login");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> {
            String acc = loginAccField.getText().trim();
            char[] pwd = loginPwdField.getPassword();
            if (acc.isEmpty()) { JOptionPane.showMessageDialog(d, "Enter account number."); return; }
            Account a = manager.getAccount(acc);
            if (a == null) { JOptionPane.showMessageDialog(d, "Account not found."); return; }
            if (a.isLocked()) { JOptionPane.showMessageDialog(d, "Account is locked due to failed logins."); return; }
            try {
                boolean okv = PasswordUtil.verifyPassword(pwd, a.getSalt(), a.getPasswordHash());
                Arrays.fill(pwd, (char)0);
                if (!okv) {
                    a.incrementFailedLogins();
                    manager.save(DATA_FILE);
                    int rem = Math.max(0, 3 - a.getFailedLogins());
                    JOptionPane.showMessageDialog(d, "Invalid password. Remaining attempts: " + rem);
                    return;
                } else {
                    a.resetFailedLogins();
                    manager.save(DATA_FILE);
                    currentAccount = a;
                    d.dispose();
                    showCustomerDashboard();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(d, "Authentication error: " + ex.getMessage());
            }
        });
        cancel.addActionListener(e -> d.dispose());
        btns.add(ok); btns.add(cancel);

        d.add(form, BorderLayout.CENTER);
        d.add(btns, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private void showCustomerDashboard() {
        cards.show(mainPanel, "customer");
        // build dynamic panel with account info
        JPanel p = (JPanel) mainPanel.getComponent(1); // customer card
        CustomerPanel cp = (CustomerPanel) Arrays.stream(p.getComponents())
                .filter(c -> c instanceof CustomerPanel).findFirst().orElse(null);
        if (cp != null) cp.setAccount(currentAccount);
    }

    private JPanel buildCustomerPanel() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBorder(new EmptyBorder(10,10,10,10));

        CustomerPanel cp = new CustomerPanel();
        wrap.add(cp, BorderLayout.CENTER);

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> {
            currentAccount = null;
            manager.save(DATA_FILE);
            cards.show(mainPanel, "welcome");
        });
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(btnLogout);
        wrap.add(south, BorderLayout.SOUTH);

        return wrap;
    }

    // inner CustomerPanel class to show account actions
    private class CustomerPanel extends JPanel {
        private JLabel lblAcc, lblName, lblBalance;
        private JTable historyTable;
        private HistoryTableModel htm;

        public CustomerPanel() {
            setLayout(new BorderLayout(10,10));
            setBorder(new EmptyBorder(10,10,10,10));

            JPanel top = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6,6,6,6);

            lblAcc = new JLabel("Account: -");
            lblAcc.setFont(new Font("SansSerif", Font.BOLD, 16));
            lblName = new JLabel("Name: -");
            lblBalance = new JLabel("Balance: -");
            lblBalance.setFont(new Font("Monospaced", Font.BOLD, 14));

            gbc.gridx=0; gbc.gridy=0; gbc.anchor=GridBagConstraints.WEST;
            top.add(lblAcc, gbc);
            gbc.gridx=1; top.add(lblName, gbc);
            gbc.gridx=2; top.add(lblBalance, gbc);

            JButton btnDeposit = new JButton("Deposit");
            JButton btnWithdraw = new JButton("Withdraw");
            JButton btnTransfer = new JButton("Transfer");
            JButton btnMini = new JButton("Mini-Statement");
            JButton btnExport = new JButton("Export CSV");
            JButton btnChangePwd = new JButton("Change Password");

            btnDeposit.addActionListener(e -> doDeposit());
            btnWithdraw.addActionListener(e -> doWithdraw());
            btnTransfer.addActionListener(e -> doTransfer());
            btnMini.addActionListener(e -> showMiniStatement());
            btnExport.addActionListener(e -> exportCSV());
            btnChangePwd.addActionListener(e -> changePassword());

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT,8,8));
            actions.add(btnDeposit); actions.add(btnWithdraw); actions.add(btnTransfer);
            actions.add(btnMini); actions.add(btnExport); actions.add(btnChangePwd);

            add(top, BorderLayout.NORTH);
            add(actions, BorderLayout.CENTER);

            htm = new HistoryTableModel(new ArrayList<>());
            historyTable = new JTable(htm);
            JScrollPane sp = new JScrollPane(historyTable);
            sp.setPreferredSize(new Dimension(850, 280));
            add(sp, BorderLayout.SOUTH);
        }

        public void setAccount(Account a) {
            this.lblAcc.setText("Account: " + a.getAccountNumber());
            this.lblName.setText("Name: " + a.getHolderName());
            this.lblBalance.setText("Balance: " + a.getBalance().toPlainString());
            htm.setHistory(a.getHistory());
        }

        private void refresh() {
            if (currentAccount != null) {
                lblBalance.setText("Balance: " + currentAccount.getBalance().toPlainString());
                htm.setHistory(currentAccount.getHistory());
            }
        }

        private void doDeposit() {
            String s = JOptionPane.showInputDialog(frame, "Enter deposit amount (supports k/M):", "Deposit", JOptionPane.PLAIN_MESSAGE);
            if (s == null) return;
            try {
                BigDecimal amt = parseAmount(s);
                if (amt.compareTo(BigDecimal.ZERO) <= 0) { JOptionPane.showMessageDialog(frame, "Amount must be positive."); return; }
                boolean ok = currentAccount.deposit(amt);
                if (ok) {
                    manager.save(DATA_FILE);
                    JOptionPane.showMessageDialog(frame, "Deposit successful.");
                    refresh();
                } else JOptionPane.showMessageDialog(frame, "Deposit failed.");
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(frame, "Invalid amount."); }
        }

        private void doWithdraw() {
            String s = JOptionPane.showInputDialog(frame, "Enter withdrawal amount (supports k/M):", "Withdraw", JOptionPane.PLAIN_MESSAGE);
            if (s == null) return;
            try {
                BigDecimal amt = parseAmount(s);
                if (amt.compareTo(BigDecimal.ZERO) <= 0) { JOptionPane.showMessageDialog(frame, "Amount must be positive."); return; }
                boolean ok = currentAccount.withdraw(amt);
                if (ok) {
                    manager.save(DATA_FILE);
                    JOptionPane.showMessageDialog(frame, "Withdraw successful.");
                    refresh();
                } else JOptionPane.showMessageDialog(frame, "Insufficient funds or invalid amount.");
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(frame, "Invalid amount."); }
        }

        private void doTransfer() {
            String target = JOptionPane.showInputDialog(frame, "Enter target account number:", "Transfer", JOptionPane.PLAIN_MESSAGE);
            if (target == null) return;
            if (manager.getAccount(target) == null) { JOptionPane.showMessageDialog(frame, "Target not found."); return; }
            String s = JOptionPane.showInputDialog(frame, "Enter amount to transfer (supports k/M):", "Transfer", JOptionPane.PLAIN_MESSAGE);
            if (s == null) return;
            try {
                BigDecimal amt = parseAmount(s);
                if (amt.compareTo(BigDecimal.ZERO) <= 0) { JOptionPane.showMessageDialog(frame, "Amount must be positive."); return; }
                int conf = JOptionPane.showConfirmDialog(frame, "Confirm transfer of " + amt.toPlainString() + " to " + target + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (conf != JOptionPane.YES_OPTION) return;
                boolean ok = manager.transfer(currentAccount.getAccountNumber(), target, amt);
                if (ok) {
                    manager.save(DATA_FILE);
                    JOptionPane.showMessageDialog(frame, "Transfer successful.");
                    refresh();
                } else JOptionPane.showMessageDialog(frame, "Transfer failed (insufficient funds or invalid).");
            } catch (NumberFormatException ex) { JOptionPane.showMessageDialog(frame, "Invalid amount."); }
        }

        private void showMiniStatement() {
            List<Transaction> hist = currentAccount.getHistory();
            StringBuilder sb = new StringBuilder();
            int start = Math.max(0, hist.size() - 10);
            for (int i = hist.size()-1; i >= start; i--) {
                sb.append(hist.get(i).toString()).append("\n");
            }
            JTextArea ta = new JTextArea(sb.toString());
            ta.setEditable(false);
            ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            JScrollPane sp = new JScrollPane(ta);
            sp.setPreferredSize(new Dimension(800, 300));
            JOptionPane.showMessageDialog(frame, sp, "Mini-Statement", JOptionPane.INFORMATION_MESSAGE);
        }

        private void exportCSV() {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Export Statement CSV");
            fc.setSelectedFile(new File("statement_" + currentAccount.getAccountNumber() + "_" +
                    new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".csv"));
            int r = fc.showSaveDialog(frame);
            if (r != JFileChooser.APPROVE_OPTION) return;
            File f = fc.getSelectedFile();
            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                pw.println("TransactionID,Timestamp,Type,Amount,Note");
                for (Transaction t : currentAccount.getHistory()) pw.println(t.toCSV());
                JOptionPane.showMessageDialog(frame, "CSV exported to: " + f.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Failed to export CSV: " + ex.getMessage());
            }
        }

        private void changePassword() {
            JPasswordField cur = new JPasswordField();
            JPasswordField np = new JPasswordField();
            JPasswordField cp = new JPasswordField();
            Object[] msg = {
                    "Current password:", cur,
                    "New password (min 8 chars incl digit & letter):", np,
                    "Confirm new password:", cp
            };
            int ok = JOptionPane.showConfirmDialog(frame, msg, "Change Password", JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION) return;
            char[] curc = cur.getPassword();
            try {
                if (!PasswordUtil.verifyPassword(curc, currentAccount.getSalt(), currentAccount.getPasswordHash())) {
                    JOptionPane.showMessageDialog(frame, "Current password incorrect.");
                    return;
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(frame, "Auth error."); return; }
            char[] npc = np.getPassword(); char[] cpc = cp.getPassword();
            String nps = new String(npc);
            if (!nps.equals(new String(cpc))) { JOptionPane.showMessageDialog(frame, "Passwords do not match."); return; }
            if (!isPasswordStrong(nps)) { JOptionPane.showMessageDialog(frame, "Password too weak."); return; }
            try {
                byte[] newSalt = PasswordUtil.generateSalt();
                byte[] newHash = PasswordUtil.hashPassword(npc, newSalt);
                currentAccount.setPasswordHashAndSalt(newHash, newSalt);
                manager.save(DATA_FILE);
                JOptionPane.showMessageDialog(frame, "Password changed successfully.");
            } catch (Exception ex) { JOptionPane.showMessageDialog(frame, "Failed to set password: " + ex.getMessage()); }
        }
    }

    /* -------------------------
       CREATE ACCOUNT Panel
       ------------------------- */
    private JPanel buildCreatePanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(20,20,20,20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,8,8,8);
        gbc.gridx=0; gbc.gridy=0; gbc.anchor = GridBagConstraints.EAST;
        p.add(new JLabel("Holder Name:"), gbc);
        gbc.gridx=1; gbc.anchor = GridBagConstraints.WEST;
        createNameField = new JTextField(18);
        p.add(createNameField, gbc);

        gbc.gridx=0; gbc.gridy=1; gbc.anchor=GridBagConstraints.EAST;
        p.add(new JLabel("Password:"), gbc);
        gbc.gridx=1; gbc.anchor=GridBagConstraints.WEST;
        createPwdField = new JPasswordField(18);
        p.add(createPwdField, gbc);

        gbc.gridx=0; gbc.gridy=2; gbc.anchor=GridBagConstraints.EAST;
        p.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx=1; gbc.anchor=GridBagConstraints.WEST;
        createPwdConfirmField = new JPasswordField(18);
        p.add(createPwdConfirmField, gbc);

        gbc.gridx=0; gbc.gridy=3; gbc.anchor=GridBagConstraints.EAST;
        p.add(new JLabel("Initial Deposit (supports k/M):"), gbc);
        gbc.gridx=1; gbc.anchor=GridBagConstraints.WEST;
        createInitialField = new JTextField(12);
        p.add(createInitialField, gbc);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCreate = new JButton("Create");
        JButton btnBack = new JButton("Back");
        btnCreate.addActionListener(e -> doCreateAccount());
        btnBack.addActionListener(e -> cards.show(mainPanel, "welcome"));
        btns.add(btnBack); btns.add(btnCreate);

        gbc.gridx=0; gbc.gridy=4; gbc.gridwidth=2; gbc.anchor=GridBagConstraints.CENTER;
        p.add(btns, gbc);

        return p;
    }

    private void doCreateAccount() {
        String name = createNameField.getText().trim();
        String pwd = new String(createPwdField.getPassword());
        String cp = new String(createPwdConfirmField.getPassword());
        String init = createInitialField.getText().trim();
        if (name.isEmpty()) { JOptionPane.showMessageDialog(frame, "Name cannot be empty."); return; }
        if (!pwd.equals(cp)) { JOptionPane.showMessageDialog(frame, "Passwords do not match."); return; }
        if (!isPasswordStrong(pwd)) { JOptionPane.showMessageDialog(frame, "Password too weak. Min 8 chars incl letter & digit."); return; }
        try {
            BigDecimal initial = parseAmount(init);
            if (initial.compareTo(BigDecimal.ZERO) < 0) { JOptionPane.showMessageDialog(frame, "Initial cannot be negative."); return; }
            String accNum = manager.createAccount(name, pwd, initial);
            manager.save(DATA_FILE);
            JOptionPane.showMessageDialog(frame, "Account created. Account Number: " + accNum);
            // clear fields
            createNameField.setText(""); createInitialField.setText("");
            createPwdField.setText(""); createPwdConfirmField.setText("");
            cards.show(mainPanel, "welcome");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Invalid amount. Use numeric or suffix k/M.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Account creation failed: " + ex.getMessage());
        }
    }

    /* -------------------------
       ADMIN Login & Dashboard
       ------------------------- */
    private JPanel buildAdminLoginPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(20,20,20,20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.gridx=0; gbc.gridy=0; gbc.anchor=GridBagConstraints.EAST;
        p.add(new JLabel("Admin username:"), gbc);
        gbc.gridx=1; gbc.anchor=GridBagConstraints.WEST;
        adminUserField = new JTextField(12);
        p.add(adminUserField, gbc);

        gbc.gridx=0; gbc.gridy=1; gbc.anchor=GridBagConstraints.EAST;
        p.add(new JLabel("Password:"), gbc);
        gbc.gridx=1; gbc.anchor=GridBagConstraints.WEST;
        adminPwdField = new JPasswordField(12);
        p.add(adminPwdField, gbc);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
}