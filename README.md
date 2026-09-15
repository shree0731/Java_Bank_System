# Online Banking Simulation System

A robust, thread-safe online banking simulation implemented in Java. The system provides both a Command-Line Interface (CLI) and an interactive Graphical User Interface (GUI) built with Java Swing.

## Key Highlights
- **Cryptographic Security**: PBKDF2 with HMAC-SHA256 password hashing and cryptographic salts.
- **Thread Safety**: Synchronized balance operations with deadlock prevention ordering.
- **Account Lockout**: Security threshold of 3 failed attempts to protect accounts against brute-force attacks.
- **Dual Interfaces**: Full-featured CLI for console users and modern Swing GUI for desktop users.
- **Persistence**: Binary object serialization for storing accounts and transactions.
