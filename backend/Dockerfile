FROM php:8.3-cli-alpine

# Cài đặt extension cần thiết cho Laravel + SQLite + PostgreSQL (Supabase)
RUN apk add --no-cache sqlite sqlite-dev sqlite-libs postgresql-dev \
    && docker-php-ext-install pdo_sqlite pdo_pgsql pcntl bcmath opcache

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

# Tạo thư mục, file .env với cấu hình chuẩn và cấp quyền storage / database
RUN echo "APP_KEY=base64:YVLaG0dZULhUiyEBXVR0Gqv+AVe24JL7cO1x3XZBOP8=" > .env \
    && echo "APP_NAME=Telesales" >> .env \
    && echo "APP_ENV=production" >> .env \
    && echo "APP_DEBUG=false" >> .env \
    && echo "SESSION_DRIVER=cookie" >> .env \
    && echo "opcache.enable_cli=1" >> /usr/local/etc/php/conf.d/docker-php-ext-opcache.ini \
    && echo "opcache.memory_consumption=128" >> /usr/local/etc/php/conf.d/docker-php-ext-opcache.ini \
    && echo "opcache.max_accelerated_files=10000" >> /usr/local/etc/php/conf.d/docker-php-ext-opcache.ini \
    && echo "opcache.validate_timestamps=0" >> /usr/local/etc/php/conf.d/docker-php-ext-opcache.ini \
    && mkdir -p database storage/framework/cache storage/framework/sessions storage/framework/views storage/logs \
    && touch database/database.sqlite \
    && chmod -R 777 storage database .env

EXPOSE 8000

# Khởi chạy: kiểm tra biến DB_CONNECTION và DATABASE_URL từ Render, migrate, seed, cache và serve
CMD ["sh", "-c", "if [ -n \"$DATABASE_URL\" ]; then echo \"DATABASE_URL=$DATABASE_URL\" >> .env; echo \"DB_URL=$DATABASE_URL\" >> .env; echo \"DB_CONNECTION=pgsql\" >> .env; fi; php artisan migrate --force && php artisan db:seed --class=UserSeeder --force && php artisan db:seed --class=DemoCustomerSeeder --force && php artisan config:cache && php artisan route:cache && php artisan serve --host=0.0.0.0 --port=${PORT:-8000}"]

