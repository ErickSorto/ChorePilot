package com.sortoapps.chorepilot.navigation

import com.sortoapps.chorepilot.features.reports.presentation.viewmodel.ReportsRange
import kotlinx.serialization.Serializable

@Serializable
enum class PremiumRouteOriginType {
    InitialOnboarding,
    DiscoveryCapture,
    Reports,
    ChoreEditorProof,
}

@Serializable
sealed class PremiumOrigin {
    @Serializable
    data object InitialOnboarding : PremiumOrigin()

    @Serializable
    data class DiscoveryCapture(
        val choreId: String = "",
        val weekdayName: String = "",
        val dayOffset: Int = 0,
        val isToday: Boolean = true,
        val dateKey: String = "",
    ) : PremiumOrigin()

    @Serializable
    data class Reports(
        val range: ReportsRange,
    ) : PremiumOrigin()

    @Serializable
    data class ChoreEditorProof(
        val choreId: String? = null,
    ) : PremiumOrigin()
}

@Serializable
sealed class AppRoute {
    @Serializable
    data object Launch : AppRoute()

    @Serializable
    data object Onboarding : AppRoute()

    @Serializable
    data object AuthLanding : AppRoute()

    @Serializable
    data object SignIn : AppRoute()

    @Serializable
    data object HouseholdChoice : AppRoute()

    @Serializable
    data object CreateHousehold : AppRoute()

    @Serializable
    data object JoinHousehold : AppRoute()

    @Serializable
    data class Premium(
        val originType: PremiumRouteOriginType = PremiumRouteOriginType.InitialOnboarding,
        val choreId: String = "",
        val weekdayName: String = "",
        val dayOffset: Int = 0,
        val isToday: Boolean = true,
        val dateKey: String = "",
        val reportsRange: ReportsRange = ReportsRange.Daily,
    ) : AppRoute() {
        constructor(origin: PremiumOrigin) : this(
            originType = origin.toRouteType(),
            choreId = when (origin) {
                is PremiumOrigin.DiscoveryCapture -> origin.choreId
                is PremiumOrigin.ChoreEditorProof -> origin.choreId.orEmpty()
                PremiumOrigin.InitialOnboarding,
                is PremiumOrigin.Reports,
                -> ""
            },
            weekdayName = (origin as? PremiumOrigin.DiscoveryCapture)?.weekdayName.orEmpty(),
            dayOffset = (origin as? PremiumOrigin.DiscoveryCapture)?.dayOffset ?: 0,
            isToday = (origin as? PremiumOrigin.DiscoveryCapture)?.isToday ?: true,
            dateKey = (origin as? PremiumOrigin.DiscoveryCapture)?.dateKey.orEmpty(),
            reportsRange = (origin as? PremiumOrigin.Reports)?.range ?: ReportsRange.Daily,
        )

        fun toPremiumOrigin(): PremiumOrigin = when (originType) {
            PremiumRouteOriginType.InitialOnboarding -> PremiumOrigin.InitialOnboarding
            PremiumRouteOriginType.DiscoveryCapture -> PremiumOrigin.DiscoveryCapture(
                choreId = choreId,
                weekdayName = weekdayName,
                dayOffset = dayOffset,
                isToday = isToday,
                dateKey = dateKey,
            )
            PremiumRouteOriginType.Reports -> PremiumOrigin.Reports(reportsRange)
            PremiumRouteOriginType.ChoreEditorProof -> PremiumOrigin.ChoreEditorProof(
                choreId = choreId.ifBlank { null },
            )
        }
    }

    @Serializable
    data object Main : AppRoute()

    @Serializable
    data object Home : AppRoute()

    @Serializable
    data object Chores : AppRoute()

    @Serializable
    data object Family : AppRoute()

    @Serializable
    data object Reports : AppRoute()

    @Serializable
    data object More : AppRoute()

    @Serializable
    data class ChoreDetail(val choreId: String) : AppRoute()

    @Serializable
    data object CreateChore : AppRoute()

    @Serializable
    data class ChoreCapture(
        val choreId: String = "",
        val weekdayName: String = "",
        val dayOffset: Int = 0,
        val isToday: Boolean = true,
        val dateKey: String = "",
        val launchCameraImmediately: Boolean = false,
    ) : AppRoute()

    @Serializable
    data class EditChore(val choreId: String) : AppRoute()

    @Serializable
    data class MemberProfile(val memberId: String) : AppRoute()

    @Serializable
    data object InviteMember : AppRoute()

    @Serializable
    data class DayOff(val memberId: String) : AppRoute()

    @Serializable
    data class GoneMode(val memberId: String) : AppRoute()

    @Serializable
    data object Settings : AppRoute()

    @Serializable
    data object Account : AppRoute()

    @Serializable
    data object NotificationCenter : AppRoute()

    @Serializable
    data object Billing : AppRoute()

    @Serializable
    data object Rewards : AppRoute()

    @Serializable
    data object PreviewGallery : AppRoute()
}

private fun PremiumOrigin.toRouteType(): PremiumRouteOriginType = when (this) {
    PremiumOrigin.InitialOnboarding -> PremiumRouteOriginType.InitialOnboarding
    is PremiumOrigin.DiscoveryCapture -> PremiumRouteOriginType.DiscoveryCapture
    is PremiumOrigin.Reports -> PremiumRouteOriginType.Reports
    is PremiumOrigin.ChoreEditorProof -> PremiumRouteOriginType.ChoreEditorProof
}
