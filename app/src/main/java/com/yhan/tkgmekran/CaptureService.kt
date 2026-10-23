package com.yhan.tkgmekran

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

/**
 * Erişilebilirlik hizmeti:
 *  - Yalnızca TKGM Parsel Sorgu ön plandayken yüzen yakalama düğmesini gösterir.
 *  - Dokun  : ekranı yakala → otomatik kes → kaydet
 *  - Basılı tut : ekranı yakala → önizleme/elle düzeltme ekranını aç
 */
class CaptureService : AccessibilityService() {

    companion object {
        const val TKGM_PKG = "com.tkgm.parselsorgu"
        @Volatile var running = false

        fun isTkgm(pkg: String?) =
            pkg != null && (pkg == TKGM_PKG || pkg.contains("tkgm", ignoreCase = true))
    }

    private lateinit var wm: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var bubble: BubbleView? = null
    private var lp: WindowManager.LayoutParams? = null
    private var busy = false
    private var imePkg: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        running = true
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        imePkg = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')
        // Hizmet açıldığında TKGM zaten öndeyse düğmeyi göster
        if (isTkgm(rootInActiveWindow?.packageName?.toString())) showBubble()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) { hideBubble(); return }   // kendi ekranlarımızda gizle
        if (pkg == "com.android.systemui" || pkg == imePkg) return
        if (isTkgm(pkg)) showBubble() else hideBubble()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        running = false
        hideBubble()
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        running = false
        hideBubble()
        return super.onUnbind(intent)
    }

    // ---------------------------------------------------------------- Düğme

    private fun dp(v: Float) = v * resources.displayMetrics.density

    @SuppressLint("ClickableViewAccessibility")
    private fun showBubble() {
        if (bubble != null) return
        val size = dp(58f).toInt()
        val p = Prefs.get(this)
        val dm = resources.displayMetrics
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = p.getInt(Prefs.K_X, dm.widthPixels - size - dp(8f).toInt())
            y = p.getInt(Prefs.K_Y, (dm.heightPixels * 0.62f).toInt())
        }
        val v = BubbleView(this)
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0
        var dragging = false; var longDone = false
        val longPress = Runnable {
            longDone = true
            v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            capture(forcePreview = true)
        }
        v.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY; startX = params.x; startY = params.y
                    dragging = false; longDone = false
                    v.basili = true; v.invalidate()
                    handler.postDelayed(longPress, 600)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX; val dy = e.rawY - downY
                    if (!dragging && (abs(dx) > slop || abs(dy) > slop)) {
                        dragging = true; handler.removeCallbacks(longPress)
                    }
                    if (dragging) {
                        params.x = (startX + dx).toInt().coerceIn(0, dm.widthPixels - size)
                        params.y = (startY + dy).toInt().coerceIn(0, dm.heightPixels - size)
                        try { wm.updateViewLayout(v, params) } catch (_: Exception) {}
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPress)
                    v.basili = false; v.invalidate()
                    if (dragging) {
                        p.edit().putInt(Prefs.K_X, params.x).putInt(Prefs.K_Y, params.y).apply()
                    } else if (!longDone && e.actionMasked == MotionEvent.ACTION_UP) {
                        capture(forcePreview = Prefs.preview(this))
                    }
                }
            }
            true
        }
        try {
            wm.addView(v, params)
            bubble = v; lp = params
        } catch (_: Exception) { }
    }

    private fun hideBubble() {
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubble = null
    }

    // ---------------------------------------------------------------- Yakalama

    private fun toast(msg: String) = handler.post {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
    }

    private fun dimen(name: String): Int {
        val id = resources.getIdentifier(name, "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }

    private fun capture(forcePreview: Boolean) {
        if (busy) return
        busy = true
        bubble?.visibility = View.INVISIBLE
        // Düğmenin görüntüde çıkmaması için kısa bekleme
        handler.postDelayed({
            takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                override fun onSuccess(result: ScreenshotResult) {
                    val hb = result.hardwareBuffer
                    val hw = Bitmap.wrapHardwareBuffer(hb, result.colorSpace)
                    val bmp = hw?.copy(Bitmap.Config.ARGB_8888, false)
                    hw?.recycle(); hb.close()
                    bubble?.visibility = View.VISIBLE
                    if (bmp == null) { busy = false; toast("Görüntü alınamadı"); return }
                    val parcel = readParcelInfo()
                    process(bmp, parcel, forcePreview)
                }

                override fun onFailure(errorCode: Int) {
                    bubble?.visibility = View.VISIBLE
                    busy = false
                    toast(if (errorCode == ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT)
                        "Çok hızlı! 1 saniye bekleyip tekrar deneyin."
                    else "Ekran görüntüsü alınamadı (kod $errorCode)")
                }
            })
        }, 250)
    }

    private fun process(bmp: Bitmap, parcel: String?, forcePreview: Boolean) {
        val sb = dimen("status_bar_height")
        val nb = dimen("navigation_bar_height")
        Thread {
            try {
                val res = AutoCrop.detect(bmp, sb, nb)
                val base = when (res.mode) {
                    AutoCrop.Mode.OZNITELIK ->
                        (if (parcel != null) parcel + "_OZNITELIK_" else "OZNITELIK_") + Saver.timeStamp()
                    else -> "HARITA_" + Saver.timeStamp()
                }
                if (forcePreview) {
                    val f = File(cacheDir, "son_ekran.png")
                    FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    val i = Intent(this, PreviewActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra(PreviewActivity.EX_RECT, intArrayOf(res.rect.left, res.rect.top, res.rect.right, res.rect.bottom))
                        putExtra(PreviewActivity.EX_NAME, base)
                        putExtra(PreviewActivity.EX_MODE, res.mode.name)
                    }
                    handler.post { startActivity(i) }
                } else {
                    val out = AutoCrop.crop(bmp, res.rect)
                    val uri = Saver.save(this, out, base)
                    val kind = if (res.mode == AutoCrop.Mode.OZNITELIK) "Öznitelik" else "Harita"
                    toast(if (uri != null) "✔ $kind kaydedildi\n${Saver.displayName(this, base)}" else "Kaydedilemedi!")
                }
            } catch (e: Exception) {
                toast("Hata: ${e.message}")
            } finally {
                handler.post { busy = false }
            }
        }.start()
    }

    /** Öznitelik penceresi açıksa Ada / Parsel değerlerini okuyup dosya adı yapar: 38700_94 */
    private fun readParcelInfo(): String? {
        return try {
            val root = rootInActiveWindow ?: return null
            val texts = ArrayList<String>()
            collect(root, texts, 0)
            fun valueAfter(label: String): String? {
                val i = texts.indexOfFirst { it.trim().equals(label, ignoreCase = true) }
                if (i < 0) return null
                return texts.drop(i + 1).firstOrNull { it.isNotBlank() }?.trim()
            }
            val ada = valueAfter("Ada")
            val parsel = valueAfter("Parsel")
            if (ada != null && parsel != null && ada.any { it.isDigit() } && parsel.any { it.isDigit() })
                Saver.clean("${ada}_$parsel")
            else null
        } catch (_: Exception) { null }
    }

    private fun collect(n: AccessibilityNodeInfo?, out: MutableList<String>, depth: Int) {
        if (n == null || depth > 60 || out.size > 3000) return
        val t = n.text?.toString() ?: n.contentDescription?.toString()
        if (!t.isNullOrBlank()) out.add(t)
        for (i in 0 until n.childCount) collect(n.getChild(i), out, depth + 1)
    }

    // ---------------------------------------------------------------- Görünüm

    /** Lacivert daire içinde beyaz kırpma çerçevesi çizen yüzen düğme */
    private class BubbleView(ctx: Context) : View(ctx) {
        var basili = false
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = Color.WHITE; strokeCap = Paint.Cap.ROUND
        }
        private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 117, 54) }

        override fun onDraw(c: Canvas) {
            val w = width.toFloat(); val h = height.toFloat(); val r = w / 2f
            fill.color = if (basili) Color.rgb(255, 117, 54) else Color.argb(235, 11, 35, 64)
            c.drawCircle(r, r, r - 2f, fill)
            stroke.strokeWidth = w * 0.045f
            c.drawCircle(r, r, r - w * 0.05f, stroke)
            // Kırpma köşeleri
            stroke.strokeWidth = w * 0.06f
            val a = w * 0.30f; val b = w * 0.70f; val l = w * 0.12f
            c.drawLine(a, a, a + l, a, stroke); c.drawLine(a, a, a, a + l, stroke)
            c.drawLine(b, a, b - l, a, stroke); c.drawLine(b, a, b, a + l, stroke)
            c.drawLine(a, b, a + l, b, stroke); c.drawLine(a, b, a, b - l, stroke)
            c.drawLine(b, b, b - l, b, stroke); c.drawLine(b, b, b, b - l, stroke)
            if (!basili) c.drawCircle(r, r, w * 0.07f, dot)
            else { stroke.strokeWidth = w * 0.03f; c.drawOval(RectF(r - w * .08f, r - w * .08f, r + w * .08f, r + w * .08f), stroke) }
        }
    }
}
