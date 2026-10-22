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
}