package com.oreoexperience.notes.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Copia URIs (foto/video del Photo Picker, archivos compartidos, etc.)
 * a una carpeta privada `filesDir/media/` para garantizar que el
 * archivo siga existiendo aunque el usuario revoque el permiso de
 * la galería o borre la imagen original.
 *
 * El nombre que devolvemos es **relativo** a esa carpeta (`<uuid>.<ext>`)
 * y es lo que persistimos en la nota — así el body queda independiente
 * de la ruta absoluta del dispositivo.
 */
class MediaStorage(private val context: Context) {

    private val mediaDir: File =
        File(context.filesDir, "media").apply { if (!exists()) mkdirs() }

    /**
     * Copia el contenido de [src] a la carpeta interna y devuelve el
     * nombre relativo del archivo guardado (sin path, p. ej.
     * `9f3e7c6b-…-4f.jpg`).
     *
     * Si la copia falla devuelve null para que el caller pueda mostrar
     * un toast / error.
     */
    suspend fun importUri(src: Uri, fallbackExt: String): String? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(src)
        val ext = mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?: fallbackExt
        val name = "${UUID.randomUUID()}.$ext"
        val outFile = File(mediaDir, name)
        runCatching {
            resolver.openInputStream(src).use { input ->
                FileOutputStream(outFile).use { output ->
                    input?.copyTo(output) ?: error("inputStream null")
                }
            }
            name
        }.getOrElse {
            outFile.delete()
            null
        }
    }

    /** Devuelve el File correspondiente a un nombre relativo guardado. */
    fun fileFor(name: String): File = File(mediaDir, name)

    /** Devuelve la URI `file://` lista para AsyncImage / VideoView. */
    fun uriFor(name: String): Uri = Uri.fromFile(fileFor(name))

    /** Borra los archivos cuya referencia ya no aparezca en ninguna nota. */
    suspend fun deleteIfExists(name: String) = withContext(Dispatchers.IO) {
        runCatching { fileFor(name).delete() }
    }
}
