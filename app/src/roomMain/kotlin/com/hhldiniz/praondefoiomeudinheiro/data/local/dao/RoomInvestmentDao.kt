package com.hhldiniz.praondefoiomeudinheiro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Investment
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.InvestmentRecord
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.toDomain
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.toRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room DAO for [InvestmentRecord]; implements the Room-free [InvestmentDao] contract. */
@Dao
interface RoomInvestmentDao : InvestmentDao {

    @Query("SELECT * FROM investments ORDER BY date_millis DESC, id DESC")
    fun getAllRecords(): Flow<List<InvestmentRecord>>

    override fun getAll(): Flow<List<Investment>> =
        getAllRecords().map { list -> list.map { it.toDomain() } }

    @Query("SELECT * FROM investments ORDER BY date_millis DESC, id DESC")
    suspend fun getAllSyncRecords(): List<InvestmentRecord>

    override suspend fun getAllSync(): List<Investment> = getAllSyncRecords().map { it.toDomain() }

    @Insert
    suspend fun insertRecord(investment: InvestmentRecord): Long

    override suspend fun insert(investment: Investment): Long = insertRecord(investment.toRecord())

    @Update
    suspend fun updateRecord(investment: InvestmentRecord)

    override suspend fun update(investment: Investment) = updateRecord(investment.toRecord())

    @Query("DELETE FROM investments WHERE id = :id")
    override suspend fun deleteById(id: Long)

    @Query("DELETE FROM investments")
    override suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM investments")
    override suspend fun count(): Int
}
