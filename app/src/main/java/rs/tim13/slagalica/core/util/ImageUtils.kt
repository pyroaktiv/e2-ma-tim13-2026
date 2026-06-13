package rs.tim13.slagalica.core.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object ImageUtils {

    /** A small icon (emoji) for a league, by its name. */
    fun leagueIcon(name: String?): String = when (name?.lowercase()) {
        "bronze" -> "🥉"
        "silver" -> "🥈"
        "gold" -> "🥇"
        "platinum" -> "🛡️"
        "diamond" -> "💎"
        "master" -> "👑"
        else -> "🏅"
    }

    /** Renders [text] as a black-on-white QR code bitmap of [size]x[size] px. */
    fun generateQr(text: String, size: Int = 512): Bitmap {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

    private val AVATAR_COLORS = intArrayOf(
        0xFFE57373.toInt(), 0xFF64B5F6.toInt(), 0xFF81C784.toInt(), 0xFFFFB74D.toInt(),
        0xFFBA68C8.toInt(), 0xFF4DB6AC.toInt(), 0xFFA1887F.toInt(), 0xFF7986CB.toInt(),
        0xFFF06292.toInt(), 0xFF90A4AE.toInt(), 0xFFAED581.toInt(),
    )

    /**
     * Generates a deterministic circular avatar from an id like "avatar_03" or a
     * username: a colored disc with a short label. Used until real avatar assets
     * are added, so a changed avatar is visibly reflected.
     */
    fun generateAvatar(seed: String, size: Int = 256): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val color = AVATAR_COLORS[(seed.hashCode() and 0x7fffffff) % AVATAR_COLORS.size]

        val circle = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, circle)

        val label = avatarLabel(seed)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            textSize = size * 0.4f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val yOffset = (text.descent() + text.ascent()) / 2f
        canvas.drawText(label, size / 2f, size / 2f - yOffset, text)
        return bmp
    }

    private fun avatarLabel(seed: String): String {
        val digits = seed.filter { it.isDigit() }
        if (digits.isNotEmpty()) return digits.takeLast(2)
        return seed.take(1).uppercase().ifEmpty { "?" }
    }
}
