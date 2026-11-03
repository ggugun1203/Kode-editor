# DroidStudio AI 🚀

DroidStudio AI adalah aplikasi Android IDE lengkap berbasis mobile-first yang menggabungkan kemampuan **Visual Studio Code**, **XAMPP Local Server**, dan **AI Coding Assistant (Google Gemini)**. Aplikasi dapat digunakan sepenuhnya langsung dari smartphone Android tanpa memerlukan komputer.

---

## 🛠️ Otomatisasi Build APK via GitHub Actions

Repositori ini sudah dilengkapi alur kerja CI/CD otomatis di `.github/workflows/build.yml` untuk mengompilasi APK langsung di server GitHub.

### 📌 Langkah Push ke GitHub dari Komputer / Termux / Smartphone:

1. **Inisialisasi Git & Commit:**
   ```bash
   git init
   git add .
   git commit -m "Initial commit DroidStudio AI"
   ```

2. **Hubungkan ke Repositori GitHub Anda:**
   ```bash
   git branch -M main
   git remote add origin https://github.com/USERNAME/NAMA-REPO.git
   git push -u origin main
   ```

3. **Proses Build Otomatis Berjalan:**
   - Masuk ke repositori GitHub Anda di browser.
   - Buka tab **Actions**.
   - Alur kerja **"Build DroidStudio AI APK"** akan otomatis berjalan mengompilasi APK.
   - Anda juga dapat menjalankannya secara manual dengan menekan tombol **"Run workflow"**.

4. **Unduh File APK:**
   - Klik proses build yang selesai (ditandai dengan centang hijau).
   - Di bagian bawah halaman pada tabel **Artifacts**, klik **`DroidStudio-AI-debug-apk`**.
   - Ekstrak file zip dan instal file `.apk` ke smartphone Android Anda!

---

## 🌟 Fitur Utama DroidStudio AI

- **Code Editor**: Multi-tab, syntax highlighting (12+ bahasa termasuk HTML, CSS, JS, PHP, Python, SQL, Kotlin), nomor baris, undo/redo, auto-save, dan quick symbol bar untuk keyboard ponsel.
- **AI Coding Assistant**: Ditenagai Google Gemini API (model `gemini-2.5-flash`, `gemini-1.5-flash`, `gemini-1.5-pro`) dengan penyimpanan kunci terenkripsi Android Keystore dan sistem persetujuan perubahan Diff (Review Before/After).
- **Local Server (XAMPP)**: Embedded Web Server berjalan di port `8080`, melayani file statis dan skrip PHP, dapat diakses dari laptop/HP lain melalui jaringan Wi-Fi lokal, serta SQLite Database Manager.
- **Live Preview Internal**: Browser WebView internal terintegrasi dengan deteksi hot-reload dan console inspector (`console.log`, `console.error`).
- **Terminal Interaktif**: Konsol terminal bash mobile lengkap untuk menjalankan perintah file, dev server, dan Git.
- **GitHub Integration**: Manajemen token GitHub dan pemicu build APK langsung dari aplikasi.
