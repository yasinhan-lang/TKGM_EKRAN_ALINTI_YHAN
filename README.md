# TKGM EKRAN ALINTI – YHAN

An Android screenshot tool that works **only inside the TKGM Parsel Sorgu app** (`com.tkgm.parselsorgu`).
When the app is open, a round capture button appears at the edge of the screen. Each capture is cropped automatically:

| Screen | What gets cropped away |
|---|---|
| Map view | The status bar at the top (clock, battery) and the blue menu + navigation bar at the bottom |
| Öznitelik Bilgisi window | Everything except the blue header and the table (the green "Favorilerde güncelle" button and the background) |

* **Tap** → capture, crop automatically, save
* **Press and hold** → open the preview and adjust the crop edges with your finger, then KAYDET or PAYLAŞ
* **Drag** → move the button anywhere on the screen

Save folder: `Resimler/TKGM Ekran Alinti`
File names: `HARITA_20261024_013000.jpg`, `38700_94_OZNITELIK_20261024_013015.jpg`
(Ada/Parsel is read automatically from the open window.)

## Building the APK (GitHub Actions)
1. Create a new repository on GitHub (e.g. `TKGM_EKRAN_ALINTI_YHAN`).
2. Upload all files from this folder (including the `.github` folder).
3. Once the **Actions → APK Derle** workflow finishes, download the APK from the **Artifacts** section of the run summary.

## Installation
1. Install the APK and open it.
2. Tap **1) ERİŞİLEBİLİRLİK HİZMETİNİ AÇ** → find "TKGM Ekran Alıntı - YHAN" under *Downloaded / Installed apps* and turn it on.
   - If you see a "Restricted setting" warning (Android 13+): tap the **UYGULAMA BİLGİSİ** button → ⋮ in the top right → *Allow restricted settings* → try again.
   - On Xiaomi phones, also set *Battery saver → No restrictions* for the app so the service isn't shut down.
3. Open TKGM Parsel Sorgu; the button appears.

Requirement: Android 11 or newer.

Design and Development: **Yasin HAN** • 0 532 234 18 06 • yasinhan@gmail.com
