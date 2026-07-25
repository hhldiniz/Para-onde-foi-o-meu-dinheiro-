package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.home

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository

/**
 * Loads [EntryDisplay] pages directly from the database with LIMIT/OFFSET,
 * instead of pulling the entire filtered result set into memory.
 */
class EntriesPagingSource(
    private val importRepository: ImportRepository,
    private val category: String?,
    private val startMillis: Long,
    private val endMillis: Long,
) : PagingSource<Int, EntryDisplay>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EntryDisplay> {
        val offset = params.key ?: 0
        val limit = params.loadSize
        return try {
            val entries = importRepository.getEntriesPage(
                category = category,
                startMillis = startMillis,
                endMillis = endMillis,
                limit = limit,
                offset = offset,
            ).map { entry ->
                EntryDisplay(
                    dateMillis = entry.dateMillis,
                    description = entry.description,
                    category = entry.category,
                    amount = entry.amount,
                    isExpense = entry.isExpense,
                    id = entry.id,
                )
            }
            LoadResult.Page(
                data = entries,
                prevKey = if (offset == 0) null else maxOf(0, offset - limit),
                nextKey = if (entries.size < limit) null else offset + limit,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, EntryDisplay>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchorPosition) ?: return null
        return page.prevKey?.plus(state.config.pageSize) ?: page.nextKey?.minus(state.config.pageSize)
    }
}
