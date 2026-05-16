# UniNexus Marketplace - JavaFX Desktop Application

## 📋 Project Overview

A modern JavaFX desktop application for university students to buy, sell, rent dorms, and share academic materials. Built with a robust OOP architecture, following MVC pattern and SOLID principles.

---

## 📁 Project Structure (Maven Standard)

```
UniNexus-marketplace/
├── pom.xml                                  # Maven configuration
├── database/
│   └── schema.sql                           # SQLite database schema
│
├── src/
│   ├── main/
│   │   ├── java/com/studentmarketplace/
│   │   │   ├── MainApplication.java         # Entry point
│   │   │   │
│   │   │   ├── model/                       # Domain models (M in MVC)
│   │   │   │   ├── Post.java                # Abstract base class
│   │   │   │   ├── Product.java             # Concrete: SALE posts
│   │   │   │   ├── Rental.java              # Concrete: RENTAL posts
│   │   │   │   ├── AcademicResource.java    # Concrete: RESOURCE posts
│   │   │   │   ├── User.java                # User entity
│   │   │   │   ├── Transaction.java         # Transaction history
│   │   │   │   └── Review.java              # User reviews/ratings
│   │   │   │
│   │   │   ├── controller/                  # Controllers (C in MVC)
│   │   │   │   ├── DormitoryListingController.java
│   │   │   │   ├── ProductListingController.java
│   │   │   │   ├── CreatePostController.java
│   │   │   │   ├── UserProfileController.java
│   │   │   │   └── LoginController.java
│   │   │   │
│   │   │   ├── service/                     # Business logic / DAO pattern
│   │   │   │   ├── RentalService.java       # Rental CRUD & queries
│   │   │   │   ├── ProductService.java      # Product CRUD & queries
│   │   │   │   ├── ResourceService.java     # Resource CRUD & queries
│   │   │   │   ├── UserService.java         # User authentication & profile
│   │   │   │   └── TransactionService.java  # Purchase history
│   │   │   │
│   │   │   ├── database/
│   │   │   │   └── DatabaseManager.java     # Singleton connection manager
│   │   │   │
│   │   │   ├── util/
│   │   │   │   ├── ImageUtil.java           # Image loading & caching
│   │   │   │   ├── ValidationUtil.java      # Input validation
│   │   │   │   ├── Constants.java           # App constants
│   │   │   │   └── FileUtil.java            # File operations
│   │   │   │
│   │   │   └── view/                        # Utility classes for views
│   │   │       ├── AlertHelper.java         # Dialog & alert utilities
│   │   │       └── StageManager.java        # Window management
│   │   │
│   │   └── resources/
│   │       ├── fxml/                        # FXML layout files (V in MVC)
│   │       │   ├── dormitory-listing.fxml
│   │       │   ├── product-listing.fxml
│   │       │   ├── create-post.fxml
│   │       │   ├── user-profile.fxml
│   │       │   ├── login.fxml
│   │       │   └── main-window.fxml
│   │       │
│   │       ├── css/                         # Stylesheets (Steam-like design)
│   │       │   ├── dormitory-listing.css
│   │       │   ├── global.css
│   │       │   ├── dark-theme.css
│   │       │   └── light-theme.css
│   │       │
│   │       └── images/                      # Static assets
│   │           ├── icons/
│   │           └── placeholder/
│   │
│   └── test/java/com/studentmarketplace/   # Unit tests
│       ├── model/
│       ├── service/
│       └── util/
│
└── README.md
```

---

## 🏗️ OOP Architecture & Design Patterns

### 1. **Inheritance Hierarchy (Post Polymorphism)**

```
┌─────────────────────────────────────┐
│        Post (Abstract)              │
│  - postId, sellerId, title, etc.   │
│  + getPrice()                       │
│  + getDetailedDescription()         │
│  + validatePostData()               │
│  + getMainImagePath()               │
└─────────────────────────────────────┘
         △         △         △
         │         │         │
    ┌────┘         │         └──────┐
    │              │                │
Product         Rental         AcademicResource
(SALE posts)   (RENTAL posts)  (RESOURCE posts)
- price         - pricePerMonth - filePath
- condition     - bedrooms      - resourceType
- category      - furnished     - downloadCount
```

**Benefits:**
- Unified handling of different post types
- Polymorphic behavior without casting
- Easy to add new post types (just extend Post)

### 2. **Service Layer Pattern (Business Logic)**

```
Controller → Service → DatabaseManager → SQLite
```

**Example: RentalService**
- Encapsulates all rental-related database operations
- Handles transactions and error handling
- Returns clean domain objects (Rental instances)
- Keeps controllers thin and testable

### 3. **Singleton Database Manager**

```java
DatabaseManager.getInstance().getConnection()
```

- Thread-safe singleton manages single DB connection
- Provides transaction support (begin, commit, rollback)
- Automatic resource management with try-with-resources
- Connection pooling ready

### 4. **MVC Pattern Implementation**

- **Model**: Domain objects (Post, User, Rental, etc.)
- **View**: FXML files + CSS styling
- **Controller**: JavaFX Controllers handling UI events

**Example Flow:**
```
1. User interacts with UI (DormitoryListingController)
2. Controller calls RentalService.getRentalsByLocation(location)
3. Service executes database queries and maps to Rental objects
4. Controller updates FXML-bound ObservableList
5. ListView automatically refreshes via JavaFX binding
```

---

## 🗄️ Database Schema

### Key Tables

**users**
- Stores student profiles with authentication
- Tracks rating, account status
- Prevents duplicate usernames/emails

**posts** (Base table)
- Polymorphic table for all post types
- Foreign key to users (seller_id)
- Post type discriminator column

**products, rentals, academic_resources**
- Type-specific tables, joined with posts
- Products: category, condition, quantity
- Rentals: amenities (JSON), location coordinates
- Resources: file metadata, download count

**transactions**
- Purchase history and payment tracking
- Links buyer, seller, post, amount
- Status: PENDING, COMPLETED, FAILED, CANCELLED

**reviews**
- User ratings (1-5 stars)
- Associated with transactions
- Automatic user rating calculation

**See database/schema.sql for complete schema**

---

## 💻 Code Examples

### Model Class: Product.java

```java
public class Product extends Post {
    private int productId;
    private ProductCategory category;
    private double price;
    // ... more fields
    
    @Override
    public double getPrice() {
        return price;
    }
    
    @Override
    public boolean validatePostData() {
        return title != null && !title.trim().isEmpty() &&
               price > 0 &&
               location != null && !location.trim().isEmpty();
    }
}
```

### Service Layer: RentalService.java

```java
public Optional<Integer> createRental(Rental rental) {
    try {
        dbManager.beginTransaction();
        
        // Insert parent post
        Optional<Long> postId = dbManager.executeInsert(
            "INSERT INTO posts ...", rentalParams);
        
        // Insert rental-specific data
        dbManager.executeUpdate("INSERT INTO rentals ...", rentalDetails);
        
        dbManager.commit();
        return Optional.of(postId.get().intValue());
    } catch (SQLException e) {
        dbManager.rollback();
        return Optional.empty();
    }
}
```

### Controller: DormitoryListingController.java

```java
@FXML
private ListView<Rental> rentalListView;

private final RentalService rentalService = new RentalService();

@Override
public void initialize(URL url, ResourceBundle resourceBundle) {
    rentalListView.setCellFactory(param -> new RentalListCell());
    loadDormitories();
}

private void loadDormitories() {
    Thread thread = new Thread(() -> {
        List<Rental> dormitories = rentalService
            .getRentalsByType(RentalType.DORMITORY);
        
        Platform.runLater(() -> {
            rentalsList.setAll(dormitories);
            rentalListView.setItems(rentalsList);
        });
    });
    thread.setDaemon(true);
    thread.start();
}
```

### Database Manager: Singleton with Try-With-Resources

```java
public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;
    
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }
    
    public Optional<Long> executeInsert(String sql, Object... params) {
        try (PreparedStatement stmt = connection.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            setParameters(stmt, params);
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return Optional.of(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            logger.error("Insert failed", e);
        }
        return Optional.empty();
    }
}
```

---

## 🎨 UI/UX Design Guidelines

### Image Handling

**Database Storage:**
```java
// Store relative paths only, not full absolute paths
product.setMainImagePath("products/2024/04/item-123.jpg");

// Load image at runtime
Image image = new Image("file:///" + relativePath);
```

**Image Directory Structure:**
```
src/main/resources/images/
├── products/
│   └── 2024/04/
├── rentals/
│   └── 2024/04/
├── profiles/
├── icons/
└── placeholders/
```

**Best Practices:**
- Generate thumbnails for list views (resize to 100x100px)
- Store full-resolution for detail views
- Use placeholder images while loading
- Implement image caching for performance

### CSS Styling (Steam-like Modern Design)

**Color Scheme:**
```css
/* Primary Colors */
--success: #00a86b (Green - actions, confirms)
--primary: #1b1b2f (Dark blue-gray header)
--accent: #16213e (Lighter variant)
--neutral: #f5f5f5 (Light background)

/* Hover Effects */
-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 5, 0, 0, 2);

/* Modern Rounded Elements */
-fx-border-radius: 6px;
```

**Typography:**
```css
/* Page Title */
-fx-font-size: 28px;
-fx-font-weight: bold;

/* Section Headers */
-fx-font-size: 16px;
-fx-font-weight: bold;

/* Body Text */
-fx-font-size: 12px;
```

---

## 🔧 Maven Build & Run

### Prerequisites
- JDK 17 or higher
- Maven 3.8+

### Build
```bash
mvn clean package
```

### Run
```bash
mvn javafx:run
```

### Create Executable JAR
```bash
mvn clean package shade:shade
java -jar target/student-marketplace-1.0.0.jar
```

---

## 📦 Key Dependencies

```xml
<!-- JavaFX 21 -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>21.0.2</version>
</dependency>

<!-- SQLite JDBC -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.44.0.0</version>
</dependency>

<!-- FontAwesome Icons -->
<dependency>
    <groupId>de.jensd</groupId>
    <artifactId>fontawesomefx-fontawesome</artifactId>
    <version>4.7.0-9.1.2</version>
</dependency>

<!-- Logging -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.9</version>
</dependency>
```

---

## 🚀 Key Features to Implement

- [ ] User authentication with password hashing (BCrypt)
- [ ] Image upload and thumbnail generation
- [ ] In-app messaging between buyers/sellers
- [ ] Rating and review system
- [ ] Search and advanced filtering
- [ ] Payment integration (mock)
- [ ] Export to PDF for lease agreements
- [ ] Notification system

---

## 📝 Best Practices Followed

✅ **Separation of Concerns**
- Models, Controllers, Services, Database layers separated

✅ **SOLID Principles**
- Single Responsibility (each class has one job)
- Open/Closed (Post class open for extension via inheritance)
- Liskov Substitution (Product, Rental, Resource interchangeable)
- Dependency Inversion (Controllers depend on Services, not DB directly)

✅ **Resource Management**
- Try-with-resources for database connections
- Thread-safe Singleton for DatabaseManager
- Proper exception handling and logging

✅ **Performance**
- Database indexing on frequently queried columns
- Lazy loading of images
- Asynchronous database operations on background threads

✅ **User Experience**
- Responsive UI with loading indicators
- Real-time filtering
- Rich ListView cells with images
- Modern CSS styling with hover effects

---

## 🔐 Security Considerations

⚠️ **TODO in Production:**
- Implement BCrypt password hashing (currently placeholder)
- Use parameterized queries (✅ already implemented)
- Add CSRF protection for web APIs
- Implement JWT tokens for API authentication
- Validate all user inputs server-side
- Encrypt sensitive data (phone, payment info)

---

## 📚 Learn More

- **JavaFX Documentation**: https://openjfx.io/
- **SQLite JDBC**: https://github.com/xerial/sqlite-jdbc
- **Maven**: https://maven.apache.org/
- **Design Patterns**: https://refactoring.guru/design-patterns/java

---

**Happy Building! 🚀**
