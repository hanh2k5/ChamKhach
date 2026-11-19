FROM php:8.2-cli-alpine

# Cài đặt extension cần thiết cho Laravel + SQLite
RUN apk add --no-cache sqlite sqlite-dev sqlite-libs \
    && docker-php-ext-install pdo_sqlite pcntl bcmath opcache

WORKDIR /var/www/html

# Lấy Composer
COPY --from=composer:latest /usr/bin/composer /usr/bin/composer

# Copy dependency files
COPY composer.json composer.lock ./
RUN composer install --no-dev --no-scripts --no-autoloader --prefer-dist

# Copy mã nguồn backend
COPY . .

# Tối ưu autoload
RUN composer dump-autoload --optimize --no-dev

# Tạo thư mục và cấp quyền storage / database SQLite
RUN mkdir -p database storage/framework/cache storage/framework/sessions storage/framework/views storage/logs \
    && touch database/database.sqlite \
    && chmod -R 777 storage database

EXPOSE 8000

# Khởi chạy: migrate, seed và serve trên cổng Render cấp ($PORT)
CMD ["sh", "-c", "php artisan key:generate --force && php artisan migrate --force && php artisan db:seed --class=DemoCustomerSeeder --force && php artisan serve --host=0.0.0.0 --port=${PORT:-8000}"]
