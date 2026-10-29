# Java Bank Management & Online Simulation System

An advanced, thread-safe online banking simulation system built with **Java** and **Swing**. Designed with enterprise software engineering patterns, this system features robust cryptographic user authentication, concurrent transaction safety with deadlock-prevention ordering, automated statement export, and a dual-interface architecture featuring both a modern graphical user interface (GUI) and an administrative command-line interface (CLI).

---

## Architecture Overview

```
                     +---------------------------------------+
                     |         Java Online Bank System       |
                     +-------------------+-------------------+
                                         |
               +-------------------------+-------------------------+
               |                                                   |
      +--------v--------+                                 +--------v--------+
      |  Swing GUI App  |                                 |   CLI Runner    |
      |  (BankApp.java) |                                 | (BankSystem.java)|
      +--------+--------+                                 +--------+--------+
               |                                                   |
               +-------------------------+-------------------------+
                                         |
                         +---------------v---------------+
                         |        Customer / Admin       |
                         |  (Customer.java, Admin.java)  |
                         +---------------+---------------+
                                         |
                         +---------------v---------------+
                         |        AccountManager         |
                         |   (Accounts & Transactions)   |
                         +-------+---------------+-------+
                                 |               |
                   +-------------v----+     +----v-------------+
                   |   Account Model  |     |   PasswordUtil   |
                   | (Balances, Locks)|     | (PBKDF2 Hashing) |
                   +------------------+     +------------------+
```

---

## Core Features

### 1. Cryptographic Security
- **PBKDF2 with HMAC-SHA256**: Passwords are never stored in plain text. They are hashed using `PBKDF2WithHmacSHA256` across 65,536 iterations.
- **Per-User Salt**: Each account generates a unique 16-byte cryptographically secure salt via `SecureRandom`.
- **Constant-Time Verification**: Prevents timing side-channel attacks by comparing hashes using `Arrays.equals`.
- **Brute-Force Lockout**: Automatically locks an account after 3 consecutive failed login attempts, requiring administrative unlock.

### 2. Concurrency & Deadlock Prevention
- **Synchronized Mutexes**: Account balances and transaction ledgers utilize method and block synchronization for high concurrency environments.
- **Ordered Lock Acquisition**: Inter-account fund transfers sort accounts by account number lexicographically before locking (`synchronized (first) { synchronized (second) { ... } }`), mathematically preventing circular wait conditions and thread deadlocks.

### 3. Banking Operations
- **Account Opening**: Initial balance validation, account number generation via `AtomicInteger`.
- **Deposits & Withdrawals**: Thread-safe balance mutations with overdraft protection and zero/negative amount rejection.
- **Inter-Account Fund Transfers**: Immediate debit and credit with audit logs created on both source and destination accounts.
- **Transaction History**: Real-time ledger recording every action (`OPEN`, `DEPOSIT`, `WITHDRAW`, `TRANSFER_OUT`, `TRANSFER_IN`, `PW_CHANGE`) with timestamps and unique UUIDs.
- **CSV Statement Export**: One-click download of the user's complete account statement in CSV format.

### 4. Dual Interfaces
- **Swing GUI (`BankApp.java`)**: Modern responsive CardLayout UI with custom color palettes, dashboard cards, modal transaction dialogs, and interactive JTable.
- **Command Line CLI (`BankSystem.java`)**: Fast, lightweight terminal workflow for server environments.

### 5. Data Persistence
- Binary object serialization (`bank_data.ser`) saves all user accounts, password hashes, and transactions seamlessly on shutdown and restores them on launch.

---

## Project Structure

```
.
├── Account.java          # Core Account entity with thread-safe mutations
├── AccountManager.java   # Central repository, deadlock-free transfers & persistence
├── Admin.java            # Administrative console operations (unlock, delete, analytics)
├── BankApp.java          # Swing GUI application entry point & CardLayout panels
├── BankSystem.java       # Alternate Console/CLI entry point
├── Customer.java         # Customer console workflows (login, transfer, statement)
├── PasswordUtil.java     # PBKDF2 hashing, salt generator, constant-time verification
├── Transaction.java      # Immutable transaction record with CSV formatting
├── .gitignore            # Ignores compiled classes, serialized cache, and PDFs
└── README.md             # Project documentation and developer guide
```

---

## How to Run

### Requirements
- **JDK 8** or higher (Java SE 8, 11, 17, or 21)

### 1. Compile the Project
Open a terminal in the project directory and run:
```bash
javac *.java
```

### 2. Launch the Swing GUI (Recommended)
```bash
java BankApp
```

### 3. Launch the Console CLI
```bash
java BankSystem
```

### Default Credentials for Testing
- **Customer 1**: Account `1001` | Password: `alice123` (Initial balance: $5,000)
- **Customer 2**: Account `1002` | Password: `bob456` (Initial balance: $3,000)
- **Administrator**: Username `admin` | Password: `adminpass`

---

## Author
- **Shreepurnaa V** ([@shree0731](https://github.com/shree0731))
