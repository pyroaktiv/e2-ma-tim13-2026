package rs.tim13.slagalica.core.util

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

/** Generisanje QR koda iz teksta (spec 2.a.viii — QR za poziv prijatelja). */
object QrCodes {
    fun encode(content: String, sizePx: Int = 512): Bitmap? = runCatching {
        BarcodeEncoder().encodeBitmap(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
    }.getOrNull()
}
