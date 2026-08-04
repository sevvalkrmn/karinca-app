package com.karinca.app.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.karinca.app.ui.auth.ForgotPasswordScreen
import com.karinca.app.ui.auth.OtpVerifyScreen
import com.karinca.app.ui.auth.ResetPasswordScreen
import com.karinca.app.ui.auth.SignInScreen
import com.karinca.app.ui.auth.SignUpScreen
import com.karinca.app.ui.auth.WelcomeScreen
import com.karinca.app.ui.categories.CategoriesScreen
import com.karinca.app.ui.coach.WeeklyReportScreen
import com.karinca.app.ui.consent.KvkkConsentScreen
import com.karinca.app.ui.goals.GoalsScreen
import com.karinca.app.ui.home.HomeScreen
import com.karinca.app.ui.onboarding.OnboardingScreen
import com.karinca.app.ui.profile.KumbaramScreen
import com.karinca.app.ui.profile.ProfileScreen
import com.karinca.app.ui.profile.ProfileSetupScreen
import com.karinca.app.ui.profile.SubscriptionsScreen
import com.karinca.app.ui.transactions.AddTransactionScreen
import com.karinca.app.ui.transactions.AnalysisScreen
import com.karinca.app.ui.transactions.CategoryDetailScreen

@Composable
fun KarincaNavHost() {
    val entryViewModel: AppEntryViewModel = hiltViewModel()
    val startDestination by entryViewModel.startDestination.collectAsStateWithLifecycle()

    when (val destination = startDestination) {
        is StartDestination.Loading -> LoadingScreen()
        is StartDestination.Route -> {
            val navController = rememberNavController()
            KarincaNavGraph(navController, destination.route)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun KarincaNavGraph(navController: NavHostController, startRoute: KarincaRoute) {
    NavHost(
        navController = navController,
        startDestination = startRoute,
        // Instant cuts instead of Navigation Compose's default 700ms cross-fade.
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable<KarincaRoute.Welcome> {
            WelcomeScreen(
                onGetStarted = { navController.navigate(KarincaRoute.SignUp) },
                onSignIn = { navController.navigate(KarincaRoute.SignIn) },
                onAuthenticated = { destination -> navController.navigateToPostAuth(destination) },
            )
        }
        composable<KarincaRoute.SignIn> {
            SignInScreen(
                onAuthenticated = { destination -> navController.navigateToPostAuth(destination) },
                onNeedsOtp = { phone -> navController.navigate(KarincaRoute.OtpVerify(phone = phone)) },
                onNavigateToSignUp = {
                    navController.navigate(KarincaRoute.SignUp) { popUpTo(KarincaRoute.Welcome) }
                },
                onForgotPassword = { navController.navigate(KarincaRoute.ForgotPassword) },
            )
        }
        composable<KarincaRoute.ForgotPassword> {
            ForgotPasswordScreen(
                onCodeSent = { resetEmail -> navController.navigate(KarincaRoute.ResetPassword(resetEmail)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<KarincaRoute.ResetPassword> { backStackEntry ->
            val route: KarincaRoute.ResetPassword = backStackEntry.toRoute()
            ResetPasswordScreen(
                email = route.email,
                onAuthenticated = { destination -> navController.navigateToPostAuth(destination) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<KarincaRoute.SignUp> {
            SignUpScreen(
                onAuthenticated = { destination -> navController.navigateToPostAuth(destination) },
                onNeedsOtp = { phone -> navController.navigate(KarincaRoute.OtpVerify(phone = phone)) },
                onNavigateToSignIn = {
                    navController.navigate(KarincaRoute.SignIn) { popUpTo(KarincaRoute.Welcome) }
                },
            )
        }
        composable<KarincaRoute.OtpVerify> { backStackEntry ->
            val route: KarincaRoute.OtpVerify = backStackEntry.toRoute()
            OtpVerifyScreen(
                phone = route.phone,
                onAuthenticated = { destination -> navController.navigateToPostAuth(destination) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<KarincaRoute.KvkkConsent> {
            KvkkConsentScreen(
                onAccepted = { navController.navigateToPostAuth(KarincaRoute.Onboarding) },
            )
        }
        composable<KarincaRoute.Onboarding> {
            OnboardingScreen(
                onCompleted = { navController.navigateToPostAuth(KarincaRoute.Home) },
            )
        }
        composable<KarincaRoute.Home> {
            HomeScreen(
                onOpenProfile = { navController.navigateToTab(KarincaRoute.Profile) },
                onOpenProfileSetup = { navController.navigate(KarincaRoute.ProfileSetup) },
                onOpenGoals = { navController.navigateToTab(KarincaRoute.Goals) },
                onOpenAnalysis = { navController.navigateToTab(KarincaRoute.Analysis) },
                onOpenCoach = { navController.navigateToTab(KarincaRoute.WeeklyReport) },
                onAddTransaction = { navController.navigate(KarincaRoute.AddTransaction()) },
                onEditTransaction = { transactionId ->
                    navController.navigate(KarincaRoute.AddTransaction(transactionId = transactionId))
                },
            )
        }
        composable<KarincaRoute.Categories> {
            CategoriesScreen(
                onBack = { navController.popBackStack() },
                onOpenCategoryDetail = { categoryId -> navController.navigate(KarincaRoute.CategoryDetail(categoryId)) },
                onNavigateHome = { navController.navigateToTab(KarincaRoute.Home) },
                onNavigateGoals = { navController.navigateToTab(KarincaRoute.Goals) },
                onNavigateReports = { navController.navigateToTab(KarincaRoute.Analysis) },
                onNavigateProfile = { navController.navigateToTab(KarincaRoute.Profile) },
                onOpenCoach = { navController.navigateToTab(KarincaRoute.WeeklyReport) },
                onAddTransaction = { navController.navigate(KarincaRoute.AddTransaction()) },
            )
        }
        composable<KarincaRoute.Profile> {
            ProfileScreen(
                onSignedOut = { navController.navigateToPostAuth(KarincaRoute.Welcome) },
                onOpenProfileSetup = { navController.navigate(KarincaRoute.ProfileSetup) },
                onOpenKumbara = { navController.navigate(KarincaRoute.Kumbaram) },
                onOpenSubscriptions = { navController.navigate(KarincaRoute.Subscriptions) },
                onNavigateHome = { navController.navigateToTab(KarincaRoute.Home) },
                onNavigateGoals = { navController.navigateToTab(KarincaRoute.Goals) },
                onNavigateReports = { navController.navigateToTab(KarincaRoute.Analysis) },
                onOpenCoach = { navController.navigateToTab(KarincaRoute.WeeklyReport) },
                onAddTransaction = { navController.navigate(KarincaRoute.AddTransaction()) },
            )
        }
        composable<KarincaRoute.ProfileSetup> {
            ProfileSetupScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable<KarincaRoute.Kumbaram> {
            KumbaramScreen(onBack = { navController.popBackStack() })
        }
        composable<KarincaRoute.Subscriptions> {
            SubscriptionsScreen(
                onBack = { navController.popBackStack() },
                onOpenCoach = { navController.navigateToTab(KarincaRoute.WeeklyReport) },
            )
        }
        composable<KarincaRoute.Goals> {
            GoalsScreen(
                onNavigateHome = { navController.navigateToTab(KarincaRoute.Home) },
                onNavigateReports = { navController.navigateToTab(KarincaRoute.Analysis) },
                onNavigateProfile = { navController.navigateToTab(KarincaRoute.Profile) },
                onOpenCoach = { navController.navigateToTab(KarincaRoute.WeeklyReport) },
                onAddTransaction = { navController.navigate(KarincaRoute.AddTransaction()) },
            )
        }
        composable<KarincaRoute.AddTransaction> { backStackEntry ->
            val route: KarincaRoute.AddTransaction = backStackEntry.toRoute()
            AddTransactionScreen(
                transactionId = route.transactionId,
                presetCategoryId = route.presetCategoryId,
                onSaved = { navController.popBackStack() },
                onClose = { navController.popBackStack() },
            )
        }
        composable<KarincaRoute.Analysis> {
            AnalysisScreen(
                onCategoryClick = { categoryId -> navController.navigate(KarincaRoute.CategoryDetail(categoryId)) },
                onNavigateHome = { navController.navigateToTab(KarincaRoute.Home) },
                onNavigateGoals = { navController.navigateToTab(KarincaRoute.Goals) },
                onNavigateProfile = { navController.navigateToTab(KarincaRoute.Profile) },
                onOpenCoach = { navController.navigateToTab(KarincaRoute.WeeklyReport) },
                onAddTransaction = { navController.navigate(KarincaRoute.AddTransaction()) },
            )
        }
        composable<KarincaRoute.CategoryDetail> { backStackEntry ->
            val route: KarincaRoute.CategoryDetail = backStackEntry.toRoute()
            CategoryDetailScreen(
                categoryId = route.categoryId,
                onBack = { navController.popBackStack() },
                onEditTransaction = { transactionId ->
                    navController.navigate(KarincaRoute.AddTransaction(transactionId = transactionId))
                },
                onAddTransaction = { categoryId ->
                    navController.navigate(KarincaRoute.AddTransaction(presetCategoryId = categoryId))
                },
            )
        }
        composable<KarincaRoute.WeeklyReport> {
            WeeklyReportScreen(
                onNavigateHome = { navController.navigateToTab(KarincaRoute.Home) },
                onNavigateGoals = { navController.navigateToTab(KarincaRoute.Goals) },
                onNavigateReports = { navController.navigateToTab(KarincaRoute.Analysis) },
                onNavigateProfile = { navController.navigateToTab(KarincaRoute.Profile) },
                onAddTransaction = { navController.navigate(KarincaRoute.AddTransaction()) },
            )
        }
    }
}

// Clears the whole back stack on success so the system back button can never return to
// Welcome/SignIn/SignUp/OtpVerify/KvkkConsent afterward. Pops all the way to the graph's
// actual start destination (which varies — Welcome, KvkkConsent, or Home, resolved by
// AppEntryViewModel) rather than the fragile popUpTo(0) id trick.
private fun NavHostController.navigateToPostAuth(route: KarincaRoute) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { inclusive = true }
    }
}

// For hopping between the app's main browsing screens (Home, Categories, Profile, Goals,
// Analysis) — reuses each screen's existing back stack entry and saved state instead of
// pushing a fresh instance (and a fresh ViewModel that reloads everything from scratch)
// every time you bounce between them, the standard Navigation Compose bottom-tab pattern.
private fun NavHostController.navigateToTab(route: KarincaRoute) {
    if (route == KarincaRoute.Home) {
        // Home is the graph's own start destination, so it's never actually removed from
        // the back stack by the popUpTo below (only entries ABOVE it get popped) — it's
        // always still sitting there. Routing this case through the same
        // navigate()+popUpTo+restoreState dance made Navigation Compose take a much
        // heavier internal path (destination == popUpTo anchor == graph start), which
        // showed up as a very noticeable lag returning to Home specifically. A plain pop
        // back to the entry that's already there is both correct and cheap.
        popBackStack(route, inclusive = false)
        return
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
