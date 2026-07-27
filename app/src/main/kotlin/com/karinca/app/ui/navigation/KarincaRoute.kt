package com.karinca.app.ui.navigation

import kotlinx.serialization.Serializable

// Navigation Compose 2.8+ type-safe routes.
sealed interface KarincaRoute {
    @Serializable
    data object Welcome : KarincaRoute

    @Serializable
    data object SignIn : KarincaRoute

    @Serializable
    data object SignUp : KarincaRoute

    @Serializable
    data class OtpVerify(val phone: String) : KarincaRoute

    @Serializable
    data object KvkkConsent : KarincaRoute

    @Serializable
    data object Onboarding : KarincaRoute

    @Serializable
    data object Home : KarincaRoute

    @Serializable
    data object Categories : KarincaRoute

    @Serializable
    data object Profile : KarincaRoute

    @Serializable
    data class AddTransaction(val transactionId: String? = null, val presetCategoryId: String? = null) : KarincaRoute

    @Serializable
    data object Analysis : KarincaRoute

    @Serializable
    data class CategoryDetail(val categoryId: String) : KarincaRoute

    @Serializable
    data object Goals : KarincaRoute

    @Serializable
    data object ProfileSetup : KarincaRoute

    @Serializable
    data object WeeklyReport : KarincaRoute

    @Serializable
    data object Kumbaram : KarincaRoute

    @Serializable
    data object Subscriptions : KarincaRoute
}
