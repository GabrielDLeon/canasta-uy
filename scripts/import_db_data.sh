#!/bin/bash
#
# import_db_data.sh
# Imports CSVs to PostgreSQL using docker exec (no local psql required)
#
# Usage: ./scripts/import_db_data.sh

set -e

# Config from .env or defaults
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-canastauy}"
DB_USER="${DB_USER:-canastauy_user}"
DB_PASS="${DB_PASSWORD:-canastauy_pass}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
IMPORT_DIR="$PROJECT_DIR/data/processed/db_import"

echo "============================================"
echo "IMPORT DB DATA - PostgreSQL"
echo "============================================"
echo "Connecting to: $DB_HOST:$DB_PORT/$DB_NAME"
echo ""

# Function to import CSV with COPY via docker
import_csv() {
	local table=$1
	local file=$2
	local columns=$3

	echo "Importing $table..."

	# Truncate table
	docker exec -i canasta-postgres psql -U "$DB_USER" -d "$DB_NAME" -c "TRUNCATE TABLE $table CASCADE;"

	# Copy file to container and execute COPY
	docker cp "$file" canasta-postgres:/tmp/import.csv
	docker exec -i canasta-postgres psql -U "$DB_USER" -d "$DB_NAME" <<EOF
COPY $table ($columns) FROM '/tmp/import.csv' WITH (FORMAT csv, HEADER true);
EOF

	# Clean up temp file
	docker exec canasta-postgres rm /tmp/import.csv

	echo "  Done"
}

# Check if container is running
if ! docker ps | grep -q canasta-postgres; then
	echo "ERROR: Container canasta-postgres is not running"
	echo "Run first: just infra-up"
	exit 1
fi

# 1. Categories
echo "[1/3] Importing categories..."
import_csv "categories" "$IMPORT_DIR/categories.csv" "category_id,name"

# 2. Products
echo "[2/3] Importing products..."
import_csv "products" "$IMPORT_DIR/products_db.csv" "product_id,name,specification,category_id,brand"

# 3. Prices
echo ""
echo "[3/3] Importing prices (this may take a while)..."
import_csv "prices" "$IMPORT_DIR/prices_db.csv" "product_id,date,price_min,price_max,price_avg,price_median,price_std,store_count,offer_count,offer_percentage"

echo ""
echo "Import completed successfully!"
