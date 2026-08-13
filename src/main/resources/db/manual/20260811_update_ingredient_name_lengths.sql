-- Manual MariaDB migration for ingredient OCR matching.
-- This does not delete, truncate, or reseed data.

ALTER TABLE ingredient
    MODIFY COLUMN name VARCHAR(1000) NOT NULL;

ALTER TABLE ingredient_alias
    MODIFY COLUMN alias VARCHAR(2000) NOT NULL;
