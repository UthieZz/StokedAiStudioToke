package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StokedViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StokedRepository
    
    // Core state flows
    val activeCycle: StateFlow<CycleState?>
    val vaultList: StateFlow<List<VaultItem>>
    val salesList: StateFlow<List<SalesEvent>>
    val withdrawalsList: StateFlow<List<Withdrawal>>
    val consignmentsList: StateFlow<List<Consignment>>
    val dropRequestsList: StateFlow<List<DropRequest>>

    // Combined calculated properties
    val totalRevenue: StateFlow<Double>
    val vaultValuation: StateFlow<Double>
    val totalWithdrawn: StateFlow<Double>
    val totalCogs: StateFlow<Double>
    val roiBalance: StateFlow<Double>
    val remainingPrincipal: StateFlow<Double>
    val remainingRoi: StateFlow<Double>

    // UI Toast emission
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent = _toastEvent.asSharedFlow()

    // AI Status
    private val _aiResult = MutableStateFlow<String?>(null)
    val aiResult = _aiResult.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    // Curated Intelligence Feed State
    private val _intelligenceFeed = MutableStateFlow<List<IntelligenceReport>>(emptyList())
    val intelligenceFeed = _intelligenceFeed.asStateFlow()

    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport = _isGeneratingReport.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        val dao = database.stokedDao()
        repository = StokedRepository(dao)

        activeCycle = repository.activeCycle.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), null
        )
        vaultList = repository.vaultLive.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        salesList = repository.salesLive.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        withdrawalsList = repository.withdrawalsLive.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        consignmentsList = repository.consignmentsLive.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        dropRequestsList = repository.dropRequestsLive.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        // Derivative calculations using state flows
        totalRevenue = salesList.map { list ->
            list.sumOf { it.orderTotal }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        vaultValuation = vaultList.map { list ->
            list.filter { !it.isArchived }.sumOf { it.weight * it.costPerG }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        totalWithdrawn = withdrawalsList.map { list ->
            list.sumOf { it.value }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        // Total COGS Flow: Sum of all (quantity * unitCogs) from sales
        totalCogs = salesList.map { list ->
            list.sumOf { it.qty * it.unitCogs }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        // Available ROI Balance: (Capital * (1 + (roi% / 100))) - Owed/Withdrawn
        roiBalance = combine(activeCycle, totalWithdrawn) { cycle, wd ->
            if (cycle != null) {
                val targetValue = cycle.investedCapital * (1.0 + (cycle.roiPercent / 100.0))
                targetValue - wd
            } else {
                0.0
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        // Granular ROI splitting
        remainingRoi = combine(activeCycle, totalWithdrawn) { cycle, wd ->
            if (cycle != null) {
                val yieldAmount = cycle.investedCapital * (cycle.roiPercent / 100.0)
                maxOf(0.0, yieldAmount - wd)
            } else {
                0.0
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        remainingPrincipal = combine(activeCycle, totalWithdrawn) { cycle, wd ->
            if (cycle != null) {
                val yieldAmount = cycle.investedCapital * (cycle.roiPercent / 100.0)
                if (wd > yieldAmount) {
                    maxOf(0.0, cycle.investedCapital - (wd - yieldAmount))
                } else {
                    cycle.investedCapital
                }
            } else {
                0.0
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        // Populate curated reports
        _intelligenceFeed.value = listOf(
            IntelligenceReport(
                id = "rep_1",
                title = "European Cannabinoid Curation Trends Q2 2026",
                category = "MARKET DYNAMICS",
                summary = "Elite European import hubs report unprecedented demand spikes for indoor curations with rare terpene profiles. Brand prestige continues to dictate premium margins.",
                fullContent = "A comprehensive audit of retail distribution hubs in Frankfurt, Amsterdam, and Barcelona indicates a clear structural shift. Connoisseur consumers are increasingly ignoring standard grades in favor of visually striking, hand-trimmed indoor selections. Price elasticity in this premium segment remains exceptionally high, allowing operators with certified genetics to maintain margins upwards of 65%. Dynamic brand presentation and secure product sourcing protocols are now the primary differentiators of prestige.",
                trendVector = "+24.8% BULLISH",
                confidence = 98
            ),
            IntelligenceReport(
                id = "rep_2",
                title = "Luxury Greenhouse Thermal Optimization Analysis",
                category = "WHOLESALE STRATEGY",
                summary = "Innovative automated light-deprivation and climate controls drive down operational overhead while elevating flower density to traditional indoor quality standards.",
                fullContent = "Recent developments in greenhouse automation present a highly lucrative win-win opportunity for large-scale operations. By integrating dual-stage light deprivation shielding paired with micro-climate humidity control grids, producers can achieve high-density trichome production previously restricted to capital-intensive indoor chambers. This hybrid methodology reduces electricity expenditure by up to 58%, significantly driving down the base cost per gram while matching premium indoor wholesale valuation.",
                trendVector = "+14.2% EXPANDING",
                confidence = 94
            ),
            IntelligenceReport(
                id = "rep_3",
                title = "Terpene Customization and Genetics Differentiation",
                category = "GENETICS RESEARCH",
                summary = "Elite consumers demand bespoke aromatic and visual properties. Strains with distinct limonene and linalool saturation command premium price tiers globally.",
                fullContent = "Consumer telemetry across premium dispensaries and private curation lounges reveals that strain names are becoming secondary to specific terpene profiles. The presence of high-concentration linalool (lavender floral) and limonene (sharp citrus) is directly correlated with a 40% retail premium. Cultivators are advised to adjust their pipeline feeds to emphasize these flavor profiles, ensuring immediate clearance of newly harvested batches into high-end retail supply lines.",
                trendVector = "+31.5% CRITICAL",
                confidence = 96
            ),
            IntelligenceReport(
                id = "rep_4",
                title = "Dynamic Wholesaling & Token-Gated Distributions",
                category = "PRESTIGE DISTRIBUTION",
                summary = "Exclusivity-driven marketing pipelines and verification-gated access protect brand capital, prevent wholesale dilution, and ensure predictable ROI cycles.",
                fullContent = "Market intelligence shows that standard public wholesale listings often lead to price wars and brand dilution. In contrast, elite producers are adopting private, token-gated digital registries to handle distributions. By restricting allocation pipelines to pre-authorized partners and verified boutique distributors, brands can lock in long-term supply commitments, defend high price ceilings, and offer investors stable, predictable return profiles protected from broader market volatility.",
                trendVector = "STABLE PROTOCOL",
                confidence = 91
            )
        )

        // Populate mock starter items if empty to bring app alive instantly!
        viewModelScope.launch {
            val existingVault = repository.getVaultOnce()
            if (existingVault.isEmpty()) {
                prepopulateSampleData()
            }
        }
    }

    private suspend fun prepopulateSampleData() {
        // Active Cycle
        repository.insertCycle(CycleState(investedCapital = 85000.0, roiPercent = 25.0))

        // Premium Vault Variety Profiles
        val id1 = repository.insertVaultItem(VaultItem(strain = "Purple Sunset", weight = 142.5, costPerG = 22.0, tier = "Indoor"))
        val id2 = repository.insertVaultItem(VaultItem(strain = "Lemon Cane", weight = 200.0, costPerG = 18.0, tier = "Premium Greenhouse"))
        val id3 = repository.insertVaultItem(VaultItem(strain = "Violet Velvet", weight = 80.0, costPerG = 25.0, tier = "Green Door"))
        repository.insertVaultItem(VaultItem(strain = "Cosmic Grape", weight = 120.0, costPerG = 15.0, tier = "Flavour Greenhouse"))
        repository.insertVaultItem(VaultItem(strain = "Skywalker", weight = 350.0, costPerG = 8.5, tier = "Sungrown"))

        // Add preloaded completed retail stream sales
        repository.insertSaleEvent(SalesEvent(
            vaultId = id1.toString(),
            strainName = "Purple Sunset",
            tier = "Indoor",
            waOrderId = "#WA-SUN-42",
            customerId = "Marlon",
            description = "Selected premium curation. Superb dense finish.",
            qty = 10.0,
            orderTotal = 1500.0,
            unitCogs = 22.0,
            screenshotBase64 = null
        ))
        repository.insertSaleEvent(SalesEvent(
            vaultId = id2.toString(),
            strainName = "Lemon Cane",
            tier = "Premium Greenhouse",
            waOrderId = "#WA-LMN-99",
            customerId = "Tash",
            description = "$20g bundle deal. Clean flavor.",
            qty = 20.0,
            orderTotal = 2400.0,
            unitCogs = 18.0,
            screenshotBase64 = null
        ))

        // Preload active withdrawals
        repository.insertWithdrawal(Withdrawal(
            strain = "ROI Withdrawal: Purple Sunset",
            weight = 30.0,
            value = 4500.0,
            price = 150.0,
            note = "Direct allocation ledger handover"
        ))

        // Preload Consignments
        repository.insertConsignment(Consignment(
            supplier = "Coastal Greenhouse Coop",
            weight = 500.0,
            soldWeight = 150.0,
            owedCpg = 12.0,
            revenueRealized = 3200.0
        ))

        // Preload Investor Drop Request Pipelines (Requiring explicit Admin confirmation logs)
        repository.insertDropRequest(DropRequest(
            vaultId = id1,
            strainName = "Purple Sunset",
            tier = "Indoor",
            requestedWeight = 50.0,
            totalValuation = 7500.0,
            costPerG = 150.0
        ))
        repository.insertDropRequest(DropRequest(
            vaultId = id3,
            strainName = "Violet Velvet",
            tier = "Green Door",
            requestedWeight = 15.0,
            totalValuation = 2700.0,
            costPerG = 180.0
        ))
    }

    fun showToast(msg: String) {
        viewModelScope.launch { _toastEvent.emit(msg) }
    }

    // --- Cycles Operations ---
    fun commitCycleParameters(invested: Double, roiPercent: Double) {
        viewModelScope.launch {
            val active = repository.getActiveCycleOnce()
            if (active != null) {
                repository.updateCycle(active.copy(investedCapital = invested, roiPercent = roiPercent))
            } else {
                repository.insertCycle(CycleState(investedCapital = invested, roiPercent = roiPercent))
            }
            showToast("Active tracking configurations locked.")
        }
    }

    fun endAndArchiveCycle() {
        viewModelScope.launch {
            val active = repository.getActiveCycleOnce()
            if (active != null) {
                // Perform archiving sequence
                repository.archiveActiveCycles()
                repository.clearWithdrawals()
                // set clean slate cycle
                repository.insertCycle(CycleState(investedCapital = 0.0, roiPercent = 0.0))
                showToast("Active cycle ended and archived. Fresh start active.")
            }
        }
    }

    // --- Vault Operations (Status-Based Inventory Management: active, low_stock, archived) ---
    fun addToVault(strain: String, weight: Double, cost: Double, tier: String) {
        viewModelScope.launch {
            val status = InventoryStatus.fromWeight(weight)
            repository.insertVaultItem(VaultItem(strain = strain, weight = weight, costPerG = cost, tier = tier, status = status))
            showToast("Vault secured: $strain ($tier) — Status: ${status.uppercase()}")
        }
    }

    fun archiveVaultItem(item: VaultItem) {
        viewModelScope.launch {
            // Keep document in vault_holdings collection and flip status to archived without moving or deleting
            repository.updateVaultItem(item.copy(status = InventoryStatus.ARCHIVED))
            showToast("Archived holding: ${item.strain} (Status: ARCHIVED)")
        }
    }

    fun deleteVaultItem(id: Long, strain: String) {
        viewModelScope.launch {
            repository.deleteVaultItem(id)
            showToast("Purged vault profile: $strain")
        }
    }

    fun restoreVaultItem(item: VaultItem, newStockCount: Double) {
        viewModelScope.launch {
            // Check to prevent restoring archived assets with zero quantity without updating stock count first
            if (newStockCount <= 0.0) {
                showToast("RESTORE BLOCKED: Cannot restore asset with zero quantity. Please enter a valid stock count (> 0g) first.")
                return@launch
            }
            val nextStatus = InventoryStatus.fromWeight(newStockCount)
            repository.updateVaultItem(item.copy(weight = newStockCount, status = nextStatus))
            showToast("Restored ${item.strain}: ${newStockCount}g secured (${nextStatus.uppercase()})")
        }
    }

    fun updateVaultItemQuantity(item: VaultItem, newQty: Double) {
        viewModelScope.launch {
            // Check to prevent restoring archived assets with zero quantity without updating stock count first
            if (item.isArchived && newQty <= 0.0) {
                showToast("RESTORE BLOCKED: Stock count must be greater than zero to restore.")
                return@launch
            }
            val nextStatus = InventoryStatus.fromWeight(newQty)
            repository.updateVaultItem(item.copy(weight = newQty, status = nextStatus))
            showToast("Vault quantity updated (${newQty}g) — Status: ${nextStatus.uppercase()}")
        }
    }

    // --- Sales Operations ---
    fun insertManualSale(
        vaultId: String,
        strainName: String,
        tier: String,
        waOrderId: String,
        customerId: String,
        description: String,
        qty: Double,
        orderTotal: Double,
        screenshotBase64: String?,
        shouldDeductVault: Boolean,
        matchingVaultItem: VaultItem? = null,
        explicitUnitCogs: Double? = null
    ) {
        viewModelScope.launch {
            if (shouldDeductVault && matchingVaultItem != null) {
                val nextWeight = maxOf(0.0, matchingVaultItem.weight - qty)
                val nextStatus = InventoryStatus.fromWeight(nextWeight)
                repository.updateVaultItem(matchingVaultItem.copy(weight = nextWeight, status = nextStatus))
            }

            val finalCogs = explicitUnitCogs ?: matchingVaultItem?.costPerG ?: when (tier) {
                "Indoor" -> 22.0
                "Green Door" -> 20.0
                "Premium Greenhouse" -> 18.0
                "Flavour Greenhouse" -> 15.0
                else -> 8.5
            }

            repository.insertSaleEvent(SalesEvent(
                vaultId = vaultId,
                strainName = strainName,
                tier = tier,
                waOrderId = waOrderId,
                customerId = customerId,
                description = description,
                qty = qty,
                orderTotal = orderTotal,
                unitCogs = finalCogs,
                screenshotBase64 = screenshotBase64
            ))
            showToast("Transaction output authorized: ZAR $orderTotal")
        }
    }

    fun deleteSaleEvent(id: Long, label: String) {
        viewModelScope.launch {
            repository.deleteSale(id)
            showToast("Purged transaction: $label")
        }
    }

    // --- Withdrawals (ROI Pickups) ---
    fun logAssetWithdrawal(vaultItem: VaultItem, qty: Double, valBase: Double, ref: String) {
        viewModelScope.launch {
            val currentBalance = roiBalance.value
            if (valBase > currentBalance) {
                showToast("TRANSACTION BLOCKED: Insufficient available capital limit.")
                return@launch
            }
            
            val nextWeight = maxOf(0.0, vaultItem.weight - qty)
            val nextStatus = InventoryStatus.fromWeight(nextWeight)
            repository.updateVaultItem(vaultItem.copy(weight = nextWeight, status = nextStatus))

            val entryName = "ROI Withdrawal: ${vaultItem.strain}"
            repository.insertWithdrawal(Withdrawal(
                strain = entryName,
                weight = qty,
                value = valBase,
                price = valBase / qty,
                note = ref.ifEmpty { "No Ref" }
            ))
            showToast("Asset withdrawal logged securely.")
        }
    }

    fun deleteWithdrawal(id: Long) {
        viewModelScope.launch {
            repository.deleteWithdrawal(id)
            showToast("Asset withdrawal reference removed")
        }
    }

    // --- Consignments ---
    fun addConsignment(supplier: String, weight: Double, owedCpg: Double) {
        viewModelScope.launch {
            repository.insertConsignment(Consignment(supplier = supplier, weight = weight, owedCpg = owedCpg))
            showToast("Consignment active for: $supplier")
        }
    }

    fun updateConsignmentDelta(consignment: Consignment, sellQty: Double, revAmount: Double) {
        viewModelScope.launch {
            val nextSold = minOf(consignment.weight, consignment.soldWeight + sellQty)
            val nextRev = consignment.revenueRealized + revAmount
            repository.updateConsignment(consignment.copy(soldWeight = nextSold, revenueRealized = nextRev))
            showToast("Consignment updated.")
        }
    }

    fun deleteConsignment(id: Long) {
        viewModelScope.launch {
            repository.deleteConsignment(id)
            showToast("Consignment loop deleted")
        }
    }

    // --- Drop authorizations ---
    fun processDropAuthorization(request: DropRequest, approve: Boolean, matchingVaultItem: VaultItem?) {
        viewModelScope.launch {
            if (approve) {
                val currentBalance = roiBalance.value
                if (request.totalValuation > currentBalance) {
                    showToast("TRANSACTION BLOCKED: Insufficient available capital limit.")
                    return@launch
                }
                
                if (matchingVaultItem != null) {
                    if (matchingVaultItem.weight < request.requestedWeight) {
                        showToast("Out of bounds: Vault weight insufficient!")
                        return@launch
                    }
                    val nextWeight = matchingVaultItem.weight - request.requestedWeight
                    val nextStatus = InventoryStatus.fromWeight(nextWeight)
                    repository.updateVaultItem(matchingVaultItem.copy(weight = nextWeight, status = nextStatus))

                    // Log withdrawal / pickup
                    repository.insertWithdrawal(Withdrawal(
                        strain = "Investor Drop: ${request.strainName}",
                        weight = request.requestedWeight,
                        price = request.costPerG,
                        value = request.totalValuation,
                        note = "Approved drop allocation"
                    ))

                    repository.updateDropRequest(request.copy(status = "APPROVED"))
                    showToast("Drop request authorized and documented.")
                } else {
                    showToast("Vault location metadata invalid!")
                }
            } else {
                repository.updateDropRequest(request.copy(status = "REJECTED"))
                showToast("Drop request rejected.")
            }
        }
    }

    fun requestInvestorDrop(vaultItem: VaultItem, requestedWeight: Double, costPerG: Double) {
        viewModelScope.launch {
            val totalValuation = requestedWeight * costPerG
            val currentBalance = roiBalance.value
            if (totalValuation > currentBalance) {
                showToast("TRANSACTION BLOCKED: Valuation exceeds available credit (ZAR ${formatDecimal(currentBalance)}).")
                return@launch
            }
            if (requestedWeight > vaultItem.weight) {
                showToast("TRANSACTION BLOCKED: Requested weight (${requestedWeight}g) exceeds vault stock (${vaultItem.weight}g).")
                return@launch
            }
            repository.insertDropRequest(DropRequest(
                vaultId = vaultItem.id,
                strainName = vaultItem.strain,
                tier = vaultItem.tier,
                requestedWeight = requestedWeight,
                totalValuation = totalValuation,
                costPerG = costPerG,
                status = "PENDING"
            ))
            showToast("Drop Request for ${vaultItem.strain} submitted to Admin pipeline.")
        }
    }

    // --- AI Intel Analyst ---
    fun executeAIAnalystQuery(query: String) {
        viewModelScope.launch {
            if (query.trim().isEmpty()) return@launch
            _isAiLoading.value = true
            _aiResult.value = "Analyzing portfolio state, margins, and active holdings..."
            
            val vault = repository.getVaultOnce()
            val sales = repository.getSalesOnce()
            val active = activeCycle.value
            
            val invested = active?.investedCapital ?: 0.0
            val roiP = active?.roiPercent ?: 0.0
            
            val result = GeminiService.runAIAnalyst(
                prompt = query,
                vaultItems = vault,
                salesEvents = sales,
                investorCapital = invested,
                roiPercent = roiP,
                totalRevenue = totalRevenue.value,
                totalWithdrawn = totalWithdrawn.value,
                roiBalance = roiBalance.value
            )
            _aiResult.value = result
            _isAiLoading.value = false
        }
    }

    fun clearAiResult() {
        _aiResult.value = null
    }

    // --- Curated Intelligence Actions ---
    fun generateIntelligenceReport(userQuery: String) {
        viewModelScope.launch {
            if (userQuery.trim().isEmpty()) return@launch
            _isGeneratingReport.value = true
            showToast("INITIATING INTEL COGNITIVE COMPILATION...")

            val vault = repository.getVaultOnce()
            val active = activeCycle.value
            val invested = active?.investedCapital ?: 0.0
            val roiP = active?.roiPercent ?: 0.0

            val prompt = """
                You are the expert luxury market analyst for StokeD4T. 
                Generate a highly detailed, professional, elite-tier market intelligence and luxury trend report in response to this theme/query: "$userQuery".
                
                Analyze how it affects high-end boutique distribution, wholesale valuations, or connoisseur-grade brand prestige. Include specific context of our current business telemetry:
                - Active Vault Holdings: ${vault.joinToString { "${it.strain} (${it.tier})" }}
                - Current Portfolio Capital: ZAR ${"$"}$invested
                - Target Return Multiplier: $roiP%
                
                You MUST format your response as a valid JSON object with the following fields (do not include any backticks like ```json, just return the raw JSON text):
                {
                  "title": "A compelling, elite, high-end headline (e.g. 'Synthesizing Organic Luxury: The Purple Sunset Effect')",
                  "category": "GLOBAL LOGISTICS, GENETICS RESEARCH, WHOLESALE STRATEGY, PRESTIGE DESIGN, or CONSUMER BEHAVIOR",
                  "summary": "A concise, elegant 2-sentence summary of the core insight.",
                  "trendVector": "A trend indicator (e.g., '+18.5% BULLISH', 'STABLE PROTOCOL', or '+12.7% SURGING')",
                  "confidence": 95,
                  "fullContent": "A deep, professional, premium market audit and analytical intelligence brief. Mention how to optimize capital and vault selections for maximum 'win-win' deals."
                }
            """.trimIndent()

            val rawResult = GeminiService.runAIAnalyst(
                prompt = prompt,
                vaultItems = vault,
                salesEvents = emptyList(),
                investorCapital = invested,
                roiPercent = roiP,
                totalRevenue = totalRevenue.value,
                totalWithdrawn = totalWithdrawn.value,
                roiBalance = roiBalance.value
            )

            try {
                // Strip markdown block if returned
                var cleanJson = rawResult.trim()
                if (cleanJson.startsWith("```json")) {
                    cleanJson = cleanJson.substringAfter("```json").substringBeforeLast("```").trim()
                } else if (cleanJson.startsWith("```")) {
                    cleanJson = cleanJson.substringAfter("```").substringBeforeLast("```").trim()
                }

                val json = org.json.JSONObject(cleanJson)
                val newReport = IntelligenceReport(
                    id = "rep_" + System.currentTimeMillis(),
                    title = json.optString("title", "AI Custom Market Study"),
                    category = json.optString("category", "MARKET DYNAMICS"),
                    summary = json.optString("summary", "Custom compiled luxury trends research compiled by STOKED⁴T AI analyst."),
                    fullContent = json.optString("fullContent", rawResult),
                    trendVector = json.optString("trendVector", "+15.0% BULLISH"),
                    confidence = json.optInt("confidence", 95),
                    isUserGenerated = true
                )
                
                _intelligenceFeed.value = listOf(newReport) + _intelligenceFeed.value
                showToast("INTEL REPORT '${newReport.title.uppercase()}' COMPILED SUCCESSFULLY!")
            } catch (e: Exception) {
                val fallbackReport = IntelligenceReport(
                    id = "rep_" + System.currentTimeMillis(),
                    title = "AI-Driven Report: " + if (userQuery.length > 40) userQuery.take(40) + "..." else userQuery,
                    category = "MARKET DYNAMICS",
                    summary = "Custom analytics compile completed.",
                    fullContent = rawResult,
                    trendVector = "STABLE PROFILE",
                    confidence = 90,
                    isUserGenerated = true
                )
                _intelligenceFeed.value = listOf(fallbackReport) + _intelligenceFeed.value
                showToast("INTEL COMPILATION COMPLETED WITH TEXT BACKUP.")
            } finally {
                _isGeneratingReport.value = false
            }
        }
    }
}

// --- Intelligence Report UI Model ---
data class IntelligenceReport(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val fullContent: String,
    val trendVector: String,
    val confidence: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isUserGenerated: Boolean = false
)
