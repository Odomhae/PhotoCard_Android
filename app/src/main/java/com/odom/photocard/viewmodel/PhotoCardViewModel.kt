package com.odom.photocard.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Data class representing a text overlay on the image
 */
data class TextOverlay(
    val id: String = System.currentTimeMillis().toString(),
    val text: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val fontSize: TextUnit = 24.sp,
    val color: Color = Color.White,
    val fontWeight: FontWeight = FontWeight.Normal,
    val rotation: Float = 0f,
    val fontFamily: String = "Default"
)

/**
 * State for the photo card editing
 */
data class PhotoCardState(
    val imageUri: Uri? = null,
    val bitmap: Bitmap? = null,
    val textOverlays: List<TextOverlay> = emptyList(),
    val selectedTextId: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

/**
 * ViewModel for managing photo card creation state
 */
class PhotoCardViewModel : ViewModel() {

    private val _state = MutableStateFlow(PhotoCardState())
    val state: StateFlow<PhotoCardState> = _state.asStateFlow()

    /**
     * Set the selected image from camera or gallery
     */
    fun setImage(uri: Uri, context: Context? = null) {
        _state.value = _state.value.copy(
            imageUri = uri,
            textOverlays = emptyList(),
            selectedTextId = null
        )
        // Load bitmap if context is provided
        context?.let {
            viewModelScope.launch {
                val bitmap = loadBitmapFromUri(it, uri)
                bitmap?.let { bmp ->
                    _state.value = _state.value.copy(bitmap = bmp)
                }
            }
        }
    }

    /**
     * Set the bitmap for processing
     */
    fun setBitmap(bitmap: Bitmap) {
        _state.value = _state.value.copy(bitmap = bitmap)
    }

    /**
     * Load bitmap from URI
     */
    private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Add a new text overlay
     */
    fun addTextOverlay(text: String = "Enter text") {
        val newOverlay = TextOverlay(
            text = text,
            x = 100f,
            y = 100f
        )
        _state.value = _state.value.copy(
            textOverlays = _state.value.textOverlays + newOverlay,
            selectedTextId = newOverlay.id
        )
    }

    /**
     * Update an existing text overlay
     */
    fun updateTextOverlay(overlay: TextOverlay) {
        val updatedList = _state.value.textOverlays.map {
            if (it.id == overlay.id) overlay else it
        }
        _state.value = _state.value.copy(textOverlays = updatedList)
    }

    /**
     * Update text position
     */
    fun updateTextPosition(id: String, x: Float, y: Float) {
        val updatedList = _state.value.textOverlays.map {
            if (it.id == id) it.copy(x = x, y = y) else it
        }
        _state.value = _state.value.copy(textOverlays = updatedList)
    }

    /**
     * Update text font size
     */
    fun updateTextSize(id: String, size: Float) {
        val updatedList = _state.value.textOverlays.map {
            if (it.id == id) it.copy(fontSize = size.sp) else it
        }
        _state.value = _state.value.copy(textOverlays = updatedList)
    }

    /**
     * Update text rotation
     */
    fun updateTextRotation(id: String, rotation: Float) {
        val updatedList = _state.value.textOverlays.map {
            if (it.id == id) it.copy(rotation = rotation) else it
        }
        _state.value = _state.value.copy(textOverlays = updatedList)
    }

    /**
     * Delete a text overlay
     */
    fun deleteTextOverlay(id: String) {
        val updatedList = _state.value.textOverlays.filter { it.id != id }
        _state.value = _state.value.copy(
            textOverlays = updatedList,
            selectedTextId = null
        )
    }

    /**
     * Select a text overlay for editing
     */
    fun selectTextOverlay(id: String?) {
        _state.value = _state.value.copy(selectedTextId = id)
    }

    /**
     * Clear all state
     */
    fun clearState() {
        _state.value = PhotoCardState()
    }

    /**
     * Set saving state
     */
    fun setSavingState(isSaving: Boolean) {
        _state.value = _state.value.copy(isSaving = isSaving)
    }

    /**
     * Mark save as successful
     */
    fun markSaveSuccess() {
        _state.value = _state.value.copy(
            isSaving = false,
            saveSuccess = true
        )
    }

    /**
     * Create solid color bitmap
     */
    fun createSolidColorBitmap(colorInt: Int) {
        val width = 1080
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(colorInt)
        _state.value = _state.value.copy(bitmap = bitmap)
    }

    /**
     * Load sample image from URL
     */
    fun loadSampleImage(imageUrl: String) {
        viewModelScope.launch {
            try {
                val bitmap = loadBitmapFromUrl(imageUrl)
                bitmap?.let {
                    _state.value = _state.value.copy(bitmap = it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Add a sample quote as text overlay
     */
    fun addSampleQuote() {
        val randomQuote = sampleQuotes.random()
        val newOverlay = TextOverlay(
            text = randomQuote,
            x = 100f,
            y = 400f,
            fontSize = 28.sp,
            color = androidx.compose.ui.graphics.Color.White
        )
        _state.value = _state.value.copy(
            textOverlays = _state.value.textOverlays + newOverlay,
            selectedTextId = newOverlay.id
        )
    }

    /**
     * Load bitmap from URL
     */
    private suspend fun loadBitmapFromUrl(urlString: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val url = java.net.URL(urlString)
                url.openStream().use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    companion object {
        // Sample quotes by category
        val sampleQuotes = listOf(
            // Motivational
            "Believe you can and you're halfway there.",
            "The future belongs to those who believe in the beauty of their dreams.",
            "Don't watch the clock; do what it does. Keep going.",
            "Success is not final, failure is not fatal: it is the courage to continue that counts.",
            "Your limitation—it's only your imagination.",
            "Push yourself, because no one else is going to do it for you.",
            "Great things never come from comfort zones.",
            "Dream it. Wish it. Do it.",
            "Success doesn't just find you. You have to go out and get it.",
            "The harder you work for something, the greater you'll feel when you achieve it.",
            // Love & Friendship
            "The best thing to hold onto in life is each other.",
            "Love is composed of a single soul inhabiting two bodies.",
            "A friend is someone who knows all about you and still loves you.",
            "True love stories never have endings.",
            "Friendship is born at that moment when one person says to another: 'What! You too?'",
            "The greatest happiness of life is the conviction that we are loved.",
            "A real friend is one who walks in when the rest of the world walks out.",
            "Love is not about how many days, months, or years you have been together.",
            "Good friends are like stars. You don't always see them, but you know they're always there.",
            "Love is when the other person's happiness is more important than your own.",
            // Life & Wisdom
            "Life is what happens when you're busy making other plans.",
            "The purpose of our lives is to be happy.",
            "Life is really simple, but we insist on making it complicated.",
            "In the middle of every difficulty lies opportunity.",
            "The only impossible journey is the one you never begin.",
            "Life isn't about finding yourself. It's about creating yourself.",
            "The way to get started is to quit talking and begin doing.",
            "You only live once, but if you do it right, once is enough.",
            "Life is 10% what happens to us and 90% how we react to it.",
            "The best time to plant a tree was 20 years ago. The second best time is now.",
            // Funny & Light
            "I'm not lazy, I'm on energy-saving mode.",
            "Life is short. Smile while you still have teeth.",
            "I followed my heart, and it led me to the fridge.",
            "My bed is a magical place where I suddenly remember everything I forgot to do.",
            "I'm not arguing, I'm just explaining why I'm right.",
            "Common sense is like deodorant. The people who need it most never use it.",
            "Life is like a camera. Focus on what's important, capture the good times.",
            "I'm on a seafood diet. I see food and I eat it.",
            "Exercise? I thought you said extra fries.",
            "I'm not weird, I'm limited edition."
        )

        // Sample image URL from picsum.photos
        const val SAMPLE_IMAGE_URL = "https://picsum.photos/1080/1080"
    }
}
