package com.example.tomatocare

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tomatocare.data.AnalysisEntity
import com.example.tomatocare.data.AnalysisRepository
import com.example.tomatocare.data.TomatoDatabase
import com.example.tomatocare.ml.AnalysisOutcome
import com.example.tomatocare.ml.LeafPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TomatoViewModel(application: Application) : AndroidViewModel(application) {

    var imageUri by mutableStateOf<Uri?>(null)

    // Используется только для результата NoLeaf, который не сохраняется в БД.
    var result by mutableStateOf<AnalysisOutcome?>(null)

    val pipeline = LeafPipeline(application)

    private val database = TomatoDatabase.getInstance(application)

    private val repository = AnalysisRepository(
        application,
        database.analysisDao()
    )

    val analyses: Flow<List<AnalysisEntity>> = repository.analyses

    fun observeAnalysis(id: Long): Flow<AnalysisEntity?> {
        return repository.observeById(id)
    }

    fun saveAnalysis(
        uri: Uri,
        outcome: AnalysisOutcome.Leaf,
        onSaved: (Long) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val id = withContext(Dispatchers.IO) {
                    repository.save(uri, outcome)
                }

                onSaved(id)
            } catch (e: Exception) {
                Log.e("TomatoViewModel", "Не удалось сохранить анализ", e)
                onError(e)
            }
        }
    }

    fun deleteAnalysis(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.delete(id)
            } catch (e: Exception) {
                Log.e("TomatoViewModel", "Не удалось удалить анализ", e)
            }
        }
    }

    init {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                pipeline.warmUp()
            } catch (e: Throwable) {
                Log.e(
                    "TomatoViewModel",
                    "Не удалось загрузить модель детектора",
                    e
                )
            }
        }
    }

    override fun onCleared() {
        pipeline.close()
    }
}