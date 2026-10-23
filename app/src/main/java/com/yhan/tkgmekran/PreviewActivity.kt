package com.yhan.tkgmekran

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Basılı tutarak çekimde açılan önizleme: otomatik kesimi gösterir, kenarlar parmakla düzeltilebilir. */
class PreviewActivity : Activity() {

    companion object {
        const val EX_RECT = "rect"
        const val EX_NAME = "name"
        const val EX_MODE = "mode"
    }

    private var full: Bitmap? = null
    private lateinit var crop: CropView
    private lateinit var info: TextView
    private var autoRect = Rect()
    private var baseName = "TKGM"

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(11, 35, 64)
        window.navigationBarColor = Color.rgb(11, 35, 64)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(20, 24, 30))
        }
        info = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), dp(10))
            setBackgroundColor(Color.rgb(11, 35, 64))
        }
        crop = CropView(this)
        root.addView(info, LinearLayout.LayoutParams(-1, -2))
        root.addView(crop, LinearLayout.LayoutParams(-1, 0, 1f))

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(6), dp(6), dp(6), 0) }
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(6), dp(4), dp(6), dp(8)) }
        row1.addView(btn("OTOMATİK", Color.rgb(51, 123, 182)) { crop.setCrop(autoRect) })
        row1.addView(btn("TAM EKRAN", Color.rgb(90, 98, 110)) { full?.let { crop.setCrop(Rect(0, 0, it.width, it.height)) } })
        row2.addView(btn("İPTAL", Color.rgb(170, 60, 60)) { finish() })
        row2.addView(btn("PAYLAŞ", Color.rgb(255, 117, 54)) { saveAndShare(true) })
        row2.addView(btn("KAYDET", Color.rgb(92, 184, 92)) { saveAndShare(false) })
        root.addView(row1, LinearLayout.LayoutParams(-1, -2))
        root.addView(row2, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)
        load(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        load(intent)
    }

    private fun load(i: Intent) {
        val f = File(cacheDir, "son_ekran.png")
        val bmp = BitmapFactory.decodeFile(f.absolutePath)
        if (bmp == null) { Toast.makeText(this, "Görüntü bulunamadı", Toast.LENGTH_SHORT).show(); finish(); return }
        full = bmp
        val r = i.getIntArrayExtra(EX_RECT)
        autoRect = if (r != null && r.size == 4) Rect(r[0], r[1], r[2], r[3]) else Rect(0, 0, bmp.width, bmp.height)
        baseName = i.getStringExtra(EX_NAME) ?: ("TKGM_" + Saver.timeStamp())
        val mode = if (i.getStringExtra(EX_MODE) == AutoCrop.Mode.OZNITELIK.name) "Öznitelik penceresi" else "Harita ekranı"
        info.text = "$mode algılandı • Kenarları sürükleyerek düzeltebilirsiniz"
        crop.bitmap = bmp
        crop.setCrop(autoRect)
    }

    private fun btn(text: String, color: Int, onClick: () -> Unit) = Button(this).apply {
        this.text = text; setTextColor(Color.WHITE); textSize = 14f; isAllCaps = false
        background = GradientDrawable().apply { setColor(color); cornerRadius = dp(10).toFloat() }
        layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(4), 0, dp(4), 0) }
        setOnClickListener { onClick() }
    }

    private fun saveAndShare(share: Boolean) {
        val bmp = full ?: return
        val out = AutoCrop.crop(bmp, crop.cropRect())
        val uri = Saver.save(this, out, baseName)
        if (uri == null) { Toast.makeText(this, "Kaydedilemedi!", Toast.LENGTH_SHORT).show(); return }
        Toast.makeText(this, "✔ Kaydedildi: ${Saver.displayName(this, baseName)}", Toast.LENGTH_SHORT).show()
        if (share) {
            val s = Intent(Intent.ACTION_SEND).apply {
                type = if (Prefs.png(this@PreviewActivity)) "image/png" else "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(s, "Paylaş"))
        }
        finish()
    }

    /** Görüntü üzerinde sürüklenebilir kesim çerçevesi */
    class CropView(ctx: Context) : View(ctx) {
        var bitmap: Bitmap? = null
            set(v) { field = v; invalidate() }
        private val crop = RectF()          // görüntü koordinatlarında
        private var scale = 1f
        private var offX = 0f
        private var offY = 0f
        private val dens = ctx.resources.displayMetrics.density
        private val dim = Paint().apply { color = Color.argb(160, 0, 0, 0) }
        private val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = Color.rgb(255, 200, 0); strokeWidth = 2.5f * dens
        }
        private val handle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 200, 0) }
        private val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG)

        private var dragL = false; private var dragT = false; private var dragR = false; private var dragB = false
        private var moveAll = false
        private var lastX = 0f; private var lastY = 0f

        fun setCrop(r: Rect) { crop.set(r); clamp(); invalidate() }
        fun cropRect() = Rect(crop.left.toInt(), crop.top.toInt(), crop.right.toInt(), crop.bottom.toInt())

        private fun layoutCalc() {
            val b = bitmap ?: return
            scale = min(width / b.width.toFloat(), height / b.height.toFloat())
            offX = (width - b.width * scale) / 2f
            offY = (height - b.height * scale) / 2f
        }

        private fun clamp() {
            val b = bitmap ?: return
            val minSize = 40f
            crop.left = crop.left.coerceIn(0f, b.width - minSize)
            crop.top = crop.top.coerceIn(0f, b.height - minSize)
            crop.right = crop.right.coerceIn(crop.left + minSize, b.width.toFloat())
            crop.bottom = crop.bottom.coerceIn(crop.top + minSize, b.height.toFloat())
        }

        override fun onDraw(c: Canvas) {
            val b = bitmap ?: return
            layoutCalc()
            val dst = RectF(offX, offY, offX + b.width * scale, offY + b.height * scale)
            c.drawBitmap(b, null, dst, bmpPaint)
            val l = offX + crop.left * scale; val t = offY + crop.top * scale
            val r = offX + crop.right * scale; val bt = offY + crop.bottom * scale
            c.drawRect(dst.left, dst.top, dst.right, t, dim)
            c.drawRect(dst.left, bt, dst.right, dst.bottom, dim)
            c.drawRect(dst.left, t, l, bt, dim)
            c.drawRect(r, t, dst.right, bt, dim)
            c.drawRect(l, t, r, bt, frame)
            val hr = 7f * dens
            for (x in floatArrayOf(l, (l + r) / 2, r)) for (y in floatArrayOf(t, (t + bt) / 2, bt)) {
                if (x == (l + r) / 2 && y == (t + bt) / 2) continue
                c.drawCircle(x, y, hr, handle)
            }
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            val b = bitmap ?: return false
            layoutCalc()
            val l = offX + crop.left * scale; val t = offY + crop.top * scale
            val r = offX + crop.right * scale; val bt = offY + crop.bottom * scale
            val tol = 28f * dens
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val inY = e.y > t - tol && e.y < bt + tol
                    val inX = e.x > l - tol && e.x < r + tol
                    dragL = inY && abs(e.x - l) < tol
                    dragR = inY && abs(e.x - r) < tol && !dragL
                    dragT = inX && abs(e.y - t) < tol
                    dragB = inX && abs(e.y - bt) < tol && !dragT
                    moveAll = !(dragL || dragR || dragT || dragB) && e.x in l..r && e.y in t..bt
                    lastX = e.x; lastY = e.y
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.x - lastX) / scale; val dy = (e.y - lastY) / scale
                    lastX = e.x; lastY = e.y
                    if (moveAll) {
                        val ddx = dx.coerceIn(-crop.left, b.width - crop.right)
                        val ddy = dy.coerceIn(-crop.top, b.height - crop.bottom)
                        crop.offset(ddx, ddy)
                    } else {
                        if (dragL) crop.left = max(0f, min(crop.left + dx, crop.right - 40f))
                        if (dragR) crop.right = min(b.width.toFloat(), max(crop.right + dx, crop.left + 40f))
                        if (dragT) crop.top = max(0f, min(crop.top + dy, crop.bottom - 40f))
                        if (dragB) crop.bottom = min(b.height.toFloat(), max(crop.bottom + dy, crop.top + 40f))
                    }
                    invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragL = false; dragR = false; dragT = false; dragB = false; moveAll = false
                }
            }
            return true
        }
    }
}
