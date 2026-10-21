package com.readmd.app.data

import com.readmd.app.domain.TextMatch
import com.readmd.app.domain.TextSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

data class SearchHit(
    val id: String,
    val name: String,
    val match: TextMatch,
)

/**
 * Searches the contents of already-scanned folder entries. Runs on the IO
 * dispatcher, emits hits as they are found, and stops promptly when the
 * collecting coroutine is cancelled (e.g. the query changed).
 */
class FolderSearcher(private val documents: DocumentRepository) {

    fun search(entries: List<FolderEntry>, query: String, maxHits: Int = MAX_HITS): Flow<SearchHit> =
        flow {
            var emitted = 0
            for (entry in entries) {
                if (emitted >= maxHits) break
                currentCoroutineContext().ensureActive()
                val text = runCatching { documents.read(entry.id).text }.getOrNull() ?: continue
                val match = TextSearch.find(text, query) ?: continue
                emit(SearchHit(id = entry.id, name = entry.name, match = match))
                emitted++
            }
        }.flowOn(Dispatchers.IO)

    companion object {
        const val MAX_HITS = 100
    }
}
