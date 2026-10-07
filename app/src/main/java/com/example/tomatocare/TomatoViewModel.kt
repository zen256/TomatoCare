package com.example.tomatocare

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tomatocare.ml.ClassificationResult
import com.example.tomatocare.ml.TomatoClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Общее состояние для экранов камеры, анализа и результата.
 * Создаётся один раз на Activity, поэтому переживает повороты экрана и навигацию.
 */
class TomatoViewModel(application: Application) : AndroidViewModel(application) {

    /** Выбранный или снятый снимок. Заполняется на CameraScreen, читается на AnalysisScreen. */
    var imageUri by mutableStateOf<Uri?>(null)

    /** Результат нейросети. Заполняется после анализа, читается на ResultScreen. */
    var result by mutableStateOf<ClassificationResult?>(null)

    // Создание классификатора читает весь ONNX-файл, поэтому он создаётся один раз и лениво.
    private val classifierDelegate = lazy { TomatoClassifier(getApplication()) }
    val classifier: TomatoClassifier by classifierDelegate

    init {
        // Прогреваем модель в фоне, чтобы к входу на экран анализа она уже была загружена
        // и главный поток не подвисал на чтении файла.
        // Ошибку ловим здесь: необработанное исключение в корутине роняет приложение при запуске.
        viewModelScope.launch(Dispatchers.Default) {
            try {
                classifier
            } catch (e: Throwable) {
                Log.e("TomatoViewModel", "Не удалось загрузить модель", e)
            }
        }
    }

    override fun onCleared() {
        if (classifierDelegate.isInitialized()) {
            classifier.close()
        }
    }
}