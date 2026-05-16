# Quick Start Guide - Student Marketplace

## ⚡ Getting Started in 5 Minutes

### 1. Prerequisites
- **JDK 17+** → [Download](https://www.oracle.com/java/technologies/downloads/#java17)
- **Maven 3.8+** → [Download](https://maven.apache.org/download.cgi)
- **Git** (optional) → [Download](https://git-scm.com/)

### 2. Verify Installation
```bash
java -version
# Should show Java 17 or higher

mvn -version
# Should show Maven 3.8 or higher
```

---

## 🏗️ Project Setup

### Step 1: Navigate to Project
```bash
cd C:\Users\Hkawng\ Zam\ Jap\OneDrive\Desktop\OOP\ Final\ Project
```

### Step 2: Initial Maven Build
```bash
mvn clean install
```
This will:
- Download all dependencies (JavaFX, SQLite, SLF4J, etc.)
- Compile source code
- Create database folder

**First build takes 2-5 minutes** ☕

---

## ▶️ Running the Application

### Option A: Direct Run (Recommended for Development)
```bash
mvn javafx:run
```

After launch, you will land on the Login screen.

You can now create a real account from the **Register** button.

Demo accounts:
- Client: `client` / `password123`
- Admin: `admin` / `password123`

### Option B: Run as Standalone JAR
```bash
# Build executable JAR
mvn clean package

# Run the JAR
java -jar target/student-marketplace-1.0.0.jar
```

Note: this project currently depends on the JavaFX runtime being available on the module path. In this build, `java -jar` is not a fully self-contained launch method, so `mvn javafx:run` is the supported way to start the app.

---

## 📊 Database Setup

The database **initializes automatically** on first run:

1. `student_marketplace.db` created in project root
2. All tables created from schema
3. Ready for CRUD operations

### Manual Database Reset (if needed)
```bash
# Delete the database file
rm student_marketplace.db

# Restart the app - new database created
mvn javafx:run
```

---

## 🔍 Testing the Application

### Test: Login + Role-Based Screens

1. **Run the app**: `mvn javafx:run`
2. **Register a new client account** from Login (or use demo client)
3. **Login as Client**: `client / password123` (or your new account)
4. **Create a post** in Client Dashboard (title, location, price, description)
5. Click **My Marketplace** and verify:
       - Post details are shown
       - Edit/save works
       - Delete marks post as deleted
6. **Login as Admin**: `admin / password123`
7. **View all posts** and archive/activate selected entries

### Browse Public Listings
1. From Login, click **Browse Listings**
2. Use search + filters in Dormitory Listing screen

---

## 🐛 Troubleshooting

### Problem: `java.lang.NoModuleFoundError: javafx.controls`
**Solution**: Ensure JAVA_HOME points to correct JDK
```bash
# On Windows
set JAVA_HOME=C:\Program Files\Java\jdk-17
mvn clean javafx:run
```

### Problem: Database locked error
**Solution**: Close other instances
```bash
# Kill any Java processes
taskkill /F /IM java.exe

# Then restart
mvn javafx:run
```

### Problem: `FXML file not found`
**Solution**: Ensure resources are properly packaged
```bash
mvn clean package
mvn javafx:run
```

### Problem: Icons not loading
- Place PNG files in `src/main/resources/images/`
- Reference as: `getResource("images/filename.png")`

---

## 📂 Key Files to Know

| File | Purpose |
|------|---------|
| `pom.xml` | Maven dependencies & build config |
| `database/schema.sql` | Database table definitions |
| `src/main/java/com/studentmarketplace/MainApplication.java` | Entry point |
| `src/main/java/com/studentmarketplace/controller/LoginController.java` | Login + role routing |
| `src/main/java/com/studentmarketplace/controller/RegisterController.java` | User registration flow |
| `src/main/java/com/studentmarketplace/controller/ClientDashboardController.java` | Client post creation + My Posts |
| `src/main/java/com/studentmarketplace/controller/ClientMarketplaceController.java` | Client post details + edit/delete |
| `src/main/java/com/studentmarketplace/controller/AdminDashboardController.java` | Admin moderation view |
| `src/main/java/com/studentmarketplace/controller/DormitoryListingController.java` | Main UI logic |
| `src/main/java/com/studentmarketplace/service/RentalService.java` | Business logic |
| `src/main/java/com/studentmarketplace/service/AuthService.java` | Authentication logic |
| `src/main/java/com/studentmarketplace/service/PostManagementService.java` | Dashboard post operations |
| `src/main/java/com/studentmarketplace/database/DatabaseManager.java` | DB connection |
| `src/main/resources/fxml/login.fxml` | Login UI |
| `src/main/resources/fxml/register.fxml` | Registration UI |
| `src/main/resources/fxml/client-dashboard.fxml` | Client dashboard UI |
| `src/main/resources/fxml/client-marketplace.fxml` | Client marketplace UI |
| `src/main/resources/fxml/admin-dashboard.fxml` | Admin dashboard UI |
| `src/main/resources/fxml/dormitory-listing.fxml` | UI layout (XML) |
| `src/main/resources/css/global.css` | Styling |

---

## ✏️ Next Steps (Implementation)

### 1. Add More Controllers
Create controllers for other views:
```
ProductListingController.java
CreatePostController.java
UserProfileController.java
LoginController.java
```

### 2. Create FXML Files
```
product-listing.fxml
create-post.fxml
user-profile.fxml
login.fxml
```

### 3. Implement Services
```
ProductService.java
UserService.java
AcademicResourceService.java
```

### 4. Add Features
- File upload
- Image processing
- Payment integration
- User authentication

---

## 🎯 Architecture at a Glance

```
User Input (UI)
       ↓
Controller (handles events)
       ↓
Service (business logic)
       ↓
DatabaseManager (CRUD)
       ↓
SQLite Database
```

**Example**: User searches for dormitories

1. **Controller** catches text input in search field
2. **Service** queries database: `getRentalsByLocation(text)`
3. **DatabaseManager** executes SQL with try-with-resources
4. **Service** maps ResultSet → Rental objects
5. **Controller** updates ListView
6. **JavaFX** auto-refreshes UI

---

## 📝 Important Notes

✅ **Good Practices to Follow:**
- Keep business logic in Services, not Controllers
- Use try-with-resources for database access
- Bind UI to Observable collections (JavaFX)
- Run long operations on background threads
- Handle exceptions with logging

❌ **Avoid:**
- Database code in Controllers
- Blocking UI with heavy operations
- Hard-coded SQL strings (use parameterized)
- Not closing database connections

---

## 🚀 Build for Distribution

### Create Standalone EXE (Windows)

Install JPackage (comes with JDK 17+):
```bash
# Build executable
jpackage --input target \
         --name StudentMarketplace \
         --main-jar student-marketplace-1.0.0.jar \
         --main-class com.studentmarketplace.MainApplication \
         --type exe \
         --icon src/main/resources/images/app-icon.ico
```

---

## 📞 Common Commands

```bash
# Clean build
mvn clean install

# Run application
mvn javafx:run

# Run tests
mvn test

# Create JAR
mvn package

# View dependencies
mvn dependency:tree

# Update dependencies
mvn versions:display-dependency-updates
```

---

**Ready to code? Let's build! 🚀**

For detailed architecture, see: `PROJECT_ARCHITECTURE.md`
