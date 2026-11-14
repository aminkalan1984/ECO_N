package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AiRepository
import com.example.data.BillExtractionUiState
import com.example.domain.BillChecks
import com.example.domain.BillIssue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillAiViewModel(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BillExtractionUiState>(BillExtractionUiState.Idle)
    val uiState: StateFlow<BillExtractionUiState> = _uiState.asStateFlow()

    private val _mergedIssues = MutableStateFlow<List<BillIssue>?>(null)
    val mergedIssues: StateFlow<List<BillIssue>?> = _mergedIssues.asStateFlow()

    private val _selectedPhotoUri = MutableStateFlow<Uri?>(null)
    val selectedPhotoUri: StateFlow<Uri?> = _selectedPhotoUri.asStateFlow()

    fun analyzeBillPhoto(context: Context, uri: Uri) {
        _selectedPhotoUri.value = uri
        _uiState.value = BillExtractionUiState.Loading
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                decodeScaledBitmap(context, uri)
            }
            if (bitmap == null) {
                _uiState.value = BillExtractionUiState.Error("فایل تصویر قابل خواندن نبود.")
                return@launch
            }
            val result = aiRepository.extractBillFromBitmap(bitmap)
            result.onSuccess { extracted ->
                val merged = BillChecks.mergeIssues(extracted)
                _mergedIssues.value = merged
                _uiState.value = BillExtractionUiState.Success(extracted)
            }.onFailure { err ->
                _uiState.value = BillExtractionUiState.Error(
                    err.message ?: "خواندن قبض با هوش مصنوعی ناموفق بود."
                )
            }
        }
    }

    fun clearError() {
        if (_uiState.value is BillExtractionUiState.Error) {
            _uiState.value = BillExtractionUiState.Idle
        }
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val maxSide = maxOf(info.size.width, info.size.height)
                if (maxSide > 1400) {
                    val sample = (maxSide / 1400).coerceAtLeast(1)
                    decoder.setTargetSampleSize(sample)
                }
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }
    }.getOrNull()

    companion object {
        fun factory(aiRepository: AiRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    BillAiViewModel(aiRepository) as T
            }
    }
}
