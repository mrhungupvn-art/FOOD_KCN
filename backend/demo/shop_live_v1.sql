-- COM11 SHOP LIVE V1 demo schema
-- IMPORTANT: demo only. Do not run against production before backup/review.

CREATE TABLE IF NOT EXISTS shops (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_user_id BIGINT UNSIGNED NOT NULL,
  name VARCHAR(160) NOT NULL,
  slug VARCHAR(180) NOT NULL,
  logo_url VARCHAR(500) NULL,
  description TEXT NULL,
  status ENUM('pending','active','suspended','closed') NOT NULL DEFAULT 'pending',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_shops_slug (slug),
  KEY idx_shops_owner (owner_user_id),
  KEY idx_shops_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS categories (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  slug VARCHAR(140) NOT NULL,
  status TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uq_categories_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  shop_id BIGINT UNSIGNED NOT NULL,
  category_id BIGINT UNSIGNED NULL,
  name VARCHAR(220) NOT NULL,
  slug VARCHAR(240) NOT NULL,
  description TEXT NULL,
  price_vnd BIGINT UNSIGNED NOT NULL DEFAULT 0,
  stock INT UNSIGNED NOT NULL DEFAULT 0,
  image_url VARCHAR(500) NULL,
  status ENUM('draft','active','hidden','out_of_stock') NOT NULL DEFAULT 'draft',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_products_shop_slug (shop_id, slug),
  KEY idx_products_shop_status (shop_id, status),
  KEY idx_products_category (category_id),
  CONSTRAINT fk_products_shop FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_images (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  product_id BIGINT UNSIGNED NOT NULL,
  image_url VARCHAR(500) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_product_images_product (product_id),
  CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS carts (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_carts_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cart_items (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  cart_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  quantity INT UNSIGNED NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_cart_product (cart_id, product_id),
  KEY idx_cart_items_product (product_id),
  CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
  CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS shop_live_streams (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  shop_id BIGINT UNSIGNED NOT NULL,
  host_user_id BIGINT UNSIGNED NOT NULL,
  title VARCHAR(220) NOT NULL,
  cover_url VARCHAR(500) NULL,
  stream_key_hash CHAR(64) NULL,
  playback_url VARCHAR(700) NULL,
  status ENUM('scheduled','live','ended','blocked') NOT NULL DEFAULT 'scheduled',
  viewer_count INT UNSIGNED NOT NULL DEFAULT 0,
  started_at DATETIME NULL,
  ended_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_live_shop_status (shop_id, status),
  KEY idx_live_status_started (status, started_at),
  CONSTRAINT fk_live_shop FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS shop_live_products (
  live_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  pinned TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (live_id, product_id),
  KEY idx_live_products_product (product_id),
  CONSTRAINT fk_live_products_live FOREIGN KEY (live_id) REFERENCES shop_live_streams(id) ON DELETE CASCADE,
  CONSTRAINT fk_live_products_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS shop_live_messages (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  live_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NULL,
  display_name VARCHAR(120) NOT NULL,
  message VARCHAR(500) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_live_messages_live_id (live_id, id),
  CONSTRAINT fk_live_messages_live FOREIGN KEY (live_id) REFERENCES shop_live_streams(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Keep chat retention bounded. A scheduled job can remove messages older than 30 days.
-- DELETE FROM shop_live_messages WHERE created_at < (NOW() - INTERVAL 30 DAY);
