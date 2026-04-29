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


CREATE TABLE IF NOT EXISTS product_images (
    image_id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id INTEGER NOT NULL,
    image_path TEXT NOT NULL,
    display_order INTEGER DEFAULT 0,
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
);


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



CREATE TABLE IF NOT EXISTS rental_images (
    image_id INTEGER PRIMARY KEY AUTOINCREMENT,
    rental_id INTEGER NOT NULL,
    image_path TEXT NOT NULL,
    display_order INTEGER DEFAULT 0,
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (rental_id) REFERENCES rentals(rental_id) ON DELETE CASCADE
);


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

--Still processing Joshua

--note for table created that's there 8 tables under one database called Marketplace
-- products for personal items like shirts, jersey, computer, tablets, shoes
-- rentals for dorm, house, apartment
-- resources for sharing lab report, lecture notes, software (e.g you shared me your cracked the multism software)

--I think later you'll create more table for more UI