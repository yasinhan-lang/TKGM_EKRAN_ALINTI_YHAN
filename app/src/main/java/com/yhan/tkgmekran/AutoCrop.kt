package com.yhan.tkgmekran

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * TKGM Parsel Sorgu ekran görüntüsünde kesilecek alanı otomatik bulur.
 *
 *  HARİTA      : Durum çubuğu (saat/pil) ve alttaki mavi araç çubuğu + gezinme çubuğu atılır.
 *                Üstteki "TKGM Parsel Sorgu" başlığı ve harita kalır.
 *  ÖZNİTELİK   : Açık olan "Öznitelik Bilgisi" penceresinin mavi başlığından başlayıp
 *                yeşil "Favorilerde güncelle" düğmesinin hemen üstünde biter (yalnız tablo).
 */
object AutoCrop {

    enum class Mode { HARITA, OZNITELIK, TAM }

    data class Result(val mode: Mode, val rect: Rect)

    private fun r(c: Int) = Color.red(c)
    private fun g(c: Int) = Color.green(c)
    private fun b(c: Int) = Color.blue(c)

    /** Sol üstteki turuncu menü düğmesi (karartılmış hali dahil) */
    private fun isOrange(c: Int) = r(c) > 100 && r(c) - g(c) > 50 && r(c) - b(c) > 70

    /** Alt araç çubuğunun mavi / lacivert tonları (karartılmış hali dahil) */
    private fun isBarBlue(c: Int) = b(c) - r(c) > 40 && b(c) > 50

    /** Öznitelik penceresinin başlık mavisi (#337AB7) */
    private fun isHeaderBlue(c: Int) =
        abs(r(c) - 51) < 22 && abs(g(c) - 123) < 22 && abs(b(c) - 183) < 25

    /** "Favorilerde güncelle" yeşili (#5CB85C) */
    private fun isGreen(c: Int) = g(c) > 140 && g(c) - r(c) > 60 && g(c) - b(c) > 60

    fun detect(bmp: Bitmap, statusBarPx: Int, navBarPx: Int): Result {
        val w = bmp.width
        val h = bmp.height
        fun px(x: Int, y: Int) = bmp.getPixel(x.coerceIn(0, w - 1), y.coerceIn(0, h - 1))
        val xl = (w * 0.02f).toInt()
        val xr = (w * 0.98f).toInt()

        // 1) Uygulama başlığının üst kenarı (durum çubuğunun bittiği yer)
        var top = -1
        val step1 = max(2, (h * 0.01f).toInt())
        for (y in 0 until (h * 0.15f).toInt()) {
            if (isOrange(px(xl, y)) && isOrange(px(xl, y + step1))) { top = y; break }
        }
        if (top < 0) top = statusBarPx.coerceIn(0, h / 5)

        // 2) Öznitelik penceresi var mı? (yeşil düğme bandı)
        val gStep = max(3, (h * 0.012f).toInt())
        var greenTop = -1
        for (y in (h * 0.2f).toInt() until (h * 0.97f).toInt()) {
            if (isGreen(px((w * 0.3f).toInt(), y)) && isGreen(px(w / 2, y)) && isGreen(px((w * 0.7f).toInt(), y)) &&
                isGreen(px((w * 0.3f).toInt(), y + gStep)) && isGreen(px((w * 0.7f).toInt(), y + gStep))
            ) { greenTop = y; break }
        }
        if (greenTop > 0) {
            val yy = greenTop + max(2, (h * 0.004f).toInt())
            var left = w / 2
            while (left > 0 && isGreen(px(left - 1, yy))) left--
            var right = w / 2
            while (right < w - 1 && isGreen(px(right + 1, yy))) right++
            val sx = (left + (right - left) * 0.25f).toInt()
            val bStep = max(3, (h * 0.01f).toInt())
            var popupTop = -1
            for (y in top until greenTop) {
                if (isHeaderBlue(px(sx, y)) && isHeaderBlue(px(sx, y + bStep))) { popupTop = y; break }
            }
            if (popupTop >= 0 && right - left > w * 0.4f && greenTop - popupTop > h * 0.15f) {
                return Result(Mode.OZNITELIK, Rect(left, popupTop, right + 1, greenTop))
            }
        }

        // 3) Harita ekranı: alttaki mavi araç çubuğunun üst kenarını bul
        val minY = (h * 0.5f).toInt()
        var y = h - 1
        while (y > minY && !(isBarBlue(px(xl, y)) && isBarBlue(px(xr, y)))) y--
        val bottom: Int
        if (y > minY) {
            val barBottom = y
            while (y > minY && isBarBlue(px(xl, y - 1)) && isBarBlue(px(xr, y - 1))) y--
            // Çok ince bir şerit ise araç çubuğu değildir
            bottom = if (barBottom - y > h * 0.025f) y else h - navBarPx - (h * 0.06f).toInt()
        } else {
            bottom = h - navBarPx - (h * 0.06f).toInt()
        }
        val b = bottom.coerceIn(top + h / 4, h)
        return Result(Mode.HARITA, Rect(0, top, w, b))
    }

    fun crop(bmp: Bitmap, rect: Rect): Bitmap {
        val l = rect.left.coerceIn(0, bmp.width - 1)
        val t = rect.top.coerceIn(0, bmp.height - 1)
        val rr = min(rect.right, bmp.width)
        val bb = min(rect.bottom, bmp.height)
        return Bitmap.createBitmap(bmp, l, t, max(1, rr - l), max(1, bb - t))
    }
}
