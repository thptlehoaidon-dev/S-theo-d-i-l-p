package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.auth.signOutFromGoogle
import com.example.data.SchoolRepository
import com.example.data.UiState
import com.example.ui.RoleReportViewModel
import com.example.ui.SchoolViewModel
import com.example.ui.components.RoomDetailSheet
import com.example.ui.theme.AcademicBlue

enum class MainAppTab {
    PUBLIC_DASHBOARD,
    ROLE_REPORT,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShellScreen(
    userEmail: String?,
    schoolViewModel: SchoolViewModel,
    roleReportViewModel: RoleReportViewModel,
    modifier: Modifier = Modifier,
) {
    val roomsState by schoolViewModel.roomsState.collectAsStateWithLifecycle()
    val seedError by schoolViewModel.seedError.collectAsStateWithLifecycle()
    val selectedFloor by schoolViewModel.selectedFloor.collectAsStateWithLifecycle()
    val selectedRoomId by schoolViewModel.selectedRoomId.collectAsStateWithLifecycle()
    val roleUiState by roleReportViewModel.uiState.collectAsStateWithLifecycle()

    var activeTab by rememberSaveable { mutableStateOf(MainAppTab.PUBLIC_DASHBOARD) }
    var accountMenuExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    LaunchedEffect(roleUiState.feedbackMessage) {
        val msg = roleUiState.feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            roleReportViewModel.consumeFeedback()
        }
    }

    // BackHandler for secondary tab / role session
    BackHandler(enabled = activeTab == MainAppTab.ROLE_REPORT) {
        if (roleUiState.activeSession != null) {
            roleReportViewModel.logoutRole()
        } else {
            activeTab = MainAppTab.PUBLIC_DASHBOARD
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 680.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.school_header_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            if (!userEmail.isNullOrBlank()) {
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { accountMenuExpanded = true },
                            modifier = Modifier.testTag("account_menu_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = "Tài khoản Google",
                                tint = AcademicBlue,
                            )
                        }
                        DropdownMenu(
                            expanded = accountMenuExpanded,
                            onDismissRequest = { accountMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sign_out_label)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    accountMenuExpanded = false
                                    signOutFromGoogle(
                                        credentialManager = credentialManager,
                                        scope = scope,
                                    )
                                },
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                    ) {
                        NavigationBarItem(
                            selected = activeTab == MainAppTab.PUBLIC_DASHBOARD,
                            onClick = { activeTab = MainAppTab.PUBLIC_DASHBOARD },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == MainAppTab.PUBLIC_DASHBOARD) {
                                        Icons.Rounded.Dashboard
                                    } else {
                                        Icons.Outlined.Dashboard
                                    },
                                    contentDescription = stringResource(R.string.tab_public_label),
                                )
                            },
                            label = { Text(stringResource(R.string.tab_public_label)) },
                            modifier = Modifier.testTag("nav_tab_public"),
                        )
                        NavigationBarItem(
                            selected = activeTab == MainAppTab.ROLE_REPORT,
                            onClick = { activeTab = MainAppTab.ROLE_REPORT },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == MainAppTab.ROLE_REPORT) {
                                        Icons.Rounded.EditNote
                                    } else {
                                        Icons.Outlined.EditNote
                                    },
                                    contentDescription = stringResource(R.string.tab_report_label),
                                )
                            },
                            label = { Text(stringResource(R.string.tab_report_label)) },
                            modifier = Modifier.testTag("nav_tab_report"),
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        Spacer(modifier = Modifier.weight(0.1f))
                        NavigationRailItem(
                            selected = activeTab == MainAppTab.PUBLIC_DASHBOARD,
                            onClick = { activeTab = MainAppTab.PUBLIC_DASHBOARD },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == MainAppTab.PUBLIC_DASHBOARD) {
                                        Icons.Rounded.Dashboard
                                    } else {
                                        Icons.Outlined.Dashboard
                                    },
                                    contentDescription = stringResource(R.string.tab_public_label),
                                )
                            },
                            label = { Text(stringResource(R.string.tab_public_label)) },
                        )
                        NavigationRailItem(
                            selected = activeTab == MainAppTab.ROLE_REPORT,
                            onClick = { activeTab = MainAppTab.ROLE_REPORT },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == MainAppTab.ROLE_REPORT) {
                                        Icons.Rounded.EditNote
                                    } else {
                                        Icons.Outlined.EditNote
                                    },
                                    contentDescription = stringResource(R.string.tab_report_label),
                                )
                            },
                            label = { Text(stringResource(R.string.tab_report_label)) },
                        )
                    }
                }

                when (activeTab) {
                    MainAppTab.PUBLIC_DASHBOARD -> PublicDashboardScreen(
                        roomsState = roomsState,
                        seedError = seedError,
                        selectedFloor = selectedFloor,
                        onSelectFloor = schoolViewModel::selectFloor,
                        onRoomClick = schoolViewModel::openRoomDetail,
                        onRetry = schoolViewModel::ensureSchoolInitialized,
                        modifier = Modifier.weight(1f),
                    )

                    MainAppTab.ROLE_REPORT -> RoleReportScreen(
                        roomsState = roomsState,
                        roleUiState = roleUiState,
                        viewModel = roleReportViewModel,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    // Room Detail Modal Bottom Sheet (reactively reflects live updates from Firestore)
    val openRoom = (roomsState as? UiState.Success)?.data?.firstOrNull { it.id == selectedRoomId }
    if (openRoom != null) {
        RoomDetailSheet(
            room = openRoom,
            todayDate = SchoolRepository.todayDateString(),
            onDismiss = schoolViewModel::closeRoomDetail,
        )
    }
}
