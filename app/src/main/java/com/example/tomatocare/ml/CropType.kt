package com.example.tomatocare.ml

enum class CropType(
    val modelFile: String,
    val classesFile: String,
    val displayName: String
) {
    WHEAT("wheat_efficientnet_b0.onnx", "wheat_classes.json", "Пшеница"),
    TOMATO("tomato_efficientnet_b0.onnx", "tomato_classes.json", "Томат"),
    CORN("corn_efficientnet_b0.onnx", "corn_classes.json", "Кукуруза");

    companion object {
        /** Индексы совпадают с names в YOLO: 0 wheat, 1 tomato, 2 corn. */
        fun fromYoloClass(id: Int): CropType? = when (id) {
            0 -> WHEAT
            1 -> TOMATO
            2 -> CORN
            else -> null
        }
    }
}