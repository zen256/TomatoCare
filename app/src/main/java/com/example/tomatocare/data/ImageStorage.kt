package com.example.tomatocare.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class SavedImage(
    val relativePath: String
)

object ImageStorage {

    fun copyFromUri(context: Context, uri: Uri): SavedImage {
        val photosDir = File(context.filesDir, "photos")

        if (!photosDir.exists() && !photosDir.mkdirs()) {
            throw IllegalStateException("Не удалось создать каталог фотографий")
        }

        val mimeType = context.contentResolver.getType(uri)

        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?: uri.lastPathSegment
                ?.substringAfterLast('.', "")
                ?.takeIf { it.matches(Regex("[A-Za-z0-9]{1,8}")) }
            ?: "img"

        val fileName = "${UUID.randomUUID()}.$extension"
        val destination = File(photosDir, fileName)

        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Не удалось открыть изображение")

            input.use { source ->
                FileOutputStream(destination).use { output ->
                    source.copyTo(output)
                }
            }
        } catch (e: Exception) {
            destination.delete()
            throw e
        }

        return SavedImage("photos/$fileName")
    }

    fun getFile(context: Context, relativePath: String): File {
        val photosDir = File(context.filesDir, "photos").canonicalFile
        val file = File(context.filesDir, relativePath).canonicalFile

        require(file.path.startsWith(photosDir.path + File.separator)) {
            "Недопустимый путь к фотографии"
        }

        return file
    }

    fun delete(context: Context, relativePath: String) {
        runCatching {
            getFile(context, relativePath).delete()
        }
    }

    /**
     * Удаляет только исходные файлы приложения из cacheDir.
     * Файлы, выбранные через галерею, не затрагиваются.
     */
    fun deleteCacheSource(context: Context, uri: Uri) {
        if (uri.scheme != "file") return

        runCatching {
            val source = File(uri.path ?: return).canonicalFile
            val cacheDir = context.cacheDir.canonicalFile

            if (source.path.startsWith(cacheDir.path + File.separator)) {
                source.delete()
            }
        }
    }
}