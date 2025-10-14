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

        JLabel subtitle = new JLabel("An online banking simulation");
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
        JButton btnLogin = new JButton("Login");
        JButton btnBack = new JButton("Back");
        btnLogin.addActionListener(e -> doAdminLogin());
        btnBack.addActionListener(e -> cards.show(mainPanel, "welcome"));
        btns.add(btnBack); btns.add(btnLogin);

        gbc.gridx=0; gbc.gridy=2; gbc.gridwidth=2; gbc.anchor=GridBagConstraints.CENTER;
        p.add(btns, gbc);

        return p;
    }

    private void doAdminLogin() {
        String user = adminUserField.getText().trim();
        char[] pwd = adminPwdField.getPassword();
        if (!"admin".equalsIgnoreCase(user)) { JOptionPane.showMessageDialog(frame, "Unknown admin username."); return; }
        Account admin = manager.getAccount("admin");
        if (admin == null) { JOptionPane.showMessageDialog(frame, "Admin not configured."); return; }
        try {
            boolean ok = PasswordUtil.verifyPassword(pwd, admin.getSalt(), admin.getPasswordHash());
            Arrays.fill(pwd, (char)0);
            if (!ok) { JOptionPane.showMessageDialog(frame, "Invalid admin password."); return; }
            // success
            cards.show(mainPanel, "admin");
            refreshAdminPanel();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Auth error: " + ex.getMessage());
        }
    }

    // admin panel UI
    private AdminPanel adminPanelInstance;
    private JPanel buildAdminPanel() {
        adminPanelInstance = new AdminPanel();
        return adminPanelInstance;
    }

    private void refreshAdminPanel() {
        if (adminPanelInstance != null) adminPanelInstance.refresh();
    }

    private class AdminPanel extends JPanel {
        private JTable accountsTable;
        private AccountsTableModel atm;
        private JTextArea detailsArea;

        public AdminPanel() {
            setLayout(new BorderLayout(10,10));
            setBorder(new EmptyBorder(10,10,10,10));
            JLabel title = new JLabel("Admin Dashboard");
            title.setFont(new Font("SansSerif", Font.BOLD, 20));
            add(title, BorderLayout.NORTH);

            atm = new AccountsTableModel(new ArrayList<>());
            accountsTable = new JTable(atm);
            accountsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane sp = new JScrollPane(accountsTable);
            sp.setPreferredSize(new Dimension(400,350));
            add(sp, BorderLayout.WEST);

            detailsArea = new JTextArea();
            detailsArea.setEditable(false);
            JScrollPane dsp = new JScrollPane(detailsArea);
            dsp.setPreferredSize(new Dimension(420,350));
            add(dsp, BorderLayout.CENTER);

            JPanel right = new JPanel(new GridLayout(6,1,6,6));
            JButton btnCreate = new JButton("Create Account (Admin)");
            JButton btnDelete = new JButton("Delete Account");
            JButton btnUnlock = new JButton("Unlock Account");
            JButton btnView = new JButton("View Statement");
            JButton btnSummary = new JButton("Admin Summary");
            JButton btnLogout = new JButton("Logout");

            btnCreate.addActionListener(e -> createAccountAdmin());
            btnDelete.addActionListener(e -> deleteSelected());
            btnUnlock.addActionListener(e -> unlockSelected());
            btnView.addActionListener(e -> viewSelectedStatement());
            btnSummary.addActionListener(e -> showSummary());
            btnLogout.addActionListener(e -> {
                manager.save(DATA_FILE);
                cards.show(mainPanel, "welcome");
            });

            JPanel rwrap = new JPanel(new BorderLayout());
            rwrap.add(right, BorderLayout.NORTH);
            right.add(btnCreate); right.add(btnDelete); right.add(btnUnlock);
            right.add(btnView); right.add(btnSummary); right.add(btnLogout);
            add(rwrap, BorderLayout.EAST);

            accountsTable.getSelectionModel().addListSelectionListener(e -> {
                int r = accountsTable.getSelectedRow();
                if (r >= 0) {
                    String acc = atm.getAccountNumberAt(r);
                    Account a = manager.getAccount(acc);
                    showAccountDetails(a);
                }
            });

            refresh();
        }

        public void refresh() {
            List<Account> list = manager.getAllAccounts().values().stream()
                    .filter(a -> !"admin".equals(a.getAccountNumber()))
                    .sorted(Comparator.comparing(Account::getAccountNumber))
                    .collect(Collectors.toList());
            atm.setAccounts(list);
            detailsArea.setText("");
        }

        private void showAccountDetails(Account a) {
            if (a == null) { detailsArea.setText(""); return; }
            StringBuilder sb = new StringBuilder();
            sb.append("Account: ").append(a.getAccountNumber()).append("\n");
            sb.append("Holder: ").append(a.getHolderName()).append("\n");
            sb.append("Balance: ").append(a.getBalance().toPlainString()).append("\n");
            sb.append("Locked: ").append(a.isLocked()).append("\n");
            sb.append("Failed logins: ").append(a.getFailedLogins()).append("\n\n");
            sb.append("Last 10 transactions:\n");
            List<Transaction> h = a.getHistory();
            int start = Math.max(0, h.size() - 10);
            for (int i = h.size()-1; i >= start; i--) sb.append(h.get(i)).append("\n");
            detailsArea.setText(sb.toString());
        }

        private void createAccountAdmin() {
            JTextField name = new JTextField();
            JPasswordField pwd = new JPasswordField();
            JPasswordField cp = new JPasswordField();
            JTextField init = new JTextField("0");
            Object[] msg = {
                    "Holder name:", name,
                    "Password (min 8 chars incl digit & letter):", pwd,
                    "Confirm password:", cp,
                    "Initial deposit (supports k/M):", init
            };
            int res = JOptionPane.showConfirmDialog(frame, msg, "Create Account (Admin)", JOptionPane.OK_CANCEL_OPTION);
            if (res != JOptionPane.OK_OPTION) return;
            String nm = name.getText().trim();
            String p = new String(pwd.getPassword());
            String c = new String(cp.getPassword());
            if (nm.isEmpty()) { JOptionPane.showMessageDialog(frame, "Name cannot be empty."); return; }
            if (!p.equals(c)) { JOptionPane.showMessageDialog(frame, "Passwords do not match."); return; }
            if (!isPasswordStrong(p)) { JOptionPane.showMessageDialog(frame, "Password too weak."); return; }
            try {
                BigDecimal initial = parseAmount(init.getText().trim());
                String acc = manager.createAccount(nm, p, initial);
                manager.save(DATA_FILE);
                JOptionPane.showMessageDialog(frame, "Account created: " + acc);
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Create failed: " + ex.getMessage());
            }
        }

        private void deleteSelected() {
            int r = accountsTable.getSelectedRow();
            if (r < 0) { JOptionPane.showMessageDialog(frame, "Select account first."); return; }
            String acc = atm.getAccountNumberAt(r);
            int c = JOptionPane.showConfirmDialog(frame, "Delete account " + acc + " ? This cannot be undone.", "Confirm", JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) return;
            boolean ok = manager.deleteAccount(acc);
            if (ok) { manager.save(DATA_FILE); JOptionPane.showMessageDialog(frame, "Deleted."); refresh(); }
            else JOptionPane.showMessageDialog(frame, "Delete failed.");
        }

        private void unlockSelected() {
            int r = accountsTable.getSelectedRow();
            if (r < 0) { JOptionPane.showMessageDialog(frame, "Select account first."); return; }
            String acc = atm.getAccountNumberAt(r);
            Account a = manager.getAccount(acc);
            if (a == null) { JOptionPane.showMessageDialog(frame, "Account not found."); return; }
            a.unlock();
            manager.save(DATA_FILE);
            JOptionPane.showMessageDialog(frame, "Unlocked.");
            refresh();
        }

        private void viewSelectedStatement() {
            int r = accountsTable.getSelectedRow();
            if (r < 0) { JOptionPane.showMessageDialog(frame, "Select account first."); return; }
            String acc = atm.getAccountNumberAt(r);
            Account a = manager.getAccount(acc);
            if (a == null) { JOptionPane.showMessageDialog(frame, "Account not found."); return; }
            JTextArea ta = new JTextArea();
            StringBuilder sb = new StringBuilder();
            for (Transaction t : a.getHistory()) sb.append(t.toString()).append("\n");
            ta.setText(sb.toString());
            ta.setEditable(false);
            ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            JScrollPane sp = new JScrollPane(ta);
            sp.setPreferredSize(new Dimension(800,400));
            JOptionPane.showMessageDialog(frame, sp, "Statement for " + acc, JOptionPane.INFORMATION_MESSAGE);
        }

        private void showSummary() {
            int total = manager.totalAccounts();
            BigDecimal funds = manager.totalFunds();
            String msg = "Total customer accounts: " + total + "\nTotal funds: " + funds.toPlainString() + "\nNext account number: " + manager.nextAccountPeek();
            JOptionPane.showMessageDialog(frame, msg, "Admin Summary", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /* -------------------------
       Table Models & Helpers
       ------------------------- */
    private static class AccountsTableModel extends AbstractTableModel {
        private final String[] cols = {"Account#", "Holder", "Balance", "Locked"};
        private List<Account> accounts = new ArrayList<>();
        public AccountsTableModel(List<Account> accounts) { setAccounts(accounts); }
        public void setAccounts(List<Account> accounts) { this.accounts = new ArrayList<>(accounts); fireTableDataChanged(); }
        public String getAccountNumberAt(int row) { return accounts.get(row).getAccountNumber(); }
        public int getRowCount() { return accounts.size(); }
        public int getColumnCount() { return cols.length; }
        public String getColumnName(int c) { return cols[c]; }
        public Object getValueAt(int r, int c) {
            Account a = accounts.get(r);
            switch (c) {
                case 0: return a.getAccountNumber();
                case 1: return a.getHolderName();
                case 2: return a.getBalance().toPlainString();
                case 3: return a.isLocked();
                default: return "";
            }
        }
    }

    private static class HistoryTableModel extends AbstractTableModel {
        private final String[] cols = {"Timestamp", "Type", "Amount", "Note"};
        private List<Transaction> history = new ArrayList<>();
        public HistoryTableModel(List<Transaction> history) { setHistory(history); }
        public void setHistory(List<Transaction> history) {
            this.history = new ArrayList<>(history);
            Collections.reverse(this.history); // show latest first
            fireTableDataChanged();
        }
        public int getRowCount() { return history.size(); }
        public int getColumnCount() { return cols.length; }
        public String getColumnName(int c) { return cols[c]; }
        public Object getValueAt(int r, int c) {
            Transaction t = history.get(r);
            switch (c) {
                case 0: return t.getTimestamp();
                case 1: return t.getType();
                case 2: return t.getAmount() == null ? "" : t.getAmount().toPlainString();
                case 3: return t.getNote();
                default: return "";
            }
        }
    }

    /* -------------------------
       Utility & Parsers
       ------------------------- */
    // parse amounts like "20k", "1.5M", etc.
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

    private static boolean isPasswordStrong(String pwd) {
        if (pwd == null) return false;
        if (pwd.length() < 8) return false;
        boolean hasDigit = false, hasLetter = false;
        for (char c : pwd.toCharArray()) {
            if (Character.isDigit(c)) hasDigit = true;
            if (Character.isLetter(c)) hasLetter = true;
        }
        return hasDigit && hasLetter;
    }

    private void ensureSampleAccounts(AccountManager manager) {
        if (manager.getAllAccounts().isEmpty()) {
            try {
                manager.createAccount("Alice", "alice123", new BigDecimal("5000"));
                manager.createAccount("Bob", "bob456", new BigDecimal("3000"));
                byte[] salt = PasswordUtil.generateSalt();
                byte[] hash = PasswordUtil.hashPassword("adminpass".toCharArray(), salt);
                manager.addAccount(new Account("admin", "Administrator", hash, salt, BigDecimal.ZERO));
                manager.save(DATA_FILE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* -------------------------
       MAIN
       ------------------------- */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BankApp());
    }

    /* =========================
       DOMAIN + UTIL CLASSES
       (Account, Manager, Transaction, PasswordUtil)
       All kept in same file for single-file convenience.
       ========================= */

    public static class Account implements Serializable {
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
            this.history.add(new Transaction("OPEN", balance, "Account opened"));
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

    public static class AccountManager implements Serializable {
        private static final long serialVersionUID = 1L;
        private Map<String, Account> accounts = new HashMap<>();
        public AtomicInteger nextAccountNumber = new AtomicInteger(1003);

        public synchronized Account getAccount(String accNum) {
            return accounts.get(accNum);
        }

        public synchronized Map<String, Account> getAllAccounts() {
            return new HashMap<>(accounts);
        }

        public synchronized String createAccount(String name, String password, BigDecimal balance) throws Exception {
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

    public static class Transaction implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String id;
        private final String timestamp;
        private final String type; // OPEN, DEPOSIT, WITHDRAW, TRANSFER_OUT, TRANSFER_IN, PW_CHANGE
        private final BigDecimal amount;
        private final String note;

        public Transaction(String type, BigDecimal amount, String note) {
            this.id = UUID.randomUUID().toString();
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            this.type = type;
            this.amount = amount;
            this.note = note;
        }

        public String getId() { return id; }
        public String getTimestamp() { return timestamp; }
        public String getType() { return type; }
        public BigDecimal getAmount() { return amount; }
        public String getNote() { return note; }

        @Override
        public String toString() {
            return String.format("%s | %s | %s | %s | %s",
                    id, timestamp, type, (amount == null ? "-" : amount.toPlainString()), note);
        }

        public String toCSV() {
            String amt = (amount == null ? "" : amount.toPlainString());
            return String.join(",", id, timestamp, type, amt, "\"" + note.replace("\"", "\"\"") + "\"");
        }
    }

    public static class PasswordUtil {
        private static final int ITERATIONS = 65536;
        private static final int KEY_LENGTH = 256;

        public static byte[] generateSalt() {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            return salt;
        }

        public static byte[] hashPassword(final char[] password, final byte[] salt)
                throws Exception {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        }

        public static byte[] hashPassword(final char[] password, final byte[] salt, boolean dummy) throws Exception {
            // overloaded helper that accepts same signature style as your original code
            return hashPassword(password, salt);
        }

        public static boolean verifyPassword(char[] attemptedPassword, byte[] salt, byte[] expectedHash)
                throws Exception {
            byte[] attemptedHash = hashPassword(attemptedPassword, salt);
            return Arrays.equals(attemptedHash, expectedHash);
        }

        // convenience wrapper for earlier call styles
        public static byte[] hashPassword(char[] password, byte[] salt, int i) throws Exception {
            return hashPassword(password, salt);
        }
    }
}
