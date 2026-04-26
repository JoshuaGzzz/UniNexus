# UniNexus: The Spartan Student Marketplace

**UniNexus** is a JavaFX-based desktop application designed as a dedicated marketplace for students. The system facilitates buying, selling, and administrative moderation through a structured Model-View-Controller (MVC) architecture and a robust Data Access Object (DAO) layer.

---

## 🛠 Tech Stack

* [cite_start]**Language:** Java (Main Backend) [cite: 13]
* [cite_start]**UI Framework:** JavaFX [cite: 14]
* [cite_start]**Database:** SQLite (Persistent data for users and products) [cite: 15]
* [cite_start]**Local Session:** JSON (Jackson-databind for cart serialization) [cite: 16, 150]
* [cite_start]**Environment:** Maven (Dependency management) [cite: 17]

---

## 🏗 System Architecture

[cite_start]The project follows a strict **MVC-DAO** pattern to separate concerns and ensure the UI remains responsive during database operations [cite: 20-27, 32]:

* [cite_start]**View:** Pure JavaFX FXML files for layout[cite: 21].
* [cite_start]**Controller:** Acts as the middleman, handling user input and communicating with models[cite: 25].
* [cite_start]**Model:** Handles data values and state management[cite: 23].
* [cite_start]**DAO (Data Access Object):** A dedicated layer for all SQLite interactions[cite: 26].
* [cite_start]**Singleton Pattern:** Ensures a single database connection is maintained across all screens to prevent database locking[cite: 28, 29].

---

## 🚀 Features & User Flow

### 1. Identity & Data Gateway
* [cite_start]**Session Check:** Upon launch, the app checks for a local JSON session[cite: 61].
* [cite_start]**Authentication:** Secure Login and Registration screens[cite: 4, 5, 63].
* [cite_start]**Role-Based Routing:** Users are routed to either the **Buyer Feed** or **Admin Dashboard** based on their account role (Client vs. Admin) [cite: 64-66].

### 2. Buyer Experience
* [cite_start]**Discovery:** A dynamic TilePane feed where users can browse approved products[cite: 81, 122].
* [cite_start]**Search/Filter:** Real-time filtering using SQL `LIKE` operators[cite: 123].
* [cite_start]**Cart Management:** A local cart system utilizing Jackson for JSON serialization, allowing users to save their cart to the hard drive between sessions[cite: 147, 148].

### 3. Seller & Admin Experience
* [cite_start]**Sell Form:** Strict data entry validation, including type-casting for prices and description length limits to prevent UI lag [cite: 91, 99-101].
* [cite_start]**Moderation:** Admins can view a table of "Pending" items to either **Approve** (update to DB) or **Reject** (delete row) [cite: 95-97].

---

## 📂 Project Structure

```text
StudentMarketplace/
├── pom.xml                   # Maven dependencies
├── app_data/                 # Local storage
│   ├── cart.json             # Serialized cart data
│   └── images/               # Uploaded product images
├── src/main/java/com/marketplace/
│   ├── Main.java             # Entry point & Routing
│   ├── UserSession.java      # Session Singleton
│   ├── model/                # User and Product entities
│   ├── dao/                  # Database logic (UserDAO, ProductDAO)
│   └── controller/           # UI logic for all screens
└── src/main/resources/
    ├── view/                 # FXML layouts
    └── database/             # marketplace.db
```

---

## 👥 The Team: CpE 2204

### 2ND GROUP
* [cite_start]**Group 1:** Identity & Data Gateway (Authentication) [cite: 102, 281]
* [cite_start]**Group 4:** The Seller & Admin Experience [cite: 127, 284, 285]
* [cite_start]**Group 7:** The Buyer Experience (Feed & Discovery) [cite: 117, 282, 283]
* [cite_start]**Group 8:** Cart & Local Transactions [cite: 140, 286, 287]
* [cite_start]**Group 9:** Integration, QA & Provisioning (DevOps) [cite: 153, 288, 289]

---

## 🔗 Repository
[https://github.com/JoshuaGzzz/UniNexus](https://github.com/JoshuaGzzz/UniNexus)
