# Production Deploy Guide (Laravel)

## 1. Environment
Set these env values in `.env` on server:

- `APP_ENV=production`
- `APP_DEBUG=false`
- `APP_URL=https://api.perfumelabpos.com`
- `DB_CONNECTION=sqlite` or your production database config
- `POS_API_TOKEN=<your-strong-random-token>`

If using SQLite:

- Create `database/database.sqlite`
- Ensure writable: `storage/` and `bootstrap/cache/`

## 2. Web Server
Choose one:

- Nginx template: `deploy/nginx/perfume-lab-pos.conf`
- Apache template: `deploy/apache/perfume-lab-pos.conf`

Document root must point to Laravel `public/` folder.

## 3. Queue Worker (Supervisor)
Use `deploy/supervisor/laravel-queue.conf` then run:

```bash
sudo supervisorctl reread
sudo supervisorctl update
sudo supervisorctl start perfume-lab-pos-queue:*
```

## 4. Deploy Script
Use `deploy/scripts/deploy.sh`:

```bash
chmod +x deploy/scripts/deploy.sh
sudo deploy/scripts/deploy.sh
```

## 5. Legacy Data Import
Import historical data from old Node SQLite DB:

```bash
php artisan pos:import-legacy --path=../backend/pos.db
```

## 6. Token on Android
Set same token in Android clients and backend `.env`:

- Backend: `POS_API_TOKEN=...`
- Android sends `Authorization: Bearer <token>`
