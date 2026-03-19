-- Tạo 4 databases riêng cho từng service
CREATE DATABASE users_db;
CREATE DATABASE products_db;
CREATE DATABASE orders_db;
CREATE DATABASE notifications_db;

-- Grant tất cả quyền cho user ecom
GRANT ALL PRIVILEGES ON DATABASE users_db TO ecom;
GRANT ALL PRIVILEGES ON DATABASE products_db TO ecom;
GRANT ALL PRIVILEGES ON DATABASE orders_db TO ecom;
GRANT ALL PRIVILEGES ON DATABASE notifications_db TO ecom;
