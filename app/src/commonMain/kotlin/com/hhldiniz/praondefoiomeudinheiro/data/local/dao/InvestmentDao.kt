package com.hhldiniz.praondefoiomeudinheiro.data.local.dao

import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Investment
import kotlinx.coroutines.flow.Flow

/**
 * [Investment] persistence contract. Room-backed on Android/iOS
 * (`RoomInvestmentDao` in `roomMain`), localStorage-backed on wasmJs
 * (`WebInvestmentDao`).
 *
 * Unlike imported entries there is no paging here: positions are typed in by
 * hand, so the whole list is small enough to hand over at once.
 */
interface InvestmentDao {

    /** Every position, newest application date first, ties broken by id. */
    fun getAll(): Flow<List<Investment>>

    /** Same ordering as [getAll], read once. */
    suspend fun getAllSync(): List<Investment>

    /** Inserts [investment], returning the new row id. */
    suspend fun insert(investment: Investment): Long

    /** Overwrites the row whose id matches [investment]'s; a no-op if none does. */
    suspend fun update(investment: Investment)

    /** Deletes the position with the given [id], if any. */
    suspend fun deleteById(id: Long)

    /** Deletes every row from the table. */
    suspend fun deleteAll()

    /** Number of stored positions. */
    suspend fun count(): Int
}
