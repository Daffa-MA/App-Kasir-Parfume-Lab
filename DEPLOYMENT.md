# Firebase App Distribution Guide

## Tujuan

Guide ini untuk distribusi APK Android lewat Firebase App Distribution menggunakan GitHub Actions.

## 1) Setup di Firebase Console

1. Buka [Firebase Console](https://console.firebase.google.com/).
2. Pilih project Firebase yang dipakai aplikasi ini.
3. Tambahkan Android app jika belum ada.
4. Aktifkan **App Distribution**.
5. Catat **Firebase App ID** Android (format: `1:xxx:android:yyy`).
6. Buat tester group di App Distribution (contoh: `internal-testers`).

## 2) Buat Service Account untuk CI

1. Buka **Project settings** -> **Service accounts**.
2. Klik **Generate new private key**.
3. Simpan file JSON key dengan aman.

## 3) Set GitHub Secrets

Di repository GitHub -> **Settings** -> **Secrets and variables** -> **Actions**, tambahkan:

- `FIREBASE_APP_ID`: Firebase App ID Android.
- `FIREBASE_SERVICE_ACCOUNT`: isi penuh JSON service account (copy-paste seluruh konten file JSON).
- `FIREBASE_TESTER_GROUPS`: nama grup tester, contoh `internal-testers`.

## 4) Trigger Distribusi

Workflow ada di `.github/workflows/android-build.yml` dan otomatis jalan saat:

- push ke branch `main`
- dijalankan manual dari tab **Actions** (`workflow_dispatch`)

Hasilnya:

- APK debug tetap di-upload sebagai artifact GitHub Actions.
- APK yang sama didistribusikan ke Firebase App Distribution.

## 5) Verifikasi

1. Cek run workflow di GitHub Actions harus sukses.
2. Buka Firebase Console -> **App Distribution**.
3. Pastikan release baru muncul dan terkirim ke tester group.