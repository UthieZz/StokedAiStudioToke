package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StokedDao {

    // --- Cycles ---
    @Query("SELECT * FROM cycles WHERE archived = 0 ORDER BY id DESC LIMIT 1")
    fun getActiveCycle(): Flow<CycleState?>

    @Query("SELECT * FROM cycles WHERE archived = 0 ORDER BY id DESC LIMIT 1")
    suspend fun getActiveCycleOnce(): CycleState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: CycleState)

    @Update
    suspend fun updateCycle(cycle: CycleState)

    @Query("UPDATE cycles SET archived = 1 WHERE archived = 0")
    suspend fun archiveActiveCycles()

    // --- Vault ---
    @Query("SELECT * FROM vault_holdings ORDER BY timestamp DESC")
    fun getVaultLive(): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_holdings ORDER BY timestamp DESC")
    suspend fun getVaultOnce(): List<VaultItem>

    @Query("SELECT * FROM vault_holdings WHERE id = :id LIMIT 1")
    suspend fun getVaultItemById(id: Long): VaultItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultItem): Long

    @Update
    suspend fun updateVaultItem(item: VaultItem)

    @Query("DELETE FROM vault_holdings WHERE id = :id")
    suspend fun deleteVaultItem(id: Long)

    // --- Sales ---
    @Query("SELECT * FROM sales_events ORDER BY timestamp DESC")
    fun getSalesLive(): Flow<List<SalesEvent>>

    @Query("SELECT * FROM sales_events ORDER BY timestamp DESC")
    suspend fun getSalesOnce(): List<SalesEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleEvent(sale: SalesEvent)

    @Query("DELETE FROM sales_events WHERE id = :id")
    suspend fun deleteSale(id: Long)

    @Query("DELETE FROM sales_events")
    suspend fun clearSales()

    // --- Withdrawals (ROI Pickups) ---
    @Query("SELECT * FROM withdrawals ORDER BY timestamp DESC")
    fun getWithdrawalsLive(): Flow<List<Withdrawal>>

    @Query("SELECT * FROM withdrawals ORDER BY timestamp DESC")
    suspend fun getWithdrawalsOnce(): List<Withdrawal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawal(withdrawal: Withdrawal)

    @Query("DELETE FROM withdrawals WHERE id = :id")
    suspend fun deleteWithdrawal(id: Long)

    @Query("DELETE FROM withdrawals")
    suspend fun clearWithdrawals()

    // --- Consignments ---
    @Query("SELECT * FROM consignments ORDER BY timestamp DESC")
    fun getConsignmentsLive(): Flow<List<Consignment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsignment(consignment: Consignment)

    @Update
    suspend fun updateConsignment(consignment: Consignment)

    @Query("DELETE FROM consignments WHERE id = :id")
    suspend fun deleteConsignment(id: Long)

    // --- Drop Requests ---
    @Query("SELECT * FROM drop_requests ORDER BY timestamp DESC")
    fun getDropRequestsLive(): Flow<List<DropRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDropRequest(request: DropRequest)

    @Update
    suspend fun updateDropRequest(request: DropRequest)

    @Query("DELETE FROM drop_requests WHERE id = :id")
    suspend fun deleteDropRequest(id: Long)
}

@Database(
    entities = [
        CycleState::class,
        VaultItem::class,
        SalesEvent::class,
        Withdrawal::class,
        Consignment::class,
        DropRequest::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stokedDao(): StokedDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stoked4t_omni_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
