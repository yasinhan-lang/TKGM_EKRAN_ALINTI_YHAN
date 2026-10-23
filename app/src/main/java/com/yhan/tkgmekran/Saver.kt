package com.yhan.tkgmekran

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Ayarlar ve kayıt işlemleri */
object Prefs {
    private const val NAME = "yhan_ayarlar"
    const val K_PREVIEW = "her_seferinde_onizleme"
    const val K_PNG = "png_kaydet"
    const val K_X = "dugme_x"
    const val K_Y = "dugme_y"

    fun get(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    fun preview(ctx: Context) = get(ctx).getBoolean(K_PREVIEW, false)
    fun png(ctx: Context) = get(ctx).getBoolean(K_PNG, false)
}

object Saver {
    const val FOLDER = "TKGM Ekran Alinti"

    fun timeStamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    /** Dosya adında kullanılamayan karakterleri temizler */
    fun clean(s: String): String =
        s.trim().replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_").trim('_')

    fun save(ctx: Context, bmp: Bitmap, baseName: String): Uri? {
        val png = Prefs.png(ctx)
        val ext = if (png) "png" else "jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$baseName.$ext")
            put(MediaStore.Images.Media.MIME_TYPE, if (png) "image/png" else "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + FOLDER)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = ctx.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { out ->
                if (png) bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                else bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    fun displayName(ctx: Context, baseName: String) =
        "$baseName." + (if (Prefs.png(ctx)) "png" else "jpg")
}
