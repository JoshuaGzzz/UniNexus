-- Student Marketplace Database Schema
-- SQLite3

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    full_name TEXT NOT NULL,
    student_id TEXT UNIQUE NOT NULL,
    university TEXT NOT NULL,
    phone TEXT,
    profile_image_path TEXT,
    bio TEXT,
    rating REAL DEFAULT 5.0 CHECK (rating >= 0.0 AND rating <= 5.0),
    total_ratings INTEGER DEFAULT 0,
    account_status TEXT DEFAULT 'ACTIVE' CHECK (account_status IN ('ACTIVE', 'SUSPENDED', 'BANNED')),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Posts Table (Base for polymorphic posts)
CREATE TABLE IF NOT EXISTS posts (
    post_id INTEGER PRIMARY KEY AUTOINCREMENT,
    seller_id INTEGER NOT NULL,
    post_type TEXT NOT NULL CHECK (post_type IN ('SALE', 'RENTAL', 'RESOURCE')),
    title TEXT NOT NULL,
    description TEXT,
    status TEXT DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SOLD', 'RENTED', 'ARCHIVED', 'DELETED')),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Products Table (For SALE type posts)
CREATE TABLE IF NOT EXISTS products (
    product_id INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id INTEGER UNIQUE NOT NULL,
    category TEXT NOT NULL CHECK (category IN ('ELECTRONICS', 'TEXTBOOKS', 'FURNITURE', 'CLOTHING', 'ACCESSORIES', 'OTHER')),
    price REAL NOT NULL CHECK (price > 0),
    condition TEXT DEFAULT 'GOOD' CHECK (condition IN ('LIKE_NEW', 'GOOD', 'FAIR', 'POOR')),
    quantity INTEGER DEFAULT 1 CHECK (quantity > 0),
    location TEXT NOT NULL,
    main_image_path TEXT,
    thumbnail_image_path TEXT,
    FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE
);

-- Product Images Table (Multiple images per product)
CREATE TABLE IF NOT EXISTS product_images (
    image_id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id INTEGER NOT NULL,
    image_path TEXT NOT NULL,
    display_order INTEGER DEFAULT 0,
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
);

-- Rentals Table (For RENTAL type posts)
CREATE TABLE IF NOT EXISTS rentals (
    rental_id INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id INTEGER UNIQUE NOT NULL,
    rental_type TEXT NOT NULL CHECK (rental_type IN ('DORMITORY', 'APARTMENT', 'ROOM', 'HOUSE')),
    location TEXT NOT NULL,
    latitude REAL,
    longitude REAL,
    price_per_month REAL NOT NULL CHECK (price_per_month > 0),
    bedrooms INTEGER DEFAULT 1,
    bathrooms INTEGER DEFAULT 1,
    area_sqm REAL,
    furnished BOOLEAN DEFAULT 0,
    amenities TEXT, -- JSON: ["wifi", "parking", "gym"]
    available_from DATE,
    main_image_path TEXT,
    thumbnail_image_path TEXT,
    FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE
);

-- Rental Images Table
CREATE TABLE IF NOT EXISTS rental_images (
    image_id INTEGER PRIMARY KEY AUTOINCREMENT,
    rental_id INTEGER NOT NULL,
    image_path TEXT NOT NULL,
    display_order INTEGER DEFAULT 0,
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (rental_id) REFERENCES rentals(rental_id) ON DELETE CASCADE
);

-- Academic Resources Table (For RESOURCE type posts)
CREATE TABLE IF NOT EXISTS academic_resources (
    resource_id INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id INTEGER UNIQUE NOT NULL,
    resource_type TEXT NOT NULL CHECK (resource_type IN ('SOFTWARE', 'PDF', 'RESEARCH_PAPER', 'NOTES', 'OTHER')),
    file_path TEXT NOT NULL,
    file_size_mb REAL,
    price REAL DEFAULT 0.0 CHECK (price >= 0),
    download_count INTEGER DEFAULT 0,
    subject_area TEXT,
    course_code TEXT,
    university TEXT,
    FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE
);

-- Transactions Table (Purchase history)
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id INTEGER PRIMARY KEY AUTOINCREMENT,
    buyer_id INTEGER NOT NULL,
    seller_id INTEGER NOT NULL,
    post_id INTEGER NOT NULL,
    amount REAL NOT NULL,
    transaction_type TEXT NOT NULL CHECK (transaction_type IN ('SALE', 'RENTAL_PAYMENT', 'RESOURCE_PURCHASE')),
    status TEXT DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    payment_method TEXT DEFAULT 'CASH' CHECK (payment_method IN ('CASH', 'ONLINE_TRANSFER', 'CREDIT_CARD')),
    transaction_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    completion_date DATETIME,
    FOREIGN KEY (buyer_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE
);

-- Reviews/Ratings Table
CREATE TABLE IF NOT EXISTS reviews (
    review_id INTEGER PRIMARY KEY AUTOINCREMENT,
    reviewer_id INTEGER NOT NULL,
    reviewed_user_id INTEGER NOT NULL,
    transaction_id INTEGER,
    rating REAL NOT NULL CHECK (rating >= 1.0 AND rating <= 5.0),
    review_text TEXT,
    review_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reviewer_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE SET NULL,
    UNIQUE(reviewer_id, reviewed_user_id, transaction_id)
);

-- Favorites/Wishlist Table
CREATE TABLE IF NOT EXISTS favorites (
    favorite_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    post_id INTEGER NOT NULL,
    added_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE,
    UNIQUE(user_id, post_id)
);

-- Messages Table (For in-app messaging)
CREATE TABLE IF NOT EXISTS messages (
    message_id INTEGER PRIMARY KEY AUTOINCREMENT,
    sender_id INTEGER NOT NULL,
    receiver_id INTEGER NOT NULL,
    subject TEXT,
    message_text TEXT NOT NULL,
    is_read BOOLEAN DEFAULT 0,
    sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME,
    FOREIGN KEY (sender_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(user_id) ON DELETE CASCADE
);


--Refund
CREATE TABLE refund_requests (
refund_id INTEGER PRIMARY KEY AUTOINCREMENT,
buyer_id INTEGER NOT NULL,
transaction_id INTEGER NOT NULL,
reason TEXT NOT NULL,
status TEXT DEFAULT 'PENDING',
created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
FOREIGN KEY (buyer_id) REFERENCES users(user_id),
FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id)
);

-- Create Indexes for Performance
CREATE INDEX IF NOT EXISTS idx_posts_seller_id ON posts(seller_id);
CREATE INDEX IF NOT EXISTS idx_posts_status ON posts(status);
CREATE INDEX IF NOT EXISTS idx_posts_created_at ON posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_location ON products(location);
CREATE INDEX IF NOT EXISTS idx_rentals_location ON rentals(location);
CREATE INDEX IF NOT EXISTS idx_transactions_buyer_id ON transactions(buyer_id);
CREATE INDEX IF NOT EXISTS idx_transactions_seller_id ON transactions(seller_id);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_messages_receiver_id ON messages(receiver_id);
CREATE INDEX IF NOT EXISTS idx_messages_is_read ON messages(is_read);
CREATE INDEX IF NOT EXISTS idx_favorites_user_id ON favorites(user_id);
