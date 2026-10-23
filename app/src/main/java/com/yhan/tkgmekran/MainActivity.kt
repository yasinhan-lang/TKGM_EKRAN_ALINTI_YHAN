package com.yhan.tkgmekran

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private val navy = Color.rgb(11, 35, 64)
    private val blue = Color.rgb(51, 123, 182)
    private val orange = Color.rgb(255, 117, 54)
    private val green = Color.rgb(92, 184, 92)

    private lateinit var status: TextView
    private lateinit var serviceBtn: Button

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = navy
        val kaushan = Fonts.kaushan(this)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(20))
        }

        // ---------- Başlık paneli: logo | ad | bayrak (simetrik)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = GradientDrawable().apply {
                setColor(navy); cornerRadius = dp(14).toFloat(); setStroke(dp(1), Color.WHITE)
            }
        }
        header.addView(LogoView(this), LinearLayout.LayoutParams(dp(68), dp(68)))
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        titles.addView(text("TKGM EKRAN ALINTI", 19f, Color.WHITE, bold = true, center = true))
        titles.addView(text("Parsel Sorgu ekranını otomatik keser", 12f, Color.rgb(190, 205, 225), center = true))
        titles.addView(text("Yasin HAN", 20f, Color.WHITE, center = true).apply { typeface = kaushan })
        header.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(WavingFlagView(this), LinearLayout.LayoutParams(dp(68), dp(68)))
        col.addView(header)

        // ---------- Hizmet durumu
        val card1 = card()
        status = text("", 16f, Color.BLACK, bold = true, center = true)
        card1.addView(status)
        serviceBtn = button("1) ERİŞİLEBİLİRLİK HİZMETİNİ AÇ", blue) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        card1.addView(serviceBtn)
        card1.addView(text(
            "Ayarlar'da \"Yüklü uygulamalar / İndirilen uygulamalar\" altında " +
                "\"TKGM Ekran Alıntı - YHAN\"ı bulup açın.\n\n" +
                "\"Kısıtlanmış ayar\" uyarısı çıkarsa: aşağıdaki düğmeyle Uygulama Bilgisi'ni açın, " +
                "sağ üstteki ⋮ menüsünden \"Kısıtlanmış ayarlara izin ver\"i seçin, sonra tekrar deneyin.",
            13f, Color.DKGRAY))
        card1.addView(button("UYGULAMA BİLGİSİ (Kısıtlı ayar izni)", Color.rgb(90, 98, 110)) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        })
        col.addView(card1)

        // ---------- TKGM'yi aç
        val card2 = card()
        card2.addView(button("2) TKGM PARSEL SORGU'YU AÇ", orange) { openTkgm() })
        card2.addView(text(
            "TKGM Parsel Sorgu açıldığında ekranın kenarında yuvarlak bir yakalama düğmesi belirir. " +
                "Başka uygulamalarda düğme görünmez.", 13f, Color.DKGRAY))
        col.addView(card2)

        // ---------- Ayarlar
        val card3 = card()
        card3.addView(text("AYARLAR", 15f, navy, bold = true))
        card3.addView(switch("Her çekimde önizleme / elle düzeltme ekranı aç", Prefs.K_PREVIEW))
        card3.addView(switch("PNG olarak kaydet (kayıpsız, dosya daha büyük)", Prefs.K_PNG))
        col.addView(card3)

        // ---------- Kullanım
        val card4 = card()
        card4.addView(text("NASIL KULLANILIR?", 15f, navy, bold = true))
        card4.addView(text(
            "• Düğmeye DOKUNUN → ekran alınır, otomatik kesilir ve kaydedilir.\n" +
                "   – Harita ekranı: saat/pil satırı ve alttaki mavi menü atılır.\n" +
                "   – Öznitelik Bilgisi penceresi: yalnızca başlık ve tablo kalır, " +
                "yeşil \"Favorilerde güncelle\" ve arka plan atılır.\n" +
                "• Düğmeye BASILI TUTUN → önizleme açılır, kenarları parmakla düzeltip KAYDET veya PAYLAŞ.\n" +
                "• Düğmeyi sürükleyerek istediğiniz yere taşıyabilirsiniz.\n\n" +
                "Kayıt klasörü: Resimler / ${Saver.FOLDER}\n" +
                "Dosya adları: HARITA_tarih_saat • ADA_PARSEL_OZNITELIK_tarih_saat",
            13.5f, Color.rgb(40, 40, 40)))
        col.addView(card4)

        // ---------- Alt bilgi (ortalı)
        col.addView(text("Tasarım ve Geliştirme", 12f, Color.GRAY, center = true).apply { setPadding(0, dp(18), 0, 0) })
        col.addView(text("Yasin HAN", 22f, navy, center = true).apply { typeface = kaushan })
        col.addView(text("0 532 234 18 06 • yasinhan@gmail.com", 12f, Color.GRAY, center = true))
        col.addView(text("Sürüm 1.0", 11f, Color.LTGRAY, center = true))

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.rgb(238, 242, 247))
            addView(col)
        })
    }

    override fun onResume() {
        super.onResume()
        val on = isServiceEnabled()
        status.text = if (on) "● HİZMET AÇIK — kullanıma hazır" else "● HİZMET KAPALI"
        status.setTextColor(if (on) Color.rgb(46, 140, 46) else Color.rgb(200, 50, 50))
        serviceBtn.text = if (on) "Erişilebilirlik ayarları" else "1) ERİŞİLEBİLİRLİK HİZMETİNİ AÇ"
    }

    private fun isServiceEnabled(): Boolean {
        val list = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return CaptureService.running
        val cn = ComponentName(this, CaptureService::class.java)
        return list.split(':').any {
            it.equals(cn.flattenToString(), true) || it.equals(cn.flattenToShortString(), true)
        }
    }

    private fun openTkgm() {
        val launch = packageManager.getLaunchIntentForPackage(CaptureService.TKGM_PKG)
        if (launch != null) { startActivity(launch); return }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${CaptureService.TKGM_PKG}")))
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=${CaptureService.TKGM_PKG}")))
        }
    }

    // ---------------------------------------------------------------- yardımcılar

    private fun text(s: String, size: Float, color: Int, bold: Boolean = false, center: Boolean = false) =
        TextView(this).apply {
            text = s; textSize = size; setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            if (center) gravity = Gravity.CENTER
            setPadding(0, dp(3), 0, dp(3))
        }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = GradientDrawable().apply {
            setColor(Color.WHITE); cornerRadius = dp(12).toFloat(); setStroke(dp(1), Color.rgb(210, 218, 228))
        }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
    }

    private fun button(s: String, color: Int, onClick: () -> Unit) = Button(this).apply {
        text = s; setTextColor(Color.WHITE); textSize = 14f; isAllCaps = false
        setTypeface(typeface, Typeface.BOLD)
        background = GradientDrawable().apply { setColor(color); cornerRadius = dp(10).toFloat() }
        layoutParams = LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(8); bottomMargin = dp(4) }
        setOnClickListener { onClick() }
    }

    @Suppress("DEPRECATION")
    private fun switch(label: String, key: String) = Switch(this).apply {
        text = label; textSize = 14f; setTextColor(Color.rgb(40, 40, 40))
        setPadding(0, dp(8), 0, dp(8))
        isChecked = Prefs.get(this@MainActivity).getBoolean(key, false)
        setOnCheckedChangeListener { _, v -> Prefs.get(this@MainActivity).edit().putBoolean(key, v).apply() }
        if (Build.VERSION.SDK_INT >= 23) textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }
}
