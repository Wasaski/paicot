package com.paicot.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.paicot.core.OperationResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * File access. Two roots:
 *  - app-private: filesDir/paicot (always readable/writable)
 *  - SAF tree granted by the user (OpenDocumentTree) via ContentResolver —
 *    arbitrary storage stay behind the Storage Access Framework, no raw-path
 *    file access to other apps (Android 11+ scoped storage).
 */
class FileManager(private val context: Context) {

    val privateRoot: File get() = File(context.filesDir, "paicot").apply { mkdirs() }

    fun listPrivate(relative: String = ""): OperationResult {
        val dir = resolvePrivate(relative) ?: return OperationResult.failure("Invalid path: $relative")
        if (!dir.exists()) return OperationResult.failure("Directory not found")
        val entries = JSONArray()
        dir.listFiles()?.forEach { f ->
            entries.put(JSONObject()
                .put("name", f.name)
                .put("path", relativePath(f))
                .put("is_dir", f.isDirectory)
                .put("size", f.length()))
        }
        return OperationResult.success(JSONObject().put("entries", entries))
    }

    fun readPrivate(relative: String): OperationResult {
        val file = resolvePrivate(relative) ?: return OperationResult.failure("Invalid path: $relative")
        if (!file.exists() || file.isDirectory) return OperationResult.failure("File not found")
        if (file.length() > MAX_READ_BYTES) return OperationResult.failure("File too large (${file.length()} bytes)")
        return try {
            OperationResult.success(JSONObject().put("content", file.readText(Charsets.UTF_8)))
        } catch (e: Exception) {
            OperationResult.failure("read failed: ${e.message}")
        }
    }

    fun writePrivate(relative: String, content: String): OperationResult {
        val file = resolvePrivate(relative) ?: return OperationResult.failure("Invalid path: $relative")
        return try {
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
            OperationResult.success(JSONObject().put("path", relativePath(file)).put("bytes", file.length()))
        } catch (e: Exception) {
            OperationResult.failure("write failed: ${e.message}")
        }
    }

    fun deletePrivate(relative: String): OperationResult {
        val file = resolvePrivate(relative) ?: return OperationResult.failure("Invalid path: $relative")
        val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
        return if (deleted) OperationResult.success(JSONObject().put("deleted", relative))
        else OperationResult.failure("Delete failed (not found or in use)")
    }

    fun copyPrivate(from: String, to: String): OperationResult {
        val src = resolvePrivate(from) ?: return OperationResult.failure("Invalid source: $from")
        val dst = resolvePrivate(to) ?: return OperationResult.failure("Invalid destination: $to")
        if (!src.exists()) return OperationResult.failure("Source not found")
        return try {
            dst.parentFile?.mkdirs()
            src.copyTo(dst, overwrite = true)
            OperationResult.success(JSONObject().put("copied", "$from -> $to"))
        } catch (e: Exception) {
            OperationResult.failure("copy failed: ${e.message}")
        }
    }

    // --- SAF tree ---------------------------------------------------------

    fun readSaf(treeUri: String, relative: String): OperationResult {
        val doc = document(treeUri, relative) ?: return OperationResult.failure("Not found in granted tree")
        return try {
            context.contentResolver.openInputStream(doc)?.use { input ->
                val bytes = input.readBytes()
                if (bytes.size > MAX_READ_BYTES) return OperationResult.failure("File too large")
                OperationResult.success(JSONObject().put("content", bytes.toString(Charsets.UTF_8)))
            } ?: OperationResult.failure("Cannot open stream")
        } catch (e: Exception) {
            OperationResult.failure("SAF read failed: ${e.message}")
        }
    }

    fun writeSaf(treeUri: String, relative: String, content: String): OperationResult {
        return try {
            val parent = relative.substringBeforeLast("/", "")
            val name = relative.substringAfterLast("/")
            val dirUri = if (parent.isEmpty()) Uri.parse(treeUri) else document(treeUri, parent)
                ?: return OperationResult.failure("Parent folder not found")
            val created = DocumentsContract.createDocument(
                context.contentResolver, dirUri, "text/plain", name)
                ?: return OperationResult.failure("createDocument returned null")
            context.contentResolver.openOutputStream(created)?.use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
            }
            OperationResult.success(JSONObject().put("uri", created.toString()))
        } catch (e: Exception) {
            OperationResult.failure("SAF write failed: ${e.message}")
        }
    }

    private fun document(treeUri: String, relative: String): Uri? {
        val query = Uri.parse(treeUri).buildUpon().appendPath("tree").build()
        // Walk segments of the relative path starting at the tree root.
        var current = Uri.parse(treeUri)
        val rootId = DocumentsContract.getTreeDocumentId(current)
        var docId = rootId
        for (segment in relative.split("/").filter { it.isNotEmpty() }) {
            docId = DocumentsContract.buildChildDocumentsUriUsingTree(current, "$docId/$segment").toString()
            val exists = runCatching {
                context.contentResolver.query(Uri.parse(docId), null, null, null, null)?.use { it.count > -1 } ?: false
            }.isSuccess
            if (!exists) return null
            current = Uri.parse(docId)
        }
        return current
    }

    private fun resolvePrivate(relative: String): File? {
        if (relative.contains("..")) return null
        val root = privateRoot.canonicalFile
        val target = File(root, relative).canonicalFile
        return if (target.path.startsWith(root.path)) target else null
    }

    private fun relativePath(file: File): String =
        file.canonicalPath.removePrefix(privateRoot.canonicalPath).trimStart('/')

    private companion object { const val MAX_READ_BYTES = 512 * 1024 }
}
