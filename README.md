MOODY — PROYEK ANDROID AWAL

Status: source project, belum dibuild atau diuji di perangkat fisik dalam paket ini.
Yang diimplementasikan:
- UI Android bergaya gelap
- Pilih video dari galeri lewat Android document picker
- Preview/play-pause dan seek video
- Input detik mulai/selesai
- Ekspor potongan video MP4 menggunakan AndroidX Media3 Transformer
- Pilih lokasi simpan hasil ekspor melalui pemilih dokumen Android

Belum diimplementasikan:
- Timeline multi-klip / gabung beberapa video
- Teks/stiker di atas video
- Menambah musik terpisah
- Filter warna/transisi
- Undo/redo, autosave proyek, template, subtitle otomatis
- Login, cloud, atau pembayaran
Ini bukan clone CapCut; ini fondasi editor gratis untuk dikembangkan.

BUILD APK DARI HP DENGAN GITHUB ACTIONS
1. Unduh ZIP ini dan ekstrak.
2. Buat akun GitHub jika belum punya.
3. Buat repository baru bernama moody-android.
4. Unggah semua ISI folder moody_android ke repository (termasuk folder .github). Pastikan settings.gradle.kts berada di root repository.
5. Buka tab Actions di repository dan izinkan Actions bila diminta.
6. Pilih workflow "Build Moody APK", tekan Run workflow.
7. Tunggu workflow selesai. Buka hasil run, cari bagian Artifacts, unduh "moody-debug-apk".
8. Ekstrak ZIP artifact dan ambil app-debug.apk. Android mungkin meminta izin instalasi dari sumber tersebut; instal hanya APK yang kamu bangun dari repository milikmu sendiri.

CATATAN BUILD
- Build membutuhkan internet untuk mengunduh Android Gradle Plugin dan library Media3.
- Workflow ini menghasilkan debug APK untuk pengujian, bukan rilis Play Store.
- Build pertama dapat gagal jika ada perubahan dependency/toolchain. Baca log di tab Actions untuk memperbaikinya.
- Tidak ada APK jadi yang dilampirkan karena proyek ini belum dibangun dan diuji di lingkungan Android.

CARA MENGGUNAKAN
1. Instal APK hasil build.
2. Buka Moody dan tekan "Pilih video dari galeri".
3. Putar preview, geser timeline, masukkan waktu mulai dan selesai dalam detik.
4. Tekan "Ekspor potongan video".
5. Tunggu proses, lalu pilih lokasi simpan pada dialog Android.

BATASAN
- Performa dan codec bergantung pada perangkat dan file sumber.
- Video besar bisa membutuhkan ruang penyimpanan dan waktu pemrosesan.
- Pilih rentang waktu yang valid; durasi akhir harus lebih besar dari waktu mulai.
- Untuk hasil profesional, proyek ini perlu ditambah pengujian, penanganan orientasi/codec, progress ekspor, dan fitur editing berikutnya.
