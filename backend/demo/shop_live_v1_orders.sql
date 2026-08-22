-- Order extension for COM11 SHOP LIVE V1 demo.
CREATE TABLE IF NOT EXISTS shop_orders (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  status ENUM('pending','confirmed','processing','shipping','completed','cancelled') NOT NULL DEFAULT 'pending',
  subtotal_vnd BIGINT UNSIGNED NOT NULL DEFAULT 0,
  shipping_vnd BIGINT UNSIGNED NOT NULL DEFAULT 0,
  total_vnd BIGINT UNSIGNED NOT NULL DEFAULT 0,
  shipping_name VARCHAR(160) NULL,
  shipping_phone VARCHAR(40) NULL,
  shipping_address VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_orders_user_status (user_id, status),
  KEY idx_orders_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS shop_order_items (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  order_id BIGINT UNSIGNED NOT NULL,
  shop_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  product_name VARCHAR(220) NOT NULL,
  unit_price_vnd BIGINT UNSIGNED NOT NULL,
  quantity INT UNSIGNED NOT NULL,
  subtotal_vnd BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (id),
  KEY idx_order_items_order (order_id),
  KEY idx_order_items_shop (shop_id),
  KEY idx_order_items_product (product_id),
  CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES shop_orders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
