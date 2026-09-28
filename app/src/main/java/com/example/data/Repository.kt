package com.example.data

import kotlinx.coroutines.flow.Flow

class StokedRepository(private val dao: StokedDao) {

    // --- Cycles ---
    val activeCycle: Flow<CycleState?> = dao.getActiveCycle()

    suspend fun getActiveCycleOnce(): CycleState? = dao.getActiveCycleOnce()

    suspend fun insertCycle(cycle: CycleState) = dao.insertCycle(cycle)

    suspend fun updateCycle(cycle: CycleState) = dao.updateCycle(cycle)

    suspend fun archiveActiveCycles() = dao.archiveActiveCycles()

    // --- Vault ---
    val vaultLive: Flow<List<VaultItem>> = dao.getVaultLive()

    suspend fun getVaultOnce(): List<VaultItem> = dao.getVaultOnce()

    suspend fun getVaultItemById(id: Long): VaultItem? = dao.getVaultItemById(id)

    suspend fun insertVaultItem(item: VaultItem): Long = dao.insertVaultItem(item)

    suspend fun updateVaultItem(item: VaultItem) = dao.updateVaultItem(item)

    suspend fun deleteVaultItem(id: Long) = dao.deleteVaultItem(id)

    // --- Sales ---
    val salesLive: Flow<List<SalesEvent>> = dao.getSalesLive()

    suspend fun getSalesOnce(): List<SalesEvent> = dao.getSalesOnce()

    suspend fun insertSaleEvent(sale: SalesEvent) = dao.insertSaleEvent(sale)

    suspend fun deleteSale(id: Long) = dao.deleteSale(id)

    suspend fun clearSales() = dao.clearSales()

    // --- Withdrawals ---
    val withdrawalsLive: Flow<List<Withdrawal>> = dao.getWithdrawalsLive()

    suspend fun getWithdrawalsOnce(): List<Withdrawal> = dao.getWithdrawalsOnce()

    suspend fun insertWithdrawal(withdrawal: Withdrawal) = dao.insertWithdrawal(withdrawal)

    suspend fun deleteWithdrawal(id: Long) = dao.deleteWithdrawal(id)

    suspend fun clearWithdrawals() = dao.clearWithdrawals()

    // --- Consignments ---
    val consignmentsLive: Flow<List<Consignment>> = dao.getConsignmentsLive()

    suspend fun insertConsignment(consignment: Consignment) = dao.insertConsignment(consignment)

    suspend fun updateConsignment(consignment: Consignment) = dao.updateConsignment(consignment)

    suspend fun deleteConsignment(id: Long) = dao.deleteConsignment(id)

    // --- Drop Requests ---
    val dropRequestsLive: Flow<List<DropRequest>> = dao.getDropRequestsLive()

    suspend fun insertDropRequest(request: DropRequest) = dao.insertDropRequest(request)

    suspend fun updateDropRequest(request: DropRequest) = dao.updateDropRequest(request)

    suspend fun deleteDropRequest(id: Long) = dao.deleteDropRequest(id)
}
