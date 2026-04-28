package com.odom.photocard.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    fun setImage(uri: Uri) {
        _state.value = _state.value.copy(
            imageUri = uri,
            textOverlays = emptyList(),
            selectedTextId = null
        )
    }

    /**
     * Set the bitmap for processing
     */
    fun setBitmap(bitmap: Bitmap) {
        _state.value = _state.value.copy(bitmap = bitmap)
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
}
