package com.yhan.tkgmekran

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object Fonts {
    private var kaushan: Typeface? = null
    fun kaushan(ctx: Context): Typeface {
        kaushan?.let { return it }
        val t = try { Typeface.createFromAsset(ctx.assets, "fonts/KaushanScript.ttf") }
        catch (_: Exception) { Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC) }
        kaushan = t
        return t
    }
}

/** YHAN logosu: beyaz daire + siyah halka + el yazısı "YH" */
class LogoView(ctx: Context) : View(ctx) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.BLACK }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; textAlign = Paint.Align.CENTER; typeface = Fonts.kaushan(ctx)
    }

    override fun onDraw(c: Canvas) {
        val s = min(width, height).toFloat()
        val cx = width / 2f; val cy = height / 2f; val r = s / 2f - 2f
        c.drawCircle(cx, cy, r, fill)
        ring.strokeWidth = s * 0.07f
        c.drawCircle(cx, cy, r - ring.strokeWidth / 2f, ring)
        text.textSize = s * 0.42f
        val fm = text.fontMetrics
        c.drawText("YH", cx, cy - (fm.ascent + fm.descent) / 2f, text)
    }
}

/** Dalgalanan Türk bayrağı */
class WavingFlagView(ctx: Context) : View(ctx) {
    private var flag: Bitmap? = null
    private var phase = 0f
    private val meshW = 24
    private val meshH = 8
    private val verts = FloatArray((meshW + 1) * (meshH + 1) * 2)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val pole = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(200, 200, 205) }

    private fun buildFlag(w: Int) {
        val g = w / 1.5f
        val h = g.toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.rgb(227, 10, 23))
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(227, 10, 23) }
        val cy = g / 2f
        c.drawCircle(0.5f * g, cy, 0.25f * g, white)          // hilal dış daire
        c.drawCircle(0.5625f * g, cy, 0.2f * g, red)          // hilal iç daire
        // Yıldız (bir ucu gönder tarafına bakar)
        val sx = (0.5625f + 0.2f + 1f / 30f + 0.125f) * g
        val ro = 0.125f * g
        val ri = ro * 0.382f
        val p = Path()
        for (i in 0 until 10) {
            val ang = PI + i * PI / 5
            val rr = if (i % 2 == 0) ro else ri
            val x = sx + (rr * cos(ang)).toFloat()
            val y = cy + (rr * sin(ang)).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        c.drawPath(p, white)
        flag = bmp
    }

    override fun onDraw(c: Canvas) {
        val poleW = width * 0.05f
        val fw = (width - poleW * 1.5f).toInt().coerceAtLeast(10)
        if (flag == null || flag!!.width != fw) buildFlag(fw)
        val f = flag ?: return
        val fh = f.height.toFloat()
        val top = (height - fh) / 2f
        c.drawRoundRect(0f, top - fh * 0.15f, poleW, height.toFloat(), poleW / 2, poleW / 2, pole)
        val amp = fh * 0.07f
        var k = 0
        for (j in 0..meshH) {
            val y0 = fh * j / meshH
            for (i in 0..meshW) {
                val fx = i / meshW.toFloat()
                val x0 = fw * fx
                val wave = sin(fx * 2.4f * PI.toFloat() - phase)
                verts[k++] = poleW + x0 - amp * 0.3f * fx * cos(fx * 2.4f * PI.toFloat() - phase)
                verts[k++] = top + y0 + amp * fx * wave
            }
        }
        c.drawBitmapMesh(f, meshW, meshH, verts, 0, null, 0, paint)
        phase += 0.12f
        if (isShown) postInvalidateOnAnimation()
    }
}
