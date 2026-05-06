CREATE TABLE IF NOT EXISTS home_section_block (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    block_key VARCHAR(120) NOT NULL UNIQUE,
    block_type VARCHAR(40) NOT NULL,
    title VARCHAR(300) NOT NULL,
    content TEXT NULL,
    display_order INT NOT NULL,
    built_in BIT NOT NULL
);

-- Trường hợp bảng đã tồn tại từ bản cũ nhưng thiếu cột (tương thích MySQL 5.7+):
SET @db_name = DATABASE();

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = @db_name AND table_name = 'home_section_block' AND column_name = 'block_key'
        ),
        'SELECT 1',
        'ALTER TABLE home_section_block ADD COLUMN block_key VARCHAR(120) NULL'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = @db_name AND table_name = 'home_section_block' AND column_name = 'block_type'
        ),
        'SELECT 1',
        'ALTER TABLE home_section_block ADD COLUMN block_type VARCHAR(40) NULL'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = @db_name AND table_name = 'home_section_block' AND column_name = 'title'
        ),
        'SELECT 1',
        'ALTER TABLE home_section_block ADD COLUMN title VARCHAR(300) NULL'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = @db_name AND table_name = 'home_section_block' AND column_name = 'content'
        ),
        'SELECT 1',
        'ALTER TABLE home_section_block ADD COLUMN content TEXT NULL'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = @db_name AND table_name = 'home_section_block' AND column_name = 'display_order'
        ),
        'SELECT 1',
        'ALTER TABLE home_section_block ADD COLUMN display_order INT NULL'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = @db_name AND table_name = 'home_section_block' AND column_name = 'built_in'
        ),
        'SELECT 1',
        'ALTER TABLE home_section_block ADD COLUMN built_in BIT NULL'
    )
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Hoàn thiện ràng buộc cơ bản cho dữ liệu mới.
ALTER TABLE home_section_block
    MODIFY block_key VARCHAR(120) NOT NULL,
    MODIFY block_type VARCHAR(40) NOT NULL,
    MODIFY title VARCHAR(300) NOT NULL,
    MODIFY display_order INT NOT NULL,
    MODIFY built_in BIT NOT NULL;

INSERT INTO home_section_block (block_key, block_type, title, content, display_order, built_in)
SELECT 'banner', 'BUILT_IN', 'Banner', NULL, 1, b'1'
WHERE NOT EXISTS (SELECT 1 FROM home_section_block WHERE block_key = 'banner');

INSERT INTO home_section_block (block_key, block_type, title, content, display_order, built_in)
SELECT 'featured', 'BUILT_IN', 'Khối nổi bật', NULL, 2, b'1'
WHERE NOT EXISTS (SELECT 1 FROM home_section_block WHERE block_key = 'featured');

INSERT INTO home_section_block (block_key, block_type, title, content, display_order, built_in)
SELECT 'recommended', 'BUILT_IN', 'Khối gợi ý', NULL, 3, b'1'
WHERE NOT EXISTS (SELECT 1 FROM home_section_block WHERE block_key = 'recommended');

INSERT INTO home_section_block (block_key, block_type, title, content, display_order, built_in)
SELECT 'recentlyViewed', 'BUILT_IN', 'Khối vừa xem', NULL, 4, b'1'
WHERE NOT EXISTS (SELECT 1 FROM home_section_block WHERE block_key = 'recentlyViewed');

INSERT INTO home_section_block (block_key, block_type, title, content, display_order, built_in)
SELECT 'byCategory', 'BUILT_IN', 'Khối theo loại', NULL, 5, b'1'
WHERE NOT EXISTS (SELECT 1 FROM home_section_block WHERE block_key = 'byCategory');
