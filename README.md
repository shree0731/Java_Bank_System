#  BankApp – Java GUI Banking System

##  Overview
**BankApp** is a simple Java Swing–based GUI application that simulates a digital banking system.  
It allows users to **create accounts, log in securely, and perform basic banking operations** such as deposits, withdrawals, and balance inquiries — all within an intuitive graphical interface.

---

##  Features
-  **User Authentication** – Secure login system for existing users.  
-  **Account Creation** – Register new users directly from the app.  
-  **Core Banking Operations** –  
  - Deposit money  
  - Withdraw funds  
  - Check account balance  
  - View transaction history  
-  **Persistent Storage** – Saves account details and transactions to a local file.  
-  **Modern Swing Interface** – User-friendly layout built using `JFrame`, `JPanel`, and `JTextField`.  

---

##  Technologies Used
- **Language:** Java (JDK 17 or higher recommended)  
- **GUI Library:** Swing / AWT  
- **File Handling:** Java I/O Streams  
- **Precision:** BigDecimal for currency accuracy  

---

##  How to Run
1. **Open the Project**
   - Launch **VS Code**, **Eclipse**, or any Java IDE.
   - Open the folder containing `BankApp.java`.

2. **Compile and Run**
   ```bash
   javac BankApp.java
   java BankApp

3. **Login or Create Account**

-Default sample users (created automatically on first run):
```bash
Username: Alice
Password: alice123

Username: Bob
Password: bob456

Admin account:

Username: admin
Password: adminpass


You can also register new accounts via the “Create Account” button in the GUI.
```

## File Structure
Bank_App/
│
├── BankApp.java          # Main Java file with all GUI and banking logic
├── Account.java          # Stores account details and transaction history
├── Customer.java         # Handles customer interactions
├── Admin.java            # Handles admin operations
├── AccountManager.java   # Manages accounts and transactions
├── Transaction.java      # Represents individual transactions
├── PasswordUtil.java     # Password hashing and verification
├── bank_data.ser         # Serialized data file (auto-generated)
├── README.md             # Project documentation
