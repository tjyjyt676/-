package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.KringeCrmTheme
import com.example.ui.theme.LocalCrmColors
import com.example.viewmodel.BannerTone
import com.example.viewmodel.CrmViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KringeCrmApp(viewModel: CrmViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var currentSection by remember { mutableStateOf(CrmSection.DASHBOARD) }
    var activeClient360Id by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    val allowedSections = remember(state.settings.currentRole) {
        CrmSection.entries.filter { state.settings.currentRole in it.allowedRoles }
    }

    // Ensure currentSection stays accessible when switching roles
    LaunchedEffect(state.settings.currentRole) {
        if (currentSection !in allowedSections) {
            currentSection = CrmSection.DASHBOARD
        }
    }

    LaunchedEffect(Unit) {
        viewModel.feedback.collect { fb ->
            snackbarHostState.showSnackbar(
                message = fb.message,
                duration = if (fb.tone == BannerTone.ERROR) SnackbarDuration.Long else SnackbarDuration.Short
            )
        }
    }

    // System back button returns from secondary screens to Dashboard or closes Client 360 dialog
    BackHandler(enabled = activeClient360Id != null || currentSection != CrmSection.DASHBOARD) {
        if (activeClient360Id != null) {
            activeClient360Id = null
        } else {
            currentSection = CrmSection.DASHBOARD
        }
    }

    KringeCrmTheme(
        darkTheme = state.settings.darkTheme,
        largeFont = state.settings.largeFont
    ) {
        val crmColors = LocalCrmColors.current

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isExpanded = maxWidth >= 840.dp

            val contentSlot: @Composable (PaddingValues) -> Unit = { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentSection) {
                        CrmSection.DASHBOARD -> DashboardScreen(
                            state = state,
                            onNavigate = { sec -> if (sec in allowedSections) currentSection = sec },
                            onOpenClient = { activeClient360Id = it },
                            onSelectSpecialist = viewModel::selectActiveSpecialist
                        )
                        CrmSection.KANBAN -> KanbanScreen(
                            state = state,
                            onMoveDeal = viewModel::moveDealToStage,
                            onCreateDeal = viewModel::createDeal,
                            onAttachTourbase = viewModel::attachTourbaseToDeal,
                            onOpenClient = { activeClient360Id = it }
                        )
                        CrmSection.CLIENTS -> ClientsScreen(
                            state = state,
                            onOpenClient = { activeClient360Id = it },
                            onAddClient = viewModel::addClient
                        )
                        CrmSection.CALENDAR -> CalendarScreen(
                            state = state,
                            hoursUntil = viewModel::hoursUntil,
                            onCreateBooking = viewModel::createBooking,
                            onCancelBooking = viewModel::cancelBooking,
                            onOpenClient = { activeClient360Id = it }
                        )
                        CrmSection.TOURBASES -> TourbasesScreen(
                            state = state,
                            onAttachTourbaseToDeal = viewModel::attachTourbaseToDeal
                        )
                        CrmSection.CATALOG -> CatalogVkScreen(
                            state = state,
                            onUpsertProgram = viewModel::upsertProgram,
                            onToggleArchiveProgram = viewModel::toggleArchiveProgram,
                            onAdjustStock = viewModel::adjustVkProductStock,
                            onCreateVkOrder = viewModel::createVkOrder
                        )
                        CrmSection.FINANCE -> FinanceScreen(
                            state = state,
                            onMarkInstallmentPaid = viewModel::markInstallmentStepPaid,
                            onCancelPaidPayment = viewModel::cancelPaidPayment,
                            onSimulateAutoPayment = viewModel::simulateAutoAppPayment
                        )
                        CrmSection.TEAM -> TeamScreen(
                            state = state,
                            onAddSpecialist = viewModel::addSpecialist
                        )
                        CrmSection.CHAT -> SupportChatScreen(
                            state = state,
                            onSendReply = viewModel::sendTicketReply,
                            onUpdateStatus = viewModel::updateTicketStatus,
                            onOpenClient = { activeClient360Id = it }
                        )
                        CrmSection.PUSH_VK -> PushVkScreen(
                            state = state,
                            onSendPush = viewModel::sendPushCampaign,
                            onAddVkBroadcast = viewModel::addVkBroadcastWithAutoPush
                        )
                        CrmSection.REVIEWS -> ReviewsScreen(
                            state = state,
                            onTogglePublish = viewModel::togglePublishReview
                        )
                        CrmSection.ANALYTICS -> AnalyticsScreen(
                            state = state,
                            buildClientsCsv = viewModel::buildClientsCsv,
                            buildFinanceCsv = viewModel::buildFinanceCsv
                        )
                        CrmSection.AUDIT -> AuditSettingsScreen(
                            state = state,
                            onToggleTheme = viewModel::toggleDarkTheme,
                            onToggleFont = viewModel::toggleLargeFont,
                            onResetDemoData = viewModel::resetDemoData
                        )
                    }
                }
            }

            if (isExpanded) {
                // Tablet / Desktop Adaptive Layout with Permanent Sidebar
                Row(modifier = Modifier.fillMaxSize()) {
                    SidebarDrawerContent(
                        allowedSections = allowedSections,
                        currentSection = currentSection,
                        currentRole = state.settings.currentRole,
                        onSelectSection = { currentSection = it },
                        onResetClick = { showResetDialog = true },
                        modifier = Modifier
                            .width(268.dp)
                            .fillMaxHeight()
                            .background(crmColors.deepHeader)
                            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
                    )
                    VerticalDivider(color = crmColors.subtleBorder)
                    Scaffold(
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.background,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            CrmTopHeader(
                                isExpanded = true,
                                currentRole = state.settings.currentRole,
                                darkTheme = state.settings.darkTheme,
                                largeFont = state.settings.largeFont,
                                onOpenMenu = {},
                                onSelectRole = viewModel::switchRole,
                                onToggleTheme = viewModel::toggleDarkTheme,
                                onToggleFont = viewModel::toggleLargeFont
                            )
                        },
                        content = contentSlot
                    )
                }
            } else {
                // Mobile / Compact Adaptive Layout with Collapsible ModalNavigationDrawer + Bottom Bar
                val quickBottomSections = remember(allowedSections) {
                    val preferred = listOf(
                        CrmSection.DASHBOARD,
                        CrmSection.KANBAN,
                        CrmSection.CLIENTS,
                        CrmSection.CALENDAR,
                        CrmSection.FINANCE,
                        CrmSection.REVIEWS
                    )
                    preferred.filter { it in allowedSections }.take(4)
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = crmColors.deepHeader,
                            modifier = Modifier.width(300.dp)
                        ) {
                            SidebarDrawerContent(
                                allowedSections = allowedSections,
                                currentSection = currentSection,
                                currentRole = state.settings.currentRole,
                                onSelectSection = { sec ->
                                    currentSection = sec
                                    scope.launch { drawerState.close() }
                                },
                                onResetClick = {
                                    scope.launch { drawerState.close() }
                                    showResetDialog = true
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                ) {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            CrmTopHeader(
                                isExpanded = false,
                                currentRole = state.settings.currentRole,
                                darkTheme = state.settings.darkTheme,
                                largeFont = state.settings.largeFont,
                                onOpenMenu = { scope.launch { drawerState.open() } },
                                onSelectRole = viewModel::switchRole,
                                onToggleTheme = viewModel::toggleDarkTheme,
                                onToggleFont = viewModel::toggleLargeFont
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = crmColors.deepHeader,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                quickBottomSections.forEach { sec ->
                                    NavigationBarItem(
                                        selected = currentSection == sec,
                                        onClick = { currentSection = sec },
                                        icon = { Icon(sectionIcon(sec), contentDescription = sec.title) },
                                        label = { Text(sec.shortTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        modifier = Modifier.testTag("bottom_nav_${sec.route}")
                                    )
                                }
                                NavigationBarItem(
                                    selected = currentSection !in quickBottomSections,
                                    onClick = { scope.launch { drawerState.open() } },
                                    icon = { Icon(Icons.Filled.Apps, contentDescription = "Все разделы") },
                                    label = { Text("Меню", maxLines = 1) },
                                    modifier = Modifier.testTag("bottom_nav_all_modules")
                                )
                            }
                        },
                        content = contentSlot
                    )
                }
            }
        }

        // Global Client 360 Modal Sheet
        activeClient360Id?.let { clId ->
            Client360Dialog(
                clientId = clId,
                state = state,
                onDismiss = { activeClient360Id = null },
                onTogglePdn = viewModel::togglePdnConsent,
                onAddNote = viewModel::addSpecialistNote
            )
        }

        // Quick Reset Demo Data confirmation dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Сбросить демо-данные?") },
                text = { Text("Восстановить исходные 12 турбаз, 4 специалистов, 12 клиентов, сделки, календарь, рассрочки и отзывы «Кринж & Co»?") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.resetDemoData()
                        showResetDialog = false
                    }) { Text("Сбросить") }
                },
                dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Отмена") } }
            )
        }
    }
}

@Composable
private fun CrmTopHeader(
    isExpanded: Boolean,
    currentRole: UserRole,
    darkTheme: Boolean,
    largeFont: Boolean,
    onOpenMenu: () -> Unit,
    onSelectRole: (UserRole) -> Unit,
    onToggleTheme: () -> Unit,
    onToggleFont: () -> Unit
) {
    val crmColors = LocalCrmColors.current
    Surface(
        color = crmColors.deepHeader,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (!isExpanded) {
                        IconButton(
                            onClick = onOpenMenu,
                            modifier = Modifier.testTag("open_drawer_btn")
                        ) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.cd_open_menu))
                        }
                        BrandLogoHeader(compact = true)
                    } else {
                        RoleSwitcherStrip(
                            currentRole = currentRole,
                            onSelectRole = onSelectRole
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFont,
                        modifier = Modifier.testTag("toggle_font_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FormatSize,
                            contentDescription = stringResource(R.string.cd_toggle_font),
                            tint = if (largeFont) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("toggle_theme_btn")
                    ) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = stringResource(R.string.cd_toggle_theme),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (!isExpanded) {
                // Quick Role Switcher in header for compact screens
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoleSwitcherStrip(
                        currentRole = currentRole,
                        onSelectRole = onSelectRole
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarDrawerContent(
    allowedSections: List<CrmSection>,
    currentSection: CrmSection,
    currentRole: UserRole,
    onSelectSection: (CrmSection) -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val crmColors = LocalCrmColors.current
    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        BrandLogoHeader(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
        Spacer(Modifier.height(8.dp))
        StatusPill(
            text = "Роль: ${currentRole.title}",
            containerColor = crmColors.warningContainer,
            contentColor = crmColors.warning,
            icon = Icons.Filled.Badge,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = crmColors.subtleBorder.copy(alpha = 0.6f))
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(allowedSections) { sec ->
                val selected = sec == currentSection
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = sec.title,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    selected = selected,
                    onClick = { onSelectSection(sec) },
                    icon = { Icon(sectionIcon(sec), contentDescription = null, modifier = Modifier.size(20.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("drawer_nav_${sec.route}")
                )
            }
        }

        HorizontalDivider(color = crmColors.subtleBorder.copy(alpha = 0.6f))
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onResetClick,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sidebar_reset_demo_btn")
        ) {
            Icon(Icons.Filled.RestartAlt, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.reset_demo_data))
        }
    }
}
