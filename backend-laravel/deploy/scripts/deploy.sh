#!/usr/bin/env bash
set -euo pipefail

APP_DIR="/var/www/perfume-lab-pos/backend-laravel"
PHP_BIN="/usr/bin/php"
COMPOSER_BIN="/usr/bin/composer"

cd "$APP_DIR"

echo "[1/8] Enable maintenance mode"
$PHP_BIN artisan down || true

echo "[2/8] Fetch latest code"
git fetch origin
git checkout main
git reset --hard origin/main

echo "[3/8] Install PHP dependencies"
$COMPOSER_BIN install --no-interaction --no-dev --prefer-dist --optimize-autoloader

echo "[4/8] Run migrations"
$PHP_BIN artisan migrate --force

echo "[5/8] Cache config/routes/views"
$PHP_BIN artisan config:cache
$PHP_BIN artisan route:cache
$PHP_BIN artisan view:cache

echo "[6/8] Ensure writable directories"
chown -R www-data:www-data storage bootstrap/cache
chmod -R ug+rwx storage bootstrap/cache

echo "[7/8] Restart queue workers"
$PHP_BIN artisan queue:restart
supervisorctl reread
supervisorctl update
supervisorctl restart perfume-lab-pos-queue:*

echo "[8/8] Disable maintenance mode"
$PHP_BIN artisan up

echo "Deployment finished successfully"
