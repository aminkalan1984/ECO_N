package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.MainMenuDrawerContent
import com.example.ui.components.NotificationsSheet
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.BillScreen
import com.example.ui.screens.BillsHistoryScreen
import com.example.ui.screens.CaseScreen
import com.example.ui.screens.ChannelScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DesignScreen
import com.example.ui.screens.IncomeScreen
import com.example.ui.screens.InstallmentScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OptimizeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.QuotesScreen
import com.example.ui.screens.WizardScreen
import com.example.ui.viewmodel.AssistantViewModel
import com.example.ui.viewmodel.BillAiViewModel
import com.example.ui.viewmodel.EcoViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object RouteLogin
@Serializable data object RouteDashboard
@Serializable data object RouteBill
@Serializable data object RouteBills
@Serializable data object RouteOptimize
@Serializable data object RouteDesign
@Serializable data object RouteChannel
@Serializable data object RouteInstallment
@Serializable data object RouteQuotes
@Serializable data object RouteCase
@Serializable data object RouteIncome
@Serializable data object RouteWizard
@Serializable data object RouteProfile
@Serializable data object RouteAssistant

@Composable
fun EcoAppRoot(
    ecoViewModel: EcoViewModel,
    billAiViewModel: BillAiViewModel,
    assistantViewModel: AssistantViewModel,
    navController: NavHostController = rememberNavController()
) {
    val ecoState by ecoViewModel.ecoState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var notificationsOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ecoViewModel.toastEvents.collect { msg ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteName = backStackEntry?.destination?.route.orEmpty()
    val activeKey = when {
        currentRouteName.contains("RouteDashboard") -> "dashboard"
        currentRouteName.contains("RouteBill") && !currentRouteName.contains("RouteBills") -> "bill"
        currentRouteName.contains("RouteBills") -> "bills"
        currentRouteName.contains("RouteOptimize") -> "optimize"
        currentRouteName.contains("RouteDesign") -> "design"
        currentRouteName.contains("RouteChannel") -> "channel"
        currentRouteName.contains("RouteInstallment") -> "installment"
        currentRouteName.contains("RouteQuotes") -> "quotes"
        currentRouteName.contains("RouteCase") -> "case"
        currentRouteName.contains("RouteIncome") -> "income"
        currentRouteName.contains("RouteWizard") -> "wizard"
        currentRouteName.contains("RouteProfile") -> "profile"
        currentRouteName.contains("RouteAssistant") -> "assistant"
        else -> "login"
    }

    fun navigateByKey(key: String) {
        val target: Any = when (key) {
            "login" -> RouteLogin
            "dashboard" -> RouteDashboard
            "bill" -> RouteBill
            "bills" -> RouteBills
            "optimize" -> RouteOptimize
            "design" -> RouteDesign
            "channel" -> RouteChannel
            "installment" -> RouteInstallment
            "quotes" -> RouteQuotes
            "case" -> RouteCase
            "income" -> RouteIncome
            "wizard" -> RouteWizard
            "profile" -> RouteProfile
            "assistant" -> RouteAssistant
            else -> RouteDashboard
        }
        navController.navigate(target) {
            launchSingleTop = true
        }
    }

    fun popOrDashboard() {
        if (!navController.popBackStack()) {
            navController.navigate(RouteDashboard) { launchSingleTop = true }
        }
    }

    val startDestination: Any = if (ecoState.loggedIn) RouteDashboard else RouteLogin

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = activeKey != "login",
        drawerContent = {
            MainMenuDrawerContent(
                currentRouteKey = activeKey,
                phone = ecoState.phone,
                loggedIn = ecoState.loggedIn,
                onNavigate = { key ->
                    scope.launch { drawerState.close() }
                    navigateByKey(key)
                },
                onClose = { scope.launch { drawerState.close() } },
                onLogout = {
                    scope.launch { drawerState.close() }
                    ecoViewModel.logout {
                        navController.navigate(RouteLogin) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable<RouteLogin> {
                    LoginScreen(
                        ecoViewModel = ecoViewModel,
                        onLoginSuccess = {
                            navController.navigate(RouteDashboard) {
                                popUpTo(RouteLogin) { inclusive = true }
                            }
                        }
                    )
                }
                composable<RouteDashboard> {
                    DashboardScreen(
                        ecoViewModel = ecoViewModel,
                        onNavigate = ::navigateByKey,
                        onOpenNotifications = { notificationsOpen = true },
                        onOpenMenu = { scope.launch { drawerState.open() } }
                    )
                }
                composable<RouteBill> {
                    BillScreen(
                        ecoViewModel = ecoViewModel,
                        billAiViewModel = billAiViewModel,
                        onBack = ::popOrDashboard,
                        onContinueToOptimize = { navigateByKey("optimize") }
                    )
                }
                composable<RouteBills> {
                    BillsHistoryScreen(
                        ecoViewModel = ecoViewModel,
                        onBack = ::popOrDashboard
                    )
                }
                composable<RouteOptimize> {
                    OptimizeScreen(
                        ecoViewModel = ecoViewModel,
                        onBack = ::popOrDashboard,
                        onContinueToDesign = { navigateByKey("design") }
                    )
                }
                composable<RouteDesign> {
                    DesignScreen(
                        ecoViewModel = ecoViewModel,
                        onBack = ::popOrDashboard,
                        onNavigate = ::navigateByKey
                    )
                }
                composable<RouteChannel> {
                    ChannelScreen(
                        ecoViewModel = ecoViewModel,
                        onBack = ::popOrDashboard,
                        onContinueToInstallment = { navigateByKey("installment") }
                    )
                }
                composable<RouteInstallment> {
                    InstallmentScreen(
                        ecoViewModel = ecoViewModel,
                        onBack = ::popOrDashboard,
                        onContinueToQuotes = { navigateByKey("quotes") }
                    )
                }
                composable<RouteQuotes> {
                    QuotesScreen(
                        ecoViewModel = ecoViewModel,
                        onBack = ::popOrDashboard,
                        onOrderPlaced = { navigateByKey("case") }
                    )
                }
                composable<RouteCase> {
                    CaseScreen(
                        ecoViewModel = ecoViewModel,
                        onBack = ::popOrDashboard,
                        onNavigate = ::navigateByKey
                    )
                }
                composable<RouteIncome> {
                    IncomeScreen(
                        ecoViewModel = ecoViewModel,
                        onBack = ::popOrDashboard,
                        onNavigate = ::navigateByKey
                    )
                }
                composable<RouteWizard> {
                    WizardScreen(
                        ecoViewModel = ecoViewModel,
                        onBack = ::popOrDashboard,
                        onOpenBill = { navigateByKey("bill") },
                        onCompleted = { navigateByKey("design") }
                    )
                }
                composable<RouteProfile> {
                    ProfileScreen(
                        ecoViewModel = ecoViewModel,
                        onBack = ::popOrDashboard,
                        onNavigate = ::navigateByKey,
                        onLoggedOut = {
                            navController.navigate(RouteLogin) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
                composable<RouteAssistant> {
                    AssistantScreen(
                        ecoViewModel = ecoViewModel,
                        assistantViewModel = assistantViewModel,
                        onBack = ::popOrDashboard
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 92.dp)
            )
        }
    }

    NotificationsSheet(
        open = notificationsOpen,
        onDismiss = { notificationsOpen = false },
        onNavigate = ::navigateByKey
    )
}
