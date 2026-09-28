package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cycles")
data class CycleState(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val investedCapital: Double = 0.0,
    val roiPercent: Double = 0.0,
    val archived: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

object InventoryStatus {
    const val ACTIVE = "active"
    const val LOW_STOCK = "low_stock"
    const val ARCHIVED = "archived"

    fun fromWeight(weight: Double, lowStockThreshold: Double = 10.0): String {
        return when {
            weight <= 0.0 -> ARCHIVED
            weight <= lowStockThreshold -> LOW_STOCK
            else -> ACTIVE
        }
    }
}

@Entity(tableName = "vault_holdings")
data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val strain: String,
    val weight: Double,
    val costPerG: Double,
    val tier: String,
    val status: String = InventoryStatus.ACTIVE,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isArchived: Boolean
        get() = status.equals(InventoryStatus.ARCHIVED, ignoreCase = true)

    val isLowStock: Boolean
        get() = status.equals(InventoryStatus.LOW_STOCK, ignoreCase = true)

    val isActive: Boolean
        get() = status.equals(InventoryStatus.ACTIVE, ignoreCase = true)
}

@Entity(tableName = "sales_events")
data class SalesEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vaultId: String,
    val strainName: String,
    val tier: String,
    val waOrderId: String,
    val customerId: String,
    val description: String,
    val qty: Double,
    val orderTotal: Double,
    val unitCogs: Double = 0.0,
    val screenshotBase64: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "withdrawals")
data class Withdrawal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val strain: String,
    val weight: Double,
    val value: Double,
    val price: Double,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "consignments")
data class Consignment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplier: String,
    val weight: Double,
    val soldWeight: Double = 0.0,
    val owedCpg: Double,
    val revenueRealized: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "drop_requests")
data class DropRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vaultId: Long,
    val strainName: String,
    val tier: String,
    val requestedWeight: Double,
    val totalValuation: Double,
    val costPerG: Double,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val timestamp: Long = System.currentTimeMillis()
)
