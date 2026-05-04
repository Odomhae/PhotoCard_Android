package com.odom.photocard.ui.theme

import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.odom.photocard.R

object AppFonts {

    val provider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )

    data class FontOption(val name: String, val fontFamily: FontFamily)

    val options: List<FontOption> = listOf(
        FontOption("Default", FontFamily.Default),
        FontOption("Roboto", googleFamily("Roboto")),
        FontOption("Montserrat", googleFamily("Montserrat")),
        FontOption("Pacifico", googleFamily("Pacifico")),
        FontOption("Dancing Script", googleFamily("Dancing Script")),
        FontOption("Playfair Display", googleFamily("Playfair Display")),
        FontOption("Oswald", googleFamily("Oswald")),
        FontOption("Raleway", googleFamily("Raleway")),
        FontOption("Lobster", googleFamily("Lobster")),
        FontOption("Ubuntu", googleFamily("Ubuntu")),
    )

    private fun googleFamily(name: String): FontFamily =
        FontFamily(Font(googleFont = GoogleFont(name), fontProvider = provider))

    fun getByName(name: String): FontFamily =
        options.find { it.name == name }?.fontFamily ?: FontFamily.Default

    fun getBitmapTypeface(fontName: String, fontWeight: FontWeight): Typeface {
        val base = when (fontName) {
            "Roboto" -> Typeface.create("sans-serif", Typeface.NORMAL)
            "Montserrat" -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
            "Oswald" -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
            "Raleway" -> Typeface.create("sans-serif-light", Typeface.NORMAL)
            "Ubuntu" -> Typeface.create("sans-serif", Typeface.NORMAL)
            "Pacifico", "Dancing Script", "Lobster" -> Typeface.create("cursive", Typeface.NORMAL)
            "Playfair Display" -> Typeface.create("serif", Typeface.NORMAL)
            else -> Typeface.DEFAULT
        }
        return when (fontWeight) {
            FontWeight.Bold, FontWeight.ExtraBold, FontWeight.Black ->
                Typeface.create(base, Typeface.BOLD)
            else -> base
        }
    }
}
