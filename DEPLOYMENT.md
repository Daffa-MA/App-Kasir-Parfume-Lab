# Railway Deployment Guide

## Quick Deploy (Click Button)

1. Buat akun di https://railway.app
2. Klik tombol "New Project" → "Deploy from GitHub repo"
3. Pilih repository ini
4. Railway会自动检测 Node.js 项目

## Atau Manual Deploy

```bash
# Install Railway CLI
npm install -g @railway/cli

# Login
railway login

# Initialize project
railway init

# Deploy
railway up
```

## Setelah Deploy

1. Railway会给你一个 URL, 比如: `https://your-app.up.railway.app`
2. Copy URL itu ke `RetrofitClient.kt`:
   ```kotlin
   private const val BASE_URL = "https://your-app.up.railway.app/api/"
   ```
3. Build ulang APK

## Catatan

- Railway requires credit card untuk deploy (gratis tier 500 jam/bulan)
- Untuk alternatif gratis tanpa credit card, bisa pakai:
  - **Render** (https://render.com)
  - **Fly.io** (https://fly.io)
  - **Cyclic** (https://cyclic.sh) - gratis, no credit card