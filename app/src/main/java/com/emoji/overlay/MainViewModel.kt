package com.emoji.overlay

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emoji.overlay.data.entity.CategoryEntity
import com.emoji.overlay.data.entity.EmojiEntity
import com.emoji.overlay.data.repository.EmojiRepository
import com.emoji.overlay.data.repository.EmojiRepositoryHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel : ViewModel() {
    private val repository = MutableStateFlow<EmojiRepository?>(null)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    fun init(context: android.content.Context) {
        if (repository.value != null) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val repo = EmojiRepositoryHolder.getRepository(context.applicationContext)
                repository.value = repo
                _isReady.value = true
                Log.d(TAG, "Repository initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize repository", e)
                _isReady.value = true
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> repoFlow(
        fallback: T,
        block: (EmojiRepository) -> Flow<T>
    ): Flow<T> = repository.flatMapLatest { repo ->
        repo?.let(block) ?: flowOf(fallback)
    }

    fun getAllEmojis(): Flow<List<EmojiEntity>> =
        repoFlow(emptyList()) { it.getAllEmojisFlow() }

    fun getRecentEmojis(): Flow<List<EmojiEntity>> =
        repoFlow(emptyList()) { it.getRecentEmojisFlow(limit = 50) }

    fun getFavoriteEmojis(): Flow<List<EmojiEntity>> =
        repoFlow(emptyList()) { it.getFavoritesFlow() }

    fun getEmojisByCategory(categoryId: Long): Flow<List<EmojiEntity>> =
        repoFlow(emptyList()) { it.getEmojisByCategoryFlow(categoryId) }

    fun getCategories(): Flow<List<CategoryEntity>> =
        repoFlow(emptyList()) { it.getAllCategoriesFlow() }

    fun searchEmojis(query: String): Flow<List<EmojiEntity>> =
        repoFlow(emptyList()) { it.searchEmojisFlow(query) }

    fun getEmojiCount(): Flow<Int> =
        repoFlow(0) { it.getEmojiCountFlow() }

    fun importFromUris(context: android.content.Context, uris: List<Uri>) {
        val repo = repository.value ?: run {
            Toast.makeText(context, "数据库未就绪，请稍后重试", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            var imported = 0
            var duplicates = 0
            var errors = 0

            uris.forEach { uri ->
                try {
                    val sourceName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported"
                    val resolverMime = context.contentResolver.getType(uri)?.lowercase()
                    val safeExt = resolveImportExtension(sourceName, resolverMime)
                    val tempFile = File(context.cacheDir, "import_${System.currentTimeMillis()}.$safeExt")

                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        errors++
                        return@forEach
                    }

                    inputStream.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (!tempFile.exists() || tempFile.length() <= 0L) {
                        errors++
                        tempFile.delete()
                        return@forEach
                    }

                    val name = sourceName
                        .substringBeforeLast(".")
                        .ifBlank { "emoji_${System.currentTimeMillis()}" }

                    val result = repo.importEmoji(
                        file = tempFile,
                        name = name,
                        mimeHint = resolverMime
                    )
                    result.fold(
                        onSuccess = { imported++ },
                        onFailure = { e ->
                            if (e is com.emoji.overlay.data.repository.DuplicateEmojiException) {
                                duplicates++
                            } else {
                                Log.e(TAG, "Import failed for uri=$uri mime=$resolverMime", e)
                                errors++
                            }
                        }
                    )
                    tempFile.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Import failed for uri: $uri", e)
                    errors++
                }
            }

            launch(Dispatchers.Main) {
                val msg = buildString {
                    append("导入完成: $imported 个")
                    if (duplicates > 0) append(", $duplicates 个重复")
                    if (errors > 0) append(", $errors 个失败")
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resolveImportExtension(sourceName: String, mimeType: String?): String {
        val fromName = sourceName.substringAfterLast('.', "").lowercase()
        if (fromName in setOf("png", "jpg", "jpeg", "gif", "webp")) return fromName
        return when (mimeType) {
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            else -> "bin"
        }
    }

    fun createCategory(context: android.content.Context, name: String, icon: String = "📁") {
        if (name.isBlank()) {
            Toast.makeText(context, "分类名称不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        val repo = repository.value ?: run {
            Toast.makeText(context, "数据库未就绪，请稍后重试", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.createCategory(name = name, icon = icon)
            } catch (e: Exception) {
                Log.e(TAG, "createCategory failed", e)
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "创建分类失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun renameCategory(context: android.content.Context, category: CategoryEntity, newName: String, newIcon: String) {
        if (category.isSystem) {
            Toast.makeText(context, "系统分类不可重命名", Toast.LENGTH_SHORT).show()
            return
        }
        if (newName.isBlank()) {
            Toast.makeText(context, "分类名称不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        val repo = repository.value ?: run {
            Toast.makeText(context, "数据库未就绪，请稍后重试", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.updateCategory(
                    category.copy(
                        name = newName,
                        icon = newIcon,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "renameCategory failed", e)
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "重命名失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun deleteCategory(context: android.content.Context, categoryId: Long) {
        val repo = repository.value ?: run {
            Toast.makeText(context, "数据库未就绪，请稍后重试", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.deleteCategory(categoryId)
            } catch (e: Exception) {
                Log.e(TAG, "deleteCategory failed", e)
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "删除分类失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun moveEmojiToCategory(context: android.content.Context, emojiId: Long, categoryId: Long?) {
        val repo = repository.value ?: run {
            Toast.makeText(context, "数据库未就绪，请稍后重试", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.moveEmojiToCategory(emojiId, categoryId)
                launch(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (categoryId == null) "已移至未分类" else "已添加到分类",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "moveEmojiToCategory failed", e)
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "添加到分类失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun toggleFavorite(emojiId: Long) {
        val repo = repository.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.toggleFavorite(emojiId)
            } catch (e: Exception) {
                Log.e(TAG, "toggleFavorite failed", e)
            }
        }
    }

    fun recordUsage(emojiId: Long) {
        val repo = repository.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.recordUsage(emojiId)
            } catch (e: Exception) {
                Log.e(TAG, "recordUsage failed", e)
            }
        }
    }

    fun clearHistory() {
        val repo = repository.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.clearHistory()
            } catch (e: Exception) {
                Log.e(TAG, "clearHistory failed", e)
            }
        }
    }

    fun deleteEmoji(emojiId: Long) {
        val repo = repository.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.deleteEmoji(emojiId)
            } catch (e: Exception) {
                Log.e(TAG, "deleteEmoji failed", e)
            }
        }
    }
}
