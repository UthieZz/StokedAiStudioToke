package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import androidx.compose.ui.text.TextStyle

// Brand Constants
val GoldColor = Color(0xFFD4AF37)
val GoldLight = Color(0xFFFFDF73)
val GoldDark = Color(0xFFAA7C11)
val DarkBg = Color(0xFF050505)
val CardBg = Color(0xFF0E0E10)
val CyanColor = Color(0xFF06B6D4)
val EmeraldColor = Color(0xFF10B981)
val AmberColor = Color(0xFFF59E0B)
val BorderColor = Color(0x1AFFFFFF)

val GoldGradient = Brush.horizontalGradient(
    colors = listOf(GoldColor, GoldLight, GoldDark)
)

val DarkGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF09090C), Color(0xFF040405))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StokedApp(viewModel: StokedViewModel) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf("dashboard") }
    
    // Auth & Access Control Layer State
    var appScreen by remember { mutableStateOf("public_landing") } // "public_landing", "gateway_secure", "secured_app"
    var currentRole by remember { mutableStateOf("") } // "INVESTOR" or "ADMIN"
    var accessDeniedErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSidebarExpanded by remember { mutableStateOf(true) }

    // Database and State Flows
    val activeCycle by viewModel.activeCycle.collectAsStateWithLifecycle()
    val vaultList by viewModel.vaultList.collectAsStateWithLifecycle()
    val salesList by viewModel.salesList.collectAsStateWithLifecycle()
    val withdrawalsList by viewModel.withdrawalsList.collectAsStateWithLifecycle()
    val consignmentsList by viewModel.consignmentsList.collectAsStateWithLifecycle()
    val dropRequestsList by viewModel.dropRequestsList.collectAsStateWithLifecycle()

    val totalRevenue by viewModel.totalRevenue.collectAsStateWithLifecycle()
    val vaultValuation by viewModel.vaultValuation.collectAsStateWithLifecycle()
    val totalWithdrawn by viewModel.totalWithdrawn.collectAsStateWithLifecycle()
    val totalCogs by viewModel.totalCogs.collectAsStateWithLifecycle()
    val remainingPrincipal by viewModel.remainingPrincipal.collectAsStateWithLifecycle()
    val remainingRoi by viewModel.remainingRoi.collectAsStateWithLifecycle()

    val aiResult by viewModel.aiResult.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    val toastState = remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Trigger local screen feedback toast
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { msg ->
            toastState.value = msg
        }
    }

    // System Back Handler for navigation continuity
    androidx.activity.compose.BackHandler(enabled = appScreen != "public_landing") {
        if (appScreen == "gateway_secure") {
            appScreen = "public_landing"
        } else if (appScreen == "secured_app") {
            if (currentTab != "dashboard") {
                currentTab = "dashboard"
            } else {
                appScreen = "public_landing"
                currentRole = ""
            }
        }
    }

    // Active screen dimensions / adaptive detection
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        val isWideScreen = maxWidth > 720.dp

        if (appScreen == "public_landing") {
            PublicLandingPage(
                errorMessage = accessDeniedErrorMessage,
                onCloseError = { accessDeniedErrorMessage = null },
                onEnterPortal = { appScreen = "gateway_secure" },
                vaultList = vaultList
            )
        } else if (appScreen == "gateway_secure") {
            SecureGatewayPage(
                onBackToPublic = { appScreen = "public_landing" },
                onValidateToken = { token ->
                    val cleanToken = token.trim().uppercase()
                    if (cleanToken == "INVESTOR2026") {
                        currentRole = "INVESTOR"
                        currentTab = "dashboard"
                        appScreen = "secured_app"
                        viewModel.showToast("INVESTOR SATELLITE ACCESS ESTABLISHED")
                    } else if (cleanToken == "ADMIN2026") {
                        currentRole = "ADMIN"
                        currentTab = "dashboard"
                        appScreen = "secured_app"
                        viewModel.showToast("ADMIN CREDENTIALS ACCEPTED")
                    } else {
                        accessDeniedErrorMessage = "ACCESS DENIED: SECURE GATEWAY ENCOUNTERED AN INVALID PROTOCOL NOT REGISTERED IN THE SYSTEMS REGISTRY."
                        appScreen = "public_landing"
                    }
                }
            )
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                // Adaptive Navigation Drawer (Sidebar)
                if (isWideScreen) {
                    SidebarMenu(
                        currentTab = currentTab,
                        currentRole = currentRole,
                        isExpanded = isSidebarExpanded,
                        onToggle = { isSidebarExpanded = !isSidebarExpanded },
                        onTabSelected = { tab ->
                            currentTab = tab
                            viewModel.clearAiResult()
                        }
                    )
                }

                // Main Contents
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(bottom = if (isWideScreen) 0.dp else 80.dp) // Leave space for BottomBar in mobile
                ) {
                    // Header Panel
                    EngineHeader(isWideScreen = isWideScreen)

                    // Global Stats & Indicators Metrics Row
                    GlobalMetricsGrid(
                        totalRev = totalRevenue,
                        vaultVal = vaultValuation,
                        pickupsVal = totalWithdrawn,
                        totalCogs = totalCogs,
                        remainingPrincipal = remainingPrincipal,
                        remainingRoi = remainingRoi,
                        currentTab = currentTab
                    )

                    // Main Navigation Tabs Router
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        when (currentTab) {
                            "dashboard" -> DashboardTab(
                                viewModel = viewModel,
                                sales = salesList,
                                withdrawals = withdrawalsList,
                                dropRequests = dropRequestsList,
                                vaultSnapshot = vaultList
                            )
                            "tracker" -> CycleControlsTab(
                                viewModel = viewModel,
                                activeCycle = activeCycle,
                                withdrawals = withdrawalsList
                            )
                            "vault" -> VaultTab(
                                viewModel = viewModel,
                                vaultList = vaultList,
                                currentRole = currentRole
                            )
                            "sales" -> SalesTab(
                                viewModel = viewModel,
                                salesList = salesList,
                                vaultList = vaultList
                            )
                            "withdrawals" -> WithdrawalsTab(
                                viewModel = viewModel,
                                withdrawalsList = withdrawalsList,
                                vaultList = vaultList
                            )
                            "consignments" -> ConsignmentsTab(
                                viewModel = viewModel,
                                consignmentsList = consignmentsList
                            )
                            "flyers" -> FlyerTab(
                                vaultList = vaultList
                            )
                            "ai" -> AIIntelTab(
                                viewModel = viewModel,
                                queryResult = aiResult,
                                isLoading = isAiLoading
                            )
                        }
                    }
                }
            }

            // Adaptive Mobile Bottom Navigation Bar (BottomBar)
            if (!isWideScreen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color(0xFF0C101B))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = if (currentRole == "INVESTOR") {
                            listOf(
                                Triple("dashboard", "Dash", Icons.Default.Home),
                                Triple("tracker", "Cycle", Icons.Default.Star),
                                Triple("vault", "Vault", Icons.Default.List),
                                Triple("flyers", "Flyer", Icons.Default.Email),
                                Triple("ai", "AI Intel", Icons.Default.Face)
                            )
                        } else {
                            listOf(
                                Triple("dashboard", "Dash", Icons.Default.Home),
                                Triple("tracker", "Cycle", Icons.Default.Star),
                                Triple("vault", "Vault", Icons.Default.List),
                                Triple("sales", "Retail", Icons.Default.ShoppingCart),
                                Triple("withdrawals", "Asset", Icons.Default.ArrowDownward),
                                Triple("consignments", "Consign", Icons.Default.Person),
                                Triple("flyers", "Flyer", Icons.Default.Email),
                                Triple("ai", "AI Intel", Icons.Default.Face)
                            )
                        }
                        tabs.forEach { (tab, label, icon) ->
                            MobileNavItem(tab, label, icon, currentTab) { currentTab = it }
                        }
                    }
                }
            }
        }

        // Floating Status Feedback Message Toast
        toastState.value?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isWideScreen) 24.dp else 96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .border(1.dp, GoldColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = msg.uppercase(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
            LaunchedEffect(msg) {
                delay(3000)
                toastState.value = null
            }
        }
    }
}

// Sidebar for Tablets & Larger Display Formats
@Composable
fun SidebarMenu(
    currentTab: String,
    currentRole: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onTabSelected: (String) -> Unit
) {
    var speechText by remember { mutableStateOf("Ready to execute, Moh.") }
    val startupPhrases = listOf(
        "Messing with the multiplier mechanics again?",
        "Don't go messing up the inventory counts, I'm not cleaning it up.",
        "Kaching... Boom. Money in the ledger is Dunya. Focus on Akhira.",
        "Wholesale tracking arrays. Keep the external suppliers in line.",
        "Everything database syncing securely. Ready for deployment."
    )

    Column(
        modifier = Modifier
            .width(if (isExpanded) 260.dp else 76.dp)
            .fillMaxHeight()
            .background(Color(0xFF06090F))
            .border(1.dp, Color(0xFF1E293B))
            .padding(if (isExpanded) 16.dp else 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Toggle Button
        IconButton(
            onClick = onToggle,
            modifier = Modifier.align(if (isExpanded) Alignment.End else Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowLeft else Icons.Default.KeyboardArrowRight,
                contentDescription = "Toggle Sidebar",
                tint = Color.Gray
            )
        }

        if (isExpanded) {
            // Brand Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
            Text(
                text = "FIREBASE GATEWAY",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xCCF59E0B), // amber-500/80
                letterSpacing = 3.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "STOKED",
                    fontWeight = FontWeight.Light,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "4",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.White,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "T",
                    fontWeight = FontWeight.Light,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
            }
        }
        Text(
            text = "OMNI MASTER ENGINE",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )

        // Telemetry widget simulation
        TelemetryWidget {
            speechText = startupPhrases.random()
        }

        // Mascot Speech Bubble
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                .padding(12.dp)
                .clickable { speechText = startupPhrases.random() }
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.Comment,
                    contentDescription = null,
                    tint = CyanColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = speechText,
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    lineHeight = 16.sp
                )
            }
        }
        } // Close if (isExpanded)

        if (isExpanded) {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Navigation Items
        val navigationList = if (currentRole == "INVESTOR") {
            listOf(
                Triple("dashboard", "Dashboard Stream", Icons.Default.Home),
                Triple("tracker", "Cycle Controls", Icons.Default.Star),
                Triple("vault", "Inventory Vault", Icons.Default.List),
                Triple("flyers", "Html Flyer Generator", Icons.Default.Email),
                Triple("ai", "AI Intel Analyst", Icons.Default.Face)
            )
        } else {
            listOf(
                Triple("dashboard", "Dashboard Stream", Icons.Default.Home),
                Triple("tracker", "Cycle Controls", Icons.Default.Star),
                Triple("vault", "Inventory Vault", Icons.Default.List),
                Triple("sales", "Direct Retail Sales", Icons.Default.ShoppingCart),
                Triple("withdrawals", "Asset ROI Outflow", Icons.Default.ArrowDownward),
                Triple("consignments", "Consignments Loop", Icons.Default.Person),
                Triple("flyers", "Html Flyer Generator", Icons.Default.Email),
                Triple("ai", "AI Intel Analyst", Icons.Default.Face)
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(navigationList) { triple ->
                val tab = triple.first
                val label = triple.second
                val icon = triple.third
                val isActive = currentTab == tab
                Button(
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("nav_$tab"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) Color(0x33D4AF37) else Color.Transparent,
                        contentColor = if (isActive) GoldColor else Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = if (isExpanded) PaddingValues(horizontal = 16.dp) else PaddingValues(0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = label, modifier = Modifier.size(if (isExpanded) 18.dp else 24.dp))
                        if (isExpanded) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryWidget(onTriggerAction: () -> Unit) {
    var isPulsing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            isPulsing = !isPulsing
            delay(1200)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.7f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .clickable { onTriggerAction() }
    ) {
        // Background grid pattern simulation
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeColor = Color(0x1106B6D4)
            val stepX = size.width / 10
            val stepY = size.height / 6
            for (i in 0..10) {
                drawLine(strokeColor, start = androidx.compose.ui.geometry.Offset(i * stepX, 0f), end = androidx.compose.ui.geometry.Offset(i * stepX, size.height))
            }
            for (i in 0..6) {
                drawLine(strokeColor, start = androidx.compose.ui.geometry.Offset(0f, i * stepY), end = androidx.compose.ui.geometry.Offset(size.width, i * stepY))
            }
        }

        // Telemetry details overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isPulsing) CyanColor else Color.DarkGray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "A18 // TELEMETRY",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "LIVE SYNC SECURE",
                    fontSize = 7.sp,
                    color = EmeraldColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Pulse wave
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = androidx.compose.ui.graphics.Path()
                    val width = size.width
                    val height = size.height
                    path.moveTo(0f, height / 2)
                    for (x in 0..width.toInt() step 5) {
                        val pulseFactor = if (isPulsing) 12f else 3f
                        val y = (height / 2) + Math.sin((x.toDouble() * 0.1) + (System.currentTimeMillis() * 0.01)).toFloat() * pulseFactor
                        path.lineTo(x.toFloat(), y)
                    }
                    drawPath(
                        path,
                        color = CyanColor.copy(alpha = 0.7f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            }

            Text(
                text = "SILO PORT: STOKED4T\nSYS STATUS: 100% OPERATIONAL\nDATABASE RUNTIME ACTIVE",
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.DarkGray,
                lineHeight = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun MobileNavItem(tab: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, currentTab: String, onClick: (String) -> Unit) {
    val isActive = currentTab == tab
    IconButton(
        onClick = { onClick(tab) },
        modifier = Modifier
            .size(54.dp)
            .testTag("nav_mobile_$tab")
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) GoldColor else Color(0x9994A3B8),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 8.sp,
                color = if (isActive) GoldColor else Color(0x9994A3B8),
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// Engine Header Panel
@Composable
fun EngineHeader(isWideScreen: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isWideScreen) {
            Column {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "STOKED",
                        fontWeight = FontWeight.Light,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontFamily = FontFamily.Serif,
                        fontSize = 18.sp,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "4",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = Color.White,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                    Text(
                        text = "T",
                        fontWeight = FontWeight.Light,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontFamily = FontFamily.Serif,
                        fontSize = 18.sp,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                }
                Text(
                    text = "OMNI MASTER CONTROL",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            Text(
                text = "CENTRAL PORTFOLIO DATAFEED",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(Color(0x2210B981))
                .border(1.dp, EmeraldColor.copy(alpha = 0.4f), RoundedCornerShape(50.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(EmeraldColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LIVE SYNC ACTIVE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// Global calculated metric panels
@Composable
fun GlobalMetricsGrid(
    totalRev: Double,
    vaultVal: Double,
    pickupsVal: Double,
    totalCogs: Double,
    remainingPrincipal: Double,
    remainingRoi: Double,
    currentTab: String
) {
    if (currentTab == "flyers") return // Hide metrics on flyer visualization

    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val netProfit = totalRev - totalCogs
            val marginPercent = if (totalRev > 0) ((netProfit / totalRev) * 100).toInt() else 0
            MetricCard("TOTAL REVENUE", "ZAR ${formatDecimal(totalRev)}", CyanColor)
            MetricCard("TOTAL COGS", "ZAR ${formatDecimal(totalCogs)}", AmberColor)
            MetricCard("NET PROFIT ($marginPercent%)", "ZAR ${formatDecimal(netProfit)}", EmeraldColor)
            MetricCard("VAULT ASSETS", "ZAR ${formatDecimal(vaultVal)}", Color(0xFF3B82F6))
            MetricCard("TOTAL HANDOVER", "ZAR ${formatDecimal(pickupsVal)}", GoldColor)
            MetricCard("REMAINING PRINCIPAL", "ZAR ${formatDecimal(remainingPrincipal)}", Color(0xFFA855F7))
            MetricCard("REMAINING ROI", "ZAR ${formatDecimal(remainingRoi)}", Color(0xFFEF4444))
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .width(132.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CardBg)
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(accentColor)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, top = 10.dp, end = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 8.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// FORMAT HELPER
fun formatDecimal(num: Double): String {
    return if (num % 1.0 == 0.0) {
        num.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", num)
    }
}

// VIEW LAYOUTS (DASHBOARD)
@Composable
fun DashboardTab(
    viewModel: StokedViewModel,
    sales: List<SalesEvent>,
    withdrawals: List<Withdrawal>,
    dropRequests: List<DropRequest>,
    vaultSnapshot: List<VaultItem>
) {
    val pendingDrops = dropRequests.filter { it.status == "PENDING" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Drop Authorization Pipeline
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .border(1.dp, AmberColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = AmberColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "INVESTOR DROP PIPELINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = AmberColor,
                            letterSpacing = 1.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AmberColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${pendingDrops.size} PENDING",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (pendingDrops.isEmpty()) {
                    Text(
                        text = "NO ACTIVE DROP AUTHORIZATION INPUTS DETECTED.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pendingDrops.forEach { req ->
                            val matchedVault = vaultSnapshot.find { it.id == req.vaultId }
                            DropAuthorizationRow(
                                request = req,
                                matchedItem = matchedVault,
                                onApprove = { viewModel.processDropAuthorization(req, true, matchedVault) },
                                onDecline = { viewModel.processDropAuthorization(req, false, matchedVault) }
                            )
                        }
                    }
                }
            }
        }

        // Subdivided List Grid (Immediates Handover Ledger vs Historical Logs)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Immediate Handover Ledger
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBg)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "IMMEDIATE HANDOVER LEDGER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Divider(color = Color(0xFF1E293B), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(8.dp))

                    if (withdrawals.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No handovers registered.", fontSize = 11.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(withdrawals) { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(p.strain.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                                        Text("${p.weight}g @ ZAR ${formatDecimal(p.price)}/g", fontSize = 8.sp, color = Color.Gray)
                                    }
                                    Text("-ZAR ${formatDecimal(p.value)}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }

                // Historical completed drops logs
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBg)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "HISTORICAL COMPLETED DROPS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Divider(color = Color(0xFF1E293B), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(8.dp))

                    val approvedDrops = dropRequests.filter { it.status == "APPROVED" }
                    if (approvedDrops.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No completed drop authorizations.", fontSize = 11.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(approvedDrops) { d ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(d.strainName.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                                        Text("● TRANSFERRED OUTFLOW", fontSize = 7.sp, color = EmeraldColor, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${d.requestedWeight}g", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldColor)
                                        Text("ZAR ${formatDecimal(d.totalValuation)}", fontSize = 8.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DropAuthorizationRow(
    request: DropRequest,
    matchedItem: VaultItem?,
    onApprove: () -> Unit,
    onDecline: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AmberColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(request.tier.uppercase(), fontSize = 7.sp, fontWeight = FontWeight.Bold, color = AmberColor)
                    }
                    Text(request.strainName.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Grams Needed: ${request.requestedWeight}g", fontSize = 9.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("ZAR ${formatDecimal(request.totalValuation)}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = AmberColor)
                    Text("Cost: ${request.costPerG}/g", fontSize = 8.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onDecline,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D1616), contentColor = Color(0xFFEF4444)),
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("DECLINE", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldColor, contentColor = Color.Black),
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("APPROVE DROP", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}

// CYCLE CONTROLS TAB
@Composable
fun CycleControlsTab(
    viewModel: StokedViewModel,
    activeCycle: CycleState?,
    withdrawals: List<Withdrawal>
) {
    var capitalInput by remember { mutableStateOf(activeCycle?.investedCapital?.toString() ?: "") }
    var roiInput by remember { mutableStateOf(activeCycle?.roiPercent?.toString() ?: "") }

    LaunchedEffect(activeCycle) {
        if (activeCycle != null) {
            capitalInput = activeCycle.investedCapital.toString()
            roiInput = activeCycle.roiPercent.toString()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("CYCLE CONTROLS SWITCHBOARD", fontSize = 16.sp, fontWeight = FontWeight.Black, color = GoldColor, letterSpacing = 1.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Modify active cycle card
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "INITIALIZE / MODIFY REVENUE CYCLE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldColor,
                    letterSpacing = 0.5.sp
                )

                OutlinedTextField(
                    value = capitalInput,
                    onValueChange = { capitalInput = it },
                    label = { Text("Investor Capital Input (ZAR)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = roiInput,
                    onValueChange = { roiInput = it },
                    label = { Text("Target ROI Multiplier (%)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val cap = capitalInput.toDoubleOrNull() ?: 0.0
                        val roi = roiInput.toDoubleOrNull() ?: 0.0
                        viewModel.commitCycleParameters(cap, roi)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldColor, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SAVE PARAMETERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            // Closeout & Archive card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF220A0A))
                    .border(1.dp, Color(0xFF7F1D1D), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "CLOSE OUT & ARCHIVE CYCLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "CONCLUDES THE PORTFOLIO'S TIMELINE.\n\nSAVES ACTIVE LEDGERS, RESETS METRIC COUNTERS, AND PREPARES A CLEAN SYSTEM CANVAS FOR THE NEXT LAUNCH PERIOD.",
                        fontSize = 9.sp,
                        color = Color.LightGray,
                        lineHeight = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.endAndArchiveCycle() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C), contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("END CYCLE & RESET STATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}

// VAULT SUB TAB
@Composable
fun VaultTab(viewModel: StokedViewModel, vaultList: List<VaultItem>, currentRole: String = "ADMIN") {
    val remainingRoi by viewModel.remainingRoi.collectAsStateWithLifecycle()
    val remainingPrincipal by viewModel.remainingPrincipal.collectAsStateWithLifecycle()

    var strainInput by remember { mutableStateOf("") }
    var massInput by remember { mutableStateOf("") }
    var costInput by remember { mutableStateOf("") }
    var selectedTier by remember { mutableStateOf("Indoor") }
    var isExpanded by remember { mutableStateOf(false) }

    var filterQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") } // "ALL", "ACTIVE", "LOW_STOCK", "ARCHIVED"

    val tiers = listOf("Indoor", "Green Door", "Premium Greenhouse", "Flavour Greenhouse", "Sungrown")

    var restoreTarget by remember { mutableStateOf<VaultItem?>(null) }
    var restoreQuantityInput by remember { mutableStateOf("") }

    var dropRequestTarget by remember { mutableStateOf<VaultItem?>(null) }
    var dropWeightInput by remember { mutableStateOf("") }
    var dropCostPerGInput by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Column (Asset inputs or Investor Portal view)
        Column(
            modifier = Modifier
                .width(260.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentRole == "INVESTOR") {
                Text("INVESTOR ALLOCATION PORTAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldColor, letterSpacing = 0.5.sp)
                Text(
                    text = "SELECT ANY ACTIVE STRAIN IN THE VAULT MATRIX TO SUBMIT A DROP ALLOCATION REQUEST.",
                    fontSize = 9.sp,
                    color = Color.LightGray,
                    lineHeight = 13.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0C101B))
                        .border(1.dp, CyanColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("AVAILABLE CREDIT LIMIT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = CyanColor, fontFamily = FontFamily.Monospace)
                        Text("ZAR ${formatDecimal(remainingRoi)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = EmeraldColor, fontFamily = FontFamily.Monospace)
                        Divider(color = Color(0xFF1E293B), thickness = 1.dp)
                        Text("PROTECTED PRINCIPAL", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray, fontFamily = FontFamily.Monospace)
                        Text("ZAR ${formatDecimal(remainingPrincipal)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                }
            } else {
                Text("SECURE NEW HOLDING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldColor, letterSpacing = 0.5.sp)

                OutlinedTextField(
                    value = strainInput,
                    onValueChange = { strainInput = it },
                    label = { Text("Strain Variety Name", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = massInput,
                    onValueChange = { massInput = it },
                    label = { Text("Allocated Mass Allocation (g)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = costInput,
                    onValueChange = { costInput = it },
                    label = { Text("Base Cost Per Gram (ZAR)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Dynamic Tier Spinner dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { isExpanded = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("TIER: ${selectedTier.uppercase()}", fontSize = 10.sp, color = Color.White)
                    }
                    DropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false }
                    ) {
                        tiers.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.uppercase(), fontSize = 10.sp) },
                                onClick = {
                                    selectedTier = t
                                    isExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val mass = massInput.toDoubleOrNull() ?: 0.0
                        val cost = costInput.toDoubleOrNull() ?: 0.0
                        if (strainInput.isNotBlank() && mass > 0.0 && cost > 0.0) {
                            viewModel.addToVault(strainInput, mass, cost, selectedTier)
                            strainInput = ""
                            massInput = ""
                            costInput = ""
                        } else {
                            viewModel.showToast("Verification Error: Complete values before securing.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldColor, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("LOCK SECURELY", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }

        // Right Column (Display List Control)
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("VAULT HOLDINGS MATRIX", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    placeholder = { Text("Filter strain selections...", fontSize = 9.sp) },
                    modifier = Modifier.width(180.dp).height(42.dp),
                    textStyle = TextStyle(fontSize = 11.sp)
                )
            }

            // Status Filter Chips (active, low_stock, archived)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "ACTIVE", "LOW_STOCK", "ARCHIVED").forEach { chipStatus ->
                    val isSelected = statusFilter == chipStatus
                    val chipColor = when (chipStatus) {
                        "ACTIVE" -> EmeraldColor
                        "LOW_STOCK" -> AmberColor
                        "ARCHIVED" -> Color.Red
                        else -> GoldColor
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) chipColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f))
                            .border(1.dp, if (isSelected) chipColor else BorderColor, RoundedCornerShape(6.dp))
                            .clickable { statusFilter = chipStatus }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = when (chipStatus) {
                                "ALL" -> "ALL (${vaultList.size})"
                                "ACTIVE" -> "ACTIVE (${vaultList.count { it.isActive }})"
                                "LOW_STOCK" -> "LOW STOCK (${vaultList.count { it.isLowStock }})"
                                "ARCHIVED" -> "ARCHIVED (${vaultList.count { it.isArchived }})"
                                else -> chipStatus
                            },
                            fontSize = 8.5.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) chipColor else Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val filtered = vaultList.filter { item ->
                val matchesQuery = item.strain.contains(filterQuery, ignoreCase = true)
                val matchesStatus = when (statusFilter) {
                    "ACTIVE" -> item.isActive
                    "LOW_STOCK" -> item.isLowStock
                    "ARCHIVED" -> item.isArchived
                    else -> true
                }
                matchesQuery && matchesStatus
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No profiles match filter conditions.", fontSize = 11.sp, color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered) { item ->
                        val statusBorderColor = when {
                            item.isArchived -> Color.Red.copy(alpha = 0.4f)
                            item.isLowStock -> AmberColor.copy(alpha = 0.4f)
                            else -> Color(0xFF1E293B)
                        }
                        val (badgeBg, badgeText, badgeColor) = when {
                            item.isArchived -> Triple(Color.Red.copy(alpha = 0.15f), "ARCHIVED", Color.Red)
                            item.isLowStock -> Triple(AmberColor.copy(alpha = 0.15f), "LOW_STOCK (≤10g)", AmberColor)
                            else -> Triple(EmeraldColor.copy(alpha = 0.15f), "ACTIVE", EmeraldColor)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .border(1.dp, statusBorderColor, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.strain.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (item.isArchived) Color.Gray else Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CyanColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(item.tier.uppercase(), fontSize = 7.sp, fontWeight = FontWeight.Bold, color = CyanColor)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(badgeBg)
                                            .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(badgeText, fontSize = 7.sp, fontWeight = FontWeight.Black, color = badgeColor)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val stockDisplay = if (item.isArchived && item.weight <= 0.0) {
                                    "STOCK: 0g (RESTOCK REQUIRED TO REACTIVATE)  |  BASE COST: ZAR ${formatDecimal(item.costPerG)}/g"
                                } else {
                                    "WEIGHT: ${item.weight}g left  |  BASE COST: ZAR ${formatDecimal(item.costPerG)}/g"
                                }
                                Text(
                                    stockDisplay,
                                    fontSize = 10.sp,
                                    color = if (item.isArchived && item.weight <= 0.0) AmberColor else Color.Gray,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (item.isArchived && item.weight <= 0.0) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            if (item.isArchived) {
                                Button(
                                    onClick = { 
                                        restoreTarget = item
                                        restoreQuantityInput = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldColor.copy(alpha = 0.2f), contentColor = GoldColor),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("restore_btn_${item.id}")
                                ) {
                                    Text(
                                        text = if (item.weight <= 0.0) "RESTORE (RESTOCK)" else "RESTORE", 
                                        fontSize = 8.sp, 
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (currentRole == "INVESTOR") {
                                Button(
                                    onClick = {
                                        dropRequestTarget = item
                                        dropWeightInput = ""
                                        dropCostPerGInput = item.costPerG.toString()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanColor.copy(alpha = 0.2f), contentColor = CyanColor),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("REQUEST DROP", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.archiveVaultItem(item) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(30.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("ARCHIVE", fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteVaultItem(item.id, item.strain) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Purge", tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Self-Correcting Restore Logic with zero quantity check & infinite archival loop guard
    if (restoreTarget != null) {
        val targetItem = restoreTarget!!
        val isCurrentStockZero = targetItem.weight <= 0.0
        val parsedRestoreQty = restoreQuantityInput.toDoubleOrNull() ?: 0.0
        val isRestoreValid = parsedRestoreQty > 0.0
        val previewStatus = InventoryStatus.fromWeight(parsedRestoreQty)

        Dialog(onDismissRequest = { restoreTarget = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = CardBg,
                border = BorderStroke(1.dp, GoldColor)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RESTORE INVENTORY ASSET",
                            fontWeight = FontWeight.Black,
                            color = GoldColor,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Red.copy(alpha = 0.15f))
                                .border(1.dp, Color.Red.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "STATUS: ARCHIVED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Text(
                        text = "Asset: ${targetItem.strain.uppercase()} (${targetItem.tier})\nCurrent Document Stock: ${targetItem.weight}g",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )

                    // Client-side Validation Guard Banner
                    if (isCurrentStockZero) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2D1214))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ARCHIVAL LOOP GUARD ACTIVE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFEF4444),
                                        letterSpacing = 0.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = "Current stock count is 0g. Restoring an asset with zero quantity triggers an immediate infinite re-archiving loop. The Restore action is DISABLED until a new stock quantity (> 0g) is inputted.",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFFFCA5A5),
                                    lineHeight = 13.5.sp
                                )
                            }
                        }
                    }

                    // Stock Quantity Input Field
                    OutlinedTextField(
                        value = restoreQuantityInput,
                        onValueChange = { restoreQuantityInput = it },
                        label = { Text("New Stock Quantity (grams) *", fontSize = 9.sp) },
                        placeholder = { Text("e.g. 50.0", fontSize = 9.sp, color = Color.DarkGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_quantity_input"),
                        isError = restoreQuantityInput.isNotBlank() && !isRestoreValid,
                        supportingText = {
                            if (!isRestoreValid) {
                                Text(
                                    text = "Action disabled: Enter a quantity greater than zero.",
                                    fontSize = 8.5.sp,
                                    color = Color.Red
                                )
                            } else {
                                Text(
                                    text = "Will reactivate as: ${previewStatus.uppercase()} (${parsedRestoreQty}g)",
                                    fontSize = 8.5.sp,
                                    color = if (previewStatus == InventoryStatus.LOW_STOCK) AmberColor else EmeraldColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )

                    // Quick-fill quantity presets for convenience
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(25.0, 50.0, 100.0, 250.0).forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .clickable { restoreQuantityInput = preset.toString() }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${preset.toInt()}g",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldColor
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { restoreTarget = null; restoreQuantityInput = "" }) {
                            Text("CANCEL", color = Color.Gray, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (isRestoreValid) {
                                    viewModel.restoreVaultItem(targetItem, parsedRestoreQty)
                                    restoreTarget = null
                                    restoreQuantityInput = ""
                                } else {
                                    viewModel.showToast("VALIDATION GUARD: Input a stock quantity (> 0g) before restoring.")
                                }
                            },
                            enabled = isRestoreValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldColor, 
                                contentColor = Color.Black,
                                disabledContainerColor = Color(0xFF1E293B),
                                disabledContentColor = Color.DarkGray
                            ),
                            modifier = Modifier.testTag("confirm_restore_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isRestoreValid) "CONFIRM RESTORE (${formatDecimal(parsedRestoreQty)}g)" else "RESTORE DISABLED (INPUT STOCK)",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal: Investor Drop Request Authorization
    if (dropRequestTarget != null) {
        val target = dropRequestTarget!!
        val requestedGrams = dropWeightInput.toDoubleOrNull() ?: 0.0
        val costPerGVal = dropCostPerGInput.toDoubleOrNull() ?: target.costPerG
        val totalValuation = requestedGrams * costPerGVal
        val availableCredit = remainingRoi
        val isExceeded = totalValuation > availableCredit
        val isMassExceeded = requestedGrams > target.weight
        val isValid = requestedGrams > 0.0 && !isExceeded && !isMassExceeded

        Dialog(onDismissRequest = { dropRequestTarget = null }) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = CardBg,
                border = BorderStroke(1.dp, CyanColor)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("REQUEST DROP ALLOCATION", fontWeight = FontWeight.Black, color = CyanColor, fontSize = 14.sp)
                    Text("Strain: ${target.strain.uppercase()} (${target.tier})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Available in Vault: ${target.weight}g", color = Color.Gray, fontSize = 10.sp)

                    OutlinedTextField(
                        value = dropWeightInput,
                        onValueChange = { dropWeightInput = it },
                        label = { Text("Requested Weight (g)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dropCostPerGInput,
                        onValueChange = { dropCostPerGInput = it },
                        label = { Text("Valuation Rate per Gram (ZAR)", fontSize = 9.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0C101B))
                            .border(1.dp, if (isExceeded || isMassExceeded) Color.Red else Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Drop Valuation:", fontSize = 9.sp, color = Color.Gray)
                            Text("ZAR ${formatDecimal(totalValuation)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isExceeded) Color.Red else Color.White)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Available Credit Limit:", fontSize = 9.sp, color = Color.Gray)
                            Text("ZAR ${formatDecimal(availableCredit)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldColor)
                        }
                        if (isExceeded) {
                            Text("BLOCKED: Valuation exceeds available credit limit.", fontSize = 8.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                        if (isMassExceeded) {
                            Text("BLOCKED: Requested weight exceeds vault stock (${target.weight}g).", fontSize = 8.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { dropRequestTarget = null }) {
                            Text("CANCEL", color = Color.Gray, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (isValid) {
                                    viewModel.requestInvestorDrop(target, requestedGrams, costPerGVal)
                                    dropRequestTarget = null
                                }
                            },
                            enabled = isValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanColor,
                                contentColor = Color.Black,
                                disabledContainerColor = Color(0xFF1E293B),
                                disabledContentColor = Color.Gray
                            )
                        ) {
                            Text("SUBMIT REQUEST", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// RETAIL SALES TAB (Upgraded with autoparsing selection dialog bounds)
@Composable
fun SalesTab(viewModel: StokedViewModel, salesList: List<SalesEvent>, vaultList: List<VaultItem>) {
    var selectedVaultId by remember { mutableStateOf("") }
    var orderId by remember { mutableStateOf("") }
    var customerId by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var massSold by remember { mutableStateOf("") }
    var priceSelected by remember { mutableStateOf("") }
    var resultImageBase64 by remember { mutableStateOf<String?>(null) }
    
    var isExpanded by remember { mutableStateOf(false) }
    var imageViewerUrl by remember { mutableStateOf<String?>(null) }

    // Dialog prompts for manual/auto-parsing decision handling
    var showConflictDialog by remember { mutableStateOf(false) }
    var conflictStrainName by remember { mutableStateOf("") }
    var conflictTierConfig by remember { mutableStateOf("Indoor") }

    val context = LocalContext.current

    // Activity launcher for Receipt screenshot capture (Digital Witness)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
                resultImageBase64 = "data:image/jpeg;base64,$base64"
                viewModel.showToast("Verified attachment linked successfully.")
            } catch (e: Exception) {
                viewModel.showToast("Attachment error: ${e.message}")
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left forms column
        Column(
            modifier = Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("LOG CASH TRANSACTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldColor, letterSpacing = 0.5.sp)

            // Selection Dropdown for Vault holdings
            val selectedItem = vaultList.find { it.id.toString() == selectedVaultId }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { isExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (selectedItem != null) "${selectedItem.strain.uppercase()} (${selectedItem.weight}g)" else "CUSTOM VAULT EXTRACTION",
                        fontSize = 9.sp,
                        color = Color.White
                    )
                }
                DropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("CUSTOM ENTRY / AUTO EXTRACT", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = GoldColor) },
                        onClick = {
                            selectedVaultId = ""
                            isExpanded = false
                        }
                    )
                    vaultList.forEach { v ->
                        DropdownMenuItem(
                            text = { Text("${v.strain.uppercase()} (${v.weight}g left)", fontSize = 9.sp) },
                            onClick = {
                                selectedVaultId = v.id.toString()
                                isExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = orderId,
                onValueChange = { orderId = it },
                label = { Text("Invoice Order ID (Optional)", fontSize = 9.sp) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = customerId,
                onValueChange = { customerId = it },
                label = { Text("Customer Identity Tag (Name)", fontSize = 9.sp) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Product Description Parsing Input", fontSize = 9.sp) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Purple Sunset - Indoor", fontSize = 8.sp) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = massSold,
                    onValueChange = { massSold = it },
                    label = { Text("Mass (g)", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = priceSelected,
                    onValueChange = { priceSelected = it },
                    label = { Text("ZAR Total", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            // Live Derived Background Calculations Preview
            val previewQty = massSold.toDoubleOrNull() ?: 0.0
            val previewTotal = priceSelected.toDoubleOrNull() ?: 0.0
            val effectiveCogsPerG = selectedItem?.costPerG ?: when {
                description.contains("sungrown", ignoreCase = true) -> 8.5
                description.contains("green door", ignoreCase = true) -> 20.0
                description.contains("premium greenhouse", ignoreCase = true) -> 18.0
                description.contains("flavour greenhouse", ignoreCase = true) -> 15.0
                else -> 22.0
            }

            if (previewQty > 0.0 && previewTotal > 0.0) {
                val derivedUnitPrice = previewTotal / previewQty
                val totalTransactionCogs = previewQty * effectiveCogsPerG
                val derivedNetGain = previewTotal - totalTransactionCogs

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0C101B))
                        .border(1.dp, CyanColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "DERIVED BACKGROUND CALCULATIONS",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanColor,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Derived Unit Price:", fontSize = 9.sp, color = Color.Gray)
                        Text("ZAR ${formatDecimal(derivedUnitPrice)}/g", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Unit COGS:", fontSize = 9.sp, color = Color.Gray)
                        Text("ZAR ${formatDecimal(effectiveCogsPerG)}/g", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AmberColor)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total COGS Deduction:", fontSize = 9.sp, color = Color.Gray)
                        Text("ZAR ${formatDecimal(totalTransactionCogs)}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AmberColor)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Calculated Net Gain:", fontSize = 9.sp, color = Color.Gray)
                        Text("+ZAR ${formatDecimal(derivedNetGain)}", fontSize = 9.sp, fontWeight = FontWeight.Black, color = EmeraldColor)
                    }
                }
            }

            // Digital photo witness capture button
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.LightGray),
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (resultImageBase64 != null) "WITNESS LINKED OK" else "RECEIPT SNAPSHOT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = {
                    val q = massSold.toDoubleOrNull() ?: 0.0
                    val tot = priceSelected.toDoubleOrNull() ?: 0.0

                    if (selectedVaultId.isEmpty() && resultImageBase64 == null && description.isEmpty()) {
                        viewModel.showToast("Exception: Select Vault line OR attach verification content.")
                        return@Button
                    }
                    if (q <= 0.0 || tot <= 0.0) {
                        viewModel.showToast("Exception: Values must exceed zero threshold.")
                        return@Button
                    }

                    // Perform decision path checking
                    if (selectedVaultId.isNotEmpty()) {
                        val vaultItem = vaultList.find { it.id.toString() == selectedVaultId }
                        if (vaultItem != null) {
                            viewModel.insertManualSale(
                                vaultId = vaultItem.id.toString(),
                                strainName = vaultItem.strain,
                                tier = vaultItem.tier,
                                waOrderId = orderId.ifEmpty { "#MANUAL-LEDGER" },
                                customerId = customerId.ifEmpty { "Walk-in" },
                                description = description.ifEmpty { "${vaultItem.strain} Sale" },
                                qty = q,
                                orderTotal = tot,
                                screenshotBase64 = resultImageBase64,
                                shouldDeductVault = true,
                                matchingVaultItem = vaultItem
                            )
                            // Clear inputs
                            selectedVaultId = ""
                            orderId = ""
                            customerId = ""
                            description = ""
                            massSold = ""
                            priceSelected = ""
                            resultImageBase64 = null
                        }
                    } else {
                        // Custom flow: Intelligent parsing
                        val guessedStrain = if (description.contains("-")) description.split("-")[0].trim() else description.trim()
                        val matchedVault = vaultList.find { it.strain.equals(guessedStrain, ignoreCase = true) }

                        if (matchedVault != null) {
                            viewModel.insertManualSale(
                                vaultId = matchedVault.id.toString(),
                                strainName = matchedVault.strain,
                                tier = matchedVault.tier,
                                waOrderId = orderId.ifEmpty { "#AUTO-EXTRACTED" },
                                customerId = customerId.ifEmpty { "Walk-in" },
                                description = description,
                                qty = q,
                                orderTotal = tot,
                                screenshotBase64 = resultImageBase64,
                                shouldDeductVault = true,
                                matchingVaultItem = matchedVault
                            )
                            // Clear
                            description = ""
                            massSold = ""
                            priceSelected = ""
                            resultImageBase64 = null
                        } else {
                            // Conflict detected. Ask user to auto register or just log cash stream!
                            conflictStrainName = guessedStrain.ifEmpty { "Automated Variety Input" }
                            val lowerDesc = description.lowercase()
                            conflictTierConfig = when {
                                lowerDesc.contains("sungrown") -> "Sungrown"
                                lowerDesc.contains("green door") -> "Green Door"
                                lowerDesc.contains("premium greenhouse") -> "Premium Greenhouse"
                                lowerDesc.contains("flavour greenhouse") -> "Flavour Greenhouse"
                                else -> "Indoor"
                            }
                            showConflictDialog = true
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldColor, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("PROCESS TRANSACTION", fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }

        // Right ledger list column
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text("RETAIL TRANSACTIONS REGISTERS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
            Divider(color = Color(0xFF1E293B), thickness = 1.dp)

            Spacer(modifier = Modifier.height(10.dp))

            if (salesList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No records closed yet.", fontSize = 11.sp, color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(salesList) { sale ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(sale.strainName.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CyanColor.copy(alpha = 0.12f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(sale.tier.uppercase(), fontSize = 6.5.sp, color = CyanColor, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("ID: ${sale.waOrderId}  |  USER: ${sale.customerId}", fontSize = 9.sp, color = Color.LightGray)
                                Text("${sale.qty}g  •  Description: \"${sale.description}\"", fontSize = 9.sp, color = Color.Gray)

                                if (sale.screenshotBase64 != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "[ VIEW DIGITAL RECEIPT WITNESS ]",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldColor,
                                        modifier = Modifier.clickable { imageViewerUrl = sale.screenshotBase64 }
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("+ZAR ${formatDecimal(sale.orderTotal)}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = EmeraldColor)
                                IconButton(onClick = { viewModel.deleteSaleEvent(sale.id, sale.strainName) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Conflict Dialog (Auto registrar prompt mockup)
    if (showConflictDialog) {
        Dialog(onDismissRequest = { showConflictDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = CardBg,
                border = BorderStroke(1.dp, GoldColor)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "VARIETY NOT IN VAULT!",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = GoldColor,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "THE VARIETY PROFILE \"$conflictStrainName\" WAS NOT DETECTED INSIDE THE VAULT MATRIX.\n\nCHOOSE AN OPTION BELOW:",
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        lineHeight = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                // Close conflict dialog, perform simple direct log bypass
                                val q = massSold.toDoubleOrNull() ?: 0.0
                                val tot = priceSelected.toDoubleOrNull() ?: 0.0
                                viewModel.insertManualSale(
                                    vaultId = "BYPASS_MODE",
                                    strainName = conflictStrainName,
                                    tier = conflictTierConfig,
                                    waOrderId = orderId.ifEmpty { "#BYPASS-CASH" },
                                    customerId = customerId.ifEmpty { "Walk-in" },
                                    description = description,
                                    qty = q,
                                    orderTotal = tot,
                                    screenshotBase64 = resultImageBase64,
                                    shouldDeductVault = false
                                )
                                showConflictDialog = false
                                // Clear
                                orderId = ""
                                customerId = ""
                                description = ""
                                massSold = ""
                                priceSelected = ""
                                resultImageBase64 = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("LOG ONLY", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                // Register vault on spot, then process deduction
                                val q = massSold.toDoubleOrNull() ?: 0.0
                                val tot = priceSelected.toDoubleOrNull() ?: 0.0

                                val defaultTierCogs = when (conflictTierConfig) {
                                    "Indoor" -> 22.0
                                    "Green Door" -> 20.0
                                    "Premium Greenhouse" -> 18.0
                                    "Flavour Greenhouse" -> 15.0
                                    else -> 8.5
                                }

                                // Simulate registry adding. Deduct with automatic quantity validation
                                viewModel.addToVault(conflictStrainName, 100.0, defaultTierCogs, conflictTierConfig)
                                viewModel.insertManualSale(
                                    vaultId = "AUTO_REGISTER",
                                    strainName = conflictStrainName,
                                    tier = conflictTierConfig,
                                    waOrderId = orderId.ifEmpty { "#AUTO-REGISTERED" },
                                    customerId = customerId.ifEmpty { "Walk-in" },
                                    description = description,
                                    qty = q,
                                    orderTotal = tot,
                                    screenshotBase64 = resultImageBase64,
                                    shouldDeductVault = false,
                                    explicitUnitCogs = defaultTierCogs
                                )
                                showConflictDialog = false
                                // Clear
                                orderId = ""
                                customerId = ""
                                description = ""
                                massSold = ""
                                priceSelected = ""
                                resultImageBase64 = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldColor, contentColor = Color.Black),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Text("AUTO REGISTER", fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }

    // Modal Image Preview popup
    if (imageViewerUrl != null) {
        Dialog(onDismissRequest = { imageViewerUrl = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    imageViewerUrl?.let { base64 ->
                        val cleanBase64 = base64.substringAfter("base64,")
                        val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                        val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        if (decodedImage != null) {
                            Image(
                                bitmap = decodedImage.asImageBitmap(),
                                contentDescription = "Digital receipt payload preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("Unable to parse receipt screenshot.", modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    IconButton(
                        onClick = { imageViewerUrl = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Red)
                    }
                }
            }
        }
    }
}

// ASSET ROI WITHDRAWALS SUB TAB
@Composable
fun WithdrawalsTab(viewModel: StokedViewModel, withdrawalsList: List<Withdrawal>, vaultList: List<VaultItem>) {
    val currentBalance by viewModel.roiBalance.collectAsStateWithLifecycle()
    var selectedVaultItem by remember { mutableStateOf<VaultItem?>(null) }
    var massInput by remember { mutableStateOf("") }
    var valBaseInput by remember { mutableStateOf("") }
    var referenceInput by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }

    val enteredVal = valBaseInput.toDoubleOrNull() ?: 0.0
    val isCreditExceeded = enteredVal > currentBalance

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Logging Form
        Column(
            modifier = Modifier
                .width(260.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("LOG MANUAL ASSET HANDOVER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), letterSpacing = 0.5.sp)
            Text("DEDUCTS VALUE FROM SELECTED GENERAL INVENTORY LINE AND POSTS TO ROI PORTFOLIO TIMELINE.", fontSize = 9.sp, color = Color.Gray, lineHeight = 13.sp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0C101B))
                    .border(1.dp, if (isCreditExceeded) Color.Red else CyanColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AVAILABLE CREDIT:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray, fontFamily = FontFamily.Monospace)
                    Text("ZAR ${formatDecimal(currentBalance)}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (isCreditExceeded) Color.Red else EmeraldColor, fontFamily = FontFamily.Monospace)
                }
            }

            if (isCreditExceeded) {
                Text(
                    text = "TRANSACTION BLOCKED: Valuation exceeds available credit limit.",
                    fontSize = 8.sp,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { isExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (selectedVaultItem != null) "${selectedVaultItem?.strain?.uppercase()} (${selectedVaultItem?.weight}g left)" else "SELECT VAULT ITEM LINE...",
                        fontSize = 9.sp,
                        color = Color.White
                    )
                }
                DropdownMenu(
                    expanded = isExpanded,
                    onDismissRequest = { isExpanded = false }
                ) {
                    vaultList.forEach { v ->
                        DropdownMenuItem(
                            text = { Text("${v.strain.uppercase()} (${v.weight}g left)", fontSize = 9.sp) },
                            onClick = {
                                selectedVaultItem = v
                                isExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = referenceInput,
                onValueChange = { referenceInput = it },
                label = { Text("Optional Custom Reference / Note", fontSize = 9.sp) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = massInput,
                    onValueChange = { massInput = it },
                    label = { Text("Mass Deduct (g)", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = valBaseInput,
                    onValueChange = { valBaseInput = it },
                    label = { Text("Valuation ZAR", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = {
                    val m = massInput.toDoubleOrNull() ?: 0.0
                    val v = valBaseInput.toDoubleOrNull() ?: 0.0
                    val vaultItem = selectedVaultItem

                    if (vaultItem == null) {
                        viewModel.showToast("Verification Error: No vault selection line mapped.")
                        return@Button
                    }
                    if (m <= 0.0 || v <= 0.0) {
                        viewModel.showToast("Verification Error: Values must be valid numbers higher than 0.")
                        return@Button
                    }
                    if (v > currentBalance) {
                        viewModel.showToast("TRANSACTION BLOCKED: Valuation exceeds available credit limit.")
                        return@Button
                    }

                    if (vaultItem.weight < m) {
                        viewModel.showToast("Exception: Vault holding has insufficient mass.")
                        return@Button
                    }

                    viewModel.logAssetWithdrawal(vaultItem, m, v, referenceInput)
                    // Reset
                    selectedVaultItem = null
                    massInput = ""
                    valBaseInput = ""
                    referenceInput = ""
                },
                enabled = !isCreditExceeded,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444), 
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF1E293B),
                    disabledContentColor = Color.Gray
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("PROCESS OUTFLOW WITHDRAWAL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Ledger Display List
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text("ASSET OUTFLOW WITHDRAWALS LEDGER", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
            Divider(color = Color(0xFF1E293B), thickness = 1.dp)

            Spacer(modifier = Modifier.height(10.dp))

            if (withdrawalsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No withdrawal records processed.", fontSize = 11.sp, color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(withdrawalsList) { w ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(w.strain.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("${w.weight}g @ ZAR ${formatDecimal(w.price)}/g", fontSize = 9.sp, color = Color.LightGray)
                                if (w.note.isNotBlank() && w.note != "No Ref") {
                                    Text("Ref: \"${w.note}\"", fontSize = 9.sp, color = Color.DarkGray)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("-ZAR ${formatDecimal(w.value)}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { viewModel.deleteWithdrawal(w.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// CONSIGNMENTS TAB
@Composable
fun ConsignmentsTab(viewModel: StokedViewModel, consignmentsList: List<Consignment>) {
    var supplierInput by remember { mutableStateOf("") }
    var totalMassInput by remember { mutableStateOf("") }
    var owedCpgInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("CONSIGNMENT WHOLESALE LOOPS", fontSize = 16.sp, fontWeight = FontWeight.Black, color = GoldColor)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Creation form
            Column(
                modifier = Modifier
                    .width(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("INITIALIZE WHOLESALE BATCH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldColor)

                OutlinedTextField(
                    value = supplierInput,
                    onValueChange = { supplierInput = it },
                    label = { Text("Supplier Identification", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = totalMassInput,
                    onValueChange = { totalMassInput = it },
                    label = { Text("Mass Received Allocation (g)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = owedCpgInput,
                    onValueChange = { owedCpgInput = it },
                    label = { Text("Cost Owed Per Gram Base (ZAR)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val m = totalMassInput.toDoubleOrNull() ?: 0.0
                        val cost = owedCpgInput.toDoubleOrNull() ?: 0.0
                        if (supplierInput.isNotBlank() && m > 0.0 && cost > 0.0) {
                            viewModel.addConsignment(supplierInput, m, cost)
                            supplierInput = ""
                            totalMassInput = ""
                            owedCpgInput = ""
                        } else {
                            viewModel.showToast("Verification Error: Values must be valid.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldColor, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("START LOOP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Cards display grid
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text("ACTIVE CONSIGNMENTS BATCH MONITOR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))

                if (consignmentsList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No consignment batch contracts detected.", fontSize = 11.sp, color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(consignmentsList) { c ->
                            val remainingG = maxOf(0.0, c.weight - c.soldWeight)
                            val totalOwed = c.soldWeight * c.owedCpg
                            val netProfit = c.revenueRealized - totalOwed

                            var sellDeltaQty by remember { mutableStateOf("") }
                            var sellDeltaRev by remember { mutableStateOf("") }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(c.supplier.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        IconButton(onClick = { viewModel.deleteConsignment(c.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("REMAINING: ${formatDecimal(remainingG)}g left / ${c.weight}g total", fontSize = 9.sp, color = Color.LightGray)
                                        Text("NET: ZAR ${formatDecimal(netProfit)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (netProfit >= 0.0) EmeraldColor else Color.Red)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = sellDeltaQty,
                                            onValueChange = { sellDeltaQty = it },
                                            placeholder = { Text("Qty Sold (g)", fontSize = 8.sp) },
                                            modifier = Modifier.weight(1f),
                                            textStyle = TextStyle(fontSize = 10.sp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                        OutlinedTextField(
                                            value = sellDeltaRev,
                                            onValueChange = { sellDeltaRev = it },
                                            placeholder = { Text("ZAR Collected", fontSize = 8.sp) },
                                            modifier = Modifier.weight(1f),
                                            textStyle = TextStyle(fontSize = 10.sp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                        )
                                        Button(
                                            onClick = {
                                                val q = sellDeltaQty.toDoubleOrNull() ?: 0.0
                                                val r = sellDeltaRev.toDoubleOrNull() ?: 0.0
                                                if (q > 0.0 && r > 0.0) {
                                                    viewModel.updateConsignmentDelta(c, q, r)
                                                    sellDeltaQty = ""
                                                    sellDeltaRev = ""
                                                } else {
                                                    viewModel.showToast("Delta format error.")
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7), contentColor = Color.White),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxHeight(),
                                            contentPadding = PaddingValues(horizontal = 12.dp)
                                        ) {
                                            Text("POST", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// FLYER TEMPLATE VISUALIZER SUB TAB
@Composable
fun FlyerTab(vaultList: List<VaultItem>) {
    val currentDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US).format(Date())

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("LUXURY BRAND FLYER ARCHITECTURE", fontSize = 16.sp, fontWeight = FontWeight.Black, color = GoldColor)

        // Premium simulated flyer paper rendering
        Box(
            modifier = Modifier
                .width(360.dp)
                .heightIn(min = 400.dp, max = 550.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF030303))
                .border(2.dp, GoldColor, RoundedCornerShape(16.dp))
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Flyer Header Block
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "STOKED⁴T",
                        color = GoldColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        letterSpacing = 6.sp
                    )
                    Text(
                        text = "LUXURY • ACCESS • EXCLUSIVITY",
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "CURATED CONNOISSEUR MENU",
                        color = GoldColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = currentDate.uppercase(),
                        color = Color.DarkGray,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Divider(color = GoldColor, thickness = 1.dp)

                // Render products grouped by tier beautifully
                if (vaultList.isEmpty()) {
                    Text(
                        text = "NO VALUED VARIETIES COMMITTED FOR EXPORT.",
                        fontSize = 10.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 40.dp)
                    )
                } else {
                    val grouped = vaultList.groupBy { it.tier }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        grouped.forEach { (tier, items) ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "— ${tier.uppercase()} —",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GoldColor,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Left
                                )

                                items.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "• ${item.strain.uppercase()}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF1C1304))
                                                .border(1.dp, GoldColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "ZAR ${(formatDecimal(item.costPerG * 2.5))}/g",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = GoldColor,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Divider(color = GoldColor, thickness = 1.dp)

                Text(
                    text = "STRICTLY TOKEN ENTRY REQUIREMENTS APPLY.\nSTRICTLY OVER 21 SECURE PROTOCOL.",
                    fontSize = 7.5.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// AI INTEL TAB WITH MARGIN & PORTFOLIO CALCULATIONS & CURATED TREND FEED
@Composable
fun AIIntelTab(viewModel: StokedViewModel, queryResult: String?, isLoading: Boolean) {
    var subTab by remember { mutableStateOf("trends") } // "trends" or "analyst"
    
    val intelligenceFeed by viewModel.intelligenceFeed.collectAsStateWithLifecycle()
    val isGeneratingReport by viewModel.isGeneratingReport.collectAsStateWithLifecycle()
    
    var customQueryInput by remember { mutableStateOf("") }
    var promptQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Row with dual segment toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "STOKED COGNITIVE INTELLIGENCE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = GoldColor,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "AI-Powered Market Audits & Trend Feed Analytics",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            // Sub-Tab Segment Switcher
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (subTab == "trends") GoldColor.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { subTab = "trends" }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "TREND FEED",
                        color = if (subTab == "trends") GoldColor else Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (subTab == "analyst") GoldColor.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { subTab = "analyst" }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "AI ANALYST",
                        color = if (subTab == "analyst") GoldColor else Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Divider(color = BorderColor, thickness = 1.dp)

        if (subTab == "trends") {
            // Curated Market Intelligence & Trend Reports Screen
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Interactive AI custom compiler block
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardBg)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(GoldColor.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = GoldColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = "COMPILE CUSTOM INTELLIGENCE BRIEF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldColor,
                                    letterSpacing = 1.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            Text(
                                text = "Submit a custom theme, strain, or geography to direct the AI cognitive analyst to compile a tailored trend profile.",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                lineHeight = 14.sp
                            )

                            OutlinedTextField(
                                value = customQueryInput,
                                onValueChange = { customQueryInput = it },
                                placeholder = { 
                                    Text(
                                        text = "e.g. Frankfurt retail import demand analysis...", 
                                        fontSize = 11.sp, 
                                        color = Color.DarkGray
                                    ) 
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("custom_intel_input"),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = TextStyle(fontSize = 12.sp, color = Color.White)
                            )

                            // Suggested prompts tags row
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val prompts = listOf(
                                    "Purple Sunset Luxury Profile",
                                    "Frankfurt wholesale trends",
                                    "Greenhouse vs Indoor margins"
                                )
                                prompts.forEach { itemText ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color.White.copy(alpha = 0.03f))
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                                            .clickable { customQueryInput = itemText }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = itemText,
                                            fontSize = 9.sp,
                                            color = Color.LightGray,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.generateIntelligenceReport(customQueryInput)
                                    customQueryInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldColor, 
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("compile_intel_button"),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isGeneratingReport
                            ) {
                                if (isGeneratingReport) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp), 
                                        color = Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Star, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "INITIALIZE COGNITIVE ANALYSIS", 
                                        fontSize = 10.sp, 
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Header for the Feed List
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REAL-TIME CURATED TRENDS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF0F172A))
                                .border(1.dp, CyanColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE TELEMETRY FEED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                if (intelligenceFeed.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info, 
                                    contentDescription = null, 
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "NO COGNITIVE ANALYSIS IN PIPELINE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                } else {
                    items(intelligenceFeed, key = { it.id }) { report ->
                        IntelligenceReportCard(report = report)
                    }
                }
            }
        } else {
            // Original SECURED AI ANALYST Q&A Terminal View (so they don't lose any existing features!)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "INTERACT WITH SECURED COGNITIVE ENGINE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldColor
                )

                OutlinedTextField(
                    value = promptQuery,
                    onValueChange = { promptQuery = it },
                    label = { Text("What information matrix do you require compiled?", fontSize = 10.sp) },
                    placeholder = { Text("e.g. Conduct a full margin comparison and highlight high selling variety tiers...", fontSize = 9.sp) },
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    maxLines = 3
                )

                Button(
                    onClick = { viewModel.executeAIAnalystQuery(promptQuery) },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldColor, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                    } else {
                        Text("INITIALIZE ANALYST MATRIX QUERY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Results Container
                if (queryResult != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black)
                            .border(1.dp, GoldColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AI TELEMETRY FEED RESPONSE:",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = queryResult ?: "",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// PREMIUM COMPOSABLE FOR INDIVIDUAL INTELLIGENCE REPORT CARDS
@Composable
fun IntelligenceReportCard(report: IntelligenceReport) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp, 
                if (report.isUserGenerated) GoldColor.copy(alpha = 0.3f) else BorderColor, 
                RoundedCornerShape(12.dp)
            )
            .clickable { expanded = !expanded }
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top category tags & Vector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (report.isUserGenerated) GoldColor.copy(alpha = 0.1f) 
                                else Color.White.copy(alpha = 0.04f)
                            )
                            .border(
                                1.dp, 
                                if (report.isUserGenerated) GoldColor.copy(alpha = 0.3f) 
                                else Color.White.copy(alpha = 0.1f), 
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = report.category.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (report.isUserGenerated) GoldColor else Color.LightGray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (report.isUserGenerated) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF0F172A))
                                .border(1.dp, CyanColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "USER GENERATED",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Black,
                                color = CyanColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Trend Vector Badge with subtle up-arrow
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(Color(0xFF061A12))
                        .border(1.dp, EmeraldColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp, 
                        contentDescription = null, 
                        tint = EmeraldColor,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = report.trendVector.uppercase(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Headline
            Text(
                text = report.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 18.sp,
                fontFamily = FontFamily.Serif
            )

            // Abstract/Summary
            Text(
                text = report.summary,
                fontSize = 11.sp,
                color = Color.LightGray,
                lineHeight = 15.sp
            )

            // Confidence Index and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Confidence Index:",
                        fontSize = 9.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${report.confidence}%",
                        fontSize = 9.sp,
                        color = if (report.confidence >= 95) EmeraldColor else AmberColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    // Simple little confidence block visual
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (i in 1..5) {
                            val activeBlocks = (report.confidence - 75) / 5
                            val color = if (i <= activeBlocks) {
                                if (report.confidence >= 95) EmeraldColor else AmberColor
                            } else Color.DarkGray
                            Box(
                                modifier = Modifier
                                    .size(width = 6.dp, height = 4.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(color)
                            )
                        }
                    }
                }

                Text(
                    text = if (expanded) "Tap to collapse" else "Tap to expand deep-dive",
                    fontSize = 9.sp,
                    color = GoldColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Extended Deep Dive
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ANALYTICAL INTELLIGENCE BRIEFING:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanColor,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = report.fullContent,
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// PUBLIC BRAND SHOWCASE & ACCESS DENIED PORTAL
@Composable
fun PublicLandingPage(
    errorMessage: String?,
    onCloseError: () -> Unit,
    onEnterPortal: () -> Unit,
    vaultList: List<VaultItem>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp)
    ) {
        // Subtle ambient space glow overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = GoldColor.copy(alpha = 0.04f),
                radius = size.width,
                center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 3)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text(
                    text = "STOKED⁴T",
                    color = GoldColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 6.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "CURATOR PORTAL",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Description and Curation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "Curators of premium garden craft and organic exports. Connecting elite global distributors with verified connoisseur supply.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "— EXCLUSIVE VERIFIED DISTRIBUTIONS ONLY —",
                        color = GoldColor.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Public Preview Card
                Card(
                    modifier = Modifier
                        .widthIn(max = 450.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "STATION VAULT STOCK SELECTION PREVIEW",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldColor,
                            letterSpacing = 1.5.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        if (vaultList.isEmpty()) {
                            Text(
                                text = "RESTOCK OF CONNOISSEUR VARIETIES IN PROCESS.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            // Take top 3 items to show as a premium preview
                            vaultList.take(3).forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = item.strain.uppercase(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = item.tier.uppercase(),
                                            fontSize = 8.sp,
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF141F1A))
                                            .border(1.dp, EmeraldColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "VERIFIED IN STOCK",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CTAs and Footer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onEnterPortal,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(52.dp)
                        .testTag("enter_secure_gateway"),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldColor, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SECURE STAGING V2 GATEWAY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "© 2024 STOKED4T. ALL WORLDWIDE PRIVATE RIGHTS RESERVED.",
                    fontSize = 8.sp,
                    color = Color.DarkGray,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Error message popup representing 'Access Denied'
        if (errorMessage != null) {
            Dialog(onDismissRequest = onCloseError) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF160909))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0x1AEF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Text(
                            text = "ACCESS DENIED",
                            color = Color(0xFFEF4444),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = errorMessage ?: "",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Button(
                            onClick = onCloseError,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text("ACKNOWLEDGE SECURITY PROTOCOL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// CINEMATIC SECURE INTERFACE (MIMICKING ARCHITECTURE OF INDEX_STAGING_V2.HTML)
@Composable
fun SecureGatewayPage(
    onBackToPublic: () -> Unit,
    onValidateToken: (String) -> Unit
) {
    var tokenInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Cinematic Background representation with horizontal & vertical grids
        Canvas(modifier = Modifier.fillMaxSize()) {
            val numX = 12
            val numY = 24
            for (i in 0..numX) {
                val x = (size.width / numX) * i
                drawLine(
                    color = Color.White.copy(alpha = 0.02f),
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, size.height),
                    strokeWidth = 1f
                )
            }
            for (i in 0..numY) {
                val y = (size.height / numY) * i
                drawLine(
                    color = Color.White.copy(alpha = 0.02f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1f
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header bar mimicking design header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FIREBASE GATEWAY",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberColor.copy(alpha = 0.8f),
                        letterSpacing = 3.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "STOKED",
                            fontWeight = FontWeight.Light,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontFamily = FontFamily.Serif,
                            fontSize = 20.sp,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "4",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "T",
                            fontWeight = FontWeight.Light,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontFamily = FontFamily.Serif,
                            fontSize = 20.sp,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }

                // Pulse indicator
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(EmeraldColor)
                    )
                }
            }

            // Main Core Area with luxury title and token field
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Large cinematic tagline
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Staging Environment V2",
                        color = Color(0xFF94A3B8), // slate-400
                        fontSize = 9.sp,
                        letterSpacing = 2.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Luxury.\nAccess.\nExclusivity.",
                        color = Color.White,
                        fontSize = 38.sp,
                        lineHeight = 36.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Token validation system input unit
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = tokenInput,
                                onValueChange = { tokenInput = it },
                                placeholder = {
                                    Text(
                                        text = "ENTER ACCESS TOKEN",
                                        color = Color(0xFF475569),
                                        fontSize = 11.sp,
                                        letterSpacing = 1.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("token_input_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    letterSpacing = 2.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )

                            // Validate submit arrow
                            IconButton(
                                onClick = { onValidateToken(tokenInput) },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AmberColor)
                                    .size(36.dp)
                                    .testTag("validate_token_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Validate Access",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "VALIDATING ACCESS PROTOCOLS VIA STOKED4T.WEB.APP",
                        color = Color(0xFF64748B), // slate-500
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Quick Access routing footer cards + metadata
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Investor and Admin demo helper buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Investor Demo Selector
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                tokenInput = "INVESTOR2026"
                                onValidateToken("INVESTOR2026")
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "INVESTOR",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }

                    // Admin Demo Selector
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                tokenInput = "ADMIN2026"
                                onValidateToken("ADMIN2026")
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "ADMIN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }
                }

                // System return navigation option & Staging metadata
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Back to portal landing
                    Text(
                        text = "RETURN TO BRAND HOME PORTAL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldColor,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .clickable { onBackToPublic() }
                            .padding(4.dp)
                    )

                    // Staging metadata
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "DEPLOY: STABLE",
                                color = EmeraldColor.copy(alpha = 0.8f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray)
                            )
                            Text(
                                text = "v2.0.4-staging",
                                color = Color.Gray,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "©2024 STOKED4T",
                            color = Color.DarkGray,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
