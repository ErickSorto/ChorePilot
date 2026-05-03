package com.sortoapps.chorepilot.features.billing.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sortoapps.chorepilot.core.designsystem.ChorePilotGradientBackground
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopAppBar
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopBarIconButton
import com.sortoapps.chorepilot.core.designsystem.SecondaryButton
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewTheme
import com.sortoapps.chorepilot.core.preview.CompactPreviewShell
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionPlanId
import com.sortoapps.chorepilot.features.billing.presentation.viewmodel.BillingMessageTone
import com.sortoapps.chorepilot.features.billing.presentation.viewmodel.BillingMessageUiModel
import com.sortoapps.chorepilot.features.billing.presentation.viewmodel.BillingPlanUiModel
import com.sortoapps.chorepilot.features.billing.presentation.viewmodel.BillingUiEvent
import com.sortoapps.chorepilot.features.billing.presentation.viewmodel.BillingUiState
import com.sortoapps.chorepilot.navigation.AppRoute

@Composable
fun BillingScreen(
    state: BillingUiState,
    onEvent: (BillingUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    ChorePilotGradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                ChorePilotTopAppBar(
                    sectionTitle = "Manage subscription",
                    navigationIcon = {
                        ChorePilotTopBarIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            onClick = {
                                onEvent(BillingUiEvent.TriggerVibration)
                                onBack()
                            },
                        )
                    },
                )
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp)
                        .widthIn(max = 680.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BillingSummaryCard(state = state)
                    BillingHouseholdNoticeCard()
                    state.feedback?.let { feedback ->
                        BillingFeedbackCard(feedback = feedback)
                    }
                    if (state.plans.isEmpty()) {
                        BillingMutedCard(
                            title = "Plans unavailable",
                            body = state.offeringUnavailableLabel,
                            icon = Icons.Rounded.Sell,
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            state.plans.forEach { plan ->
                                BillingPlanCard(
                                    plan = plan,
                                    onSelect = {
                                        onEvent(BillingUiEvent.TriggerVibration)
                                        onEvent(BillingUiEvent.PurchasePlanClicked(plan.planId))
                                    },
                                )
                            }
                        }
                    }
                    BillingFooterActions(
                        restoreLabel = state.restoreLabel,
                        canRestore = state.canRestore,
                        onRestore = {
                            onEvent(BillingUiEvent.TriggerVibration)
                            onEvent(BillingUiEvent.RestorePurchasesClicked)
                        },
                        onCancel = {
                            onEvent(BillingUiEvent.TriggerVibration)
                            onBack()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BillingHouseholdNoticeCard() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "One plan covers the whole household",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Everyone in your household gets the Pro features together.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BillingSummaryCard(
    state: BillingUiState,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CreditCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = state.activePlanSummary,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = state.headline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (state.householdStatusLabel.isNotBlank()) {
                Text(
                    text = state.householdStatusLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BillingFeedbackCard(
    feedback: BillingMessageUiModel,
) {
    val accent = when (feedback.tone) {
        BillingMessageTone.Info -> MaterialTheme.colorScheme.secondary
        BillingMessageTone.Success -> Color(0xFF2DCB87)
        BillingMessageTone.Error -> MaterialTheme.colorScheme.error
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = feedback.title,
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = feedback.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BillingPlanCard(
    plan: BillingPlanUiModel,
    onSelect: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(
            width = if (plan.highlightLabel != null) 1.25.dp else 1.dp,
            color = if (plan.highlightLabel != null) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            },
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = plan.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = plan.priceLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = plan.billingPeriodLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                plan.highlightLabel?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            plan.perMonthLabel?.let { perMonthLabel ->
                Text(
                    text = perMonthLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SecondaryButton(text = plan.purchaseLabel, enabled = plan.isEnabled, onClick = onSelect)
        }
    }
}

@Composable
private fun BillingMutedCard(
    title: String,
    body: String,
    icon: ImageVector,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BillingFooterActions(
    restoreLabel: String,
    canRestore: Boolean,
    onRestore: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SecondaryButton(
            text = restoreLabel,
            enabled = canRestore,
            onClick = onRestore,
        )
        TextButton(onClick = onCancel) {
            Text(
                text = "Cancel",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun BillingNoPlanPreview() {
    ChorePilotPreviewTheme {
        CompactPreviewShell(selectedRoute = AppRoute.More) { modifier ->
            Box(modifier = modifier) {
                BillingScreen(
                    state = BillingUiState(
                        isLoading = false,
                        headline = "Start ChorePilot Pro",
                        supportingText = "Choose a plan, restore a purchase, or manage your subscription from the store.",
                        householdStatusLabel = "This household is still on the free plan.",
                        purchaserStatusLabel = "This signed-in account does not currently hold the active store entitlement.",
                        activePlanSummary = "No active plan",
                        plans = listOf(
                            BillingPlanUiModel(
                                planId = SubscriptionPlanId.Monthly,
                                title = "Monthly",
                                priceLabel = "$6.99",
                                billingPeriodLabel = "Billed monthly",
                                purchaseLabel = "Start monthly",
                            ),
                            BillingPlanUiModel(
                                planId = SubscriptionPlanId.Yearly,
                                title = "Yearly",
                                priceLabel = "$49.99",
                                billingPeriodLabel = "Billed yearly",
                                perMonthLabel = "$4.16 per month equivalent",
                                highlightLabel = "Best value",
                                purchaseLabel = "Start yearly",
                            ),
                        ),
                    ),
                    onEvent = {},
                    onBack = {},
                )
            }
        }
    }
}

@Preview
@Composable
private fun BillingActivePreview() {
    ChorePilotPreviewTheme {
        CompactPreviewShell(selectedRoute = AppRoute.More) { modifier ->
            Box(modifier = modifier) {
                BillingScreen(
                    state = BillingUiState(
                        isLoading = false,
                        hasChorePilotPro = true,
                        headline = "ChorePilot Pro is active",
                        supportingText = "You’re the active purchaser for this household, so plan management lives here.",
                        householdStatusLabel = "Reports, discovery mode, and proof-required chores are unlocked for everyone in this household.",
                        purchaserStatusLabel = "This signed-in account currently holds the active store entitlement.",
                        activePlanSummary = "Yearly plan active",
                        feedback = BillingMessageUiModel(
                            tone = BillingMessageTone.Success,
                            title = "Restore successful",
                            body = "ChorePilot Pro is active for this household again.",
                        ),
                        plans = listOf(
                            BillingPlanUiModel(
                                planId = SubscriptionPlanId.Monthly,
                                title = "Monthly",
                                priceLabel = "$6.99",
                                billingPeriodLabel = "Billed monthly",
                                purchaseLabel = "Already active",
                                isEnabled = false,
                            ),
                            BillingPlanUiModel(
                                planId = SubscriptionPlanId.Yearly,
                                title = "Yearly",
                                priceLabel = "$49.99",
                                billingPeriodLabel = "Billed yearly",
                                perMonthLabel = "$4.16 per month equivalent",
                                highlightLabel = "Best value",
                                purchaseLabel = "Already active",
                                isEnabled = false,
                            ),
                        ),
                        managementUrl = "https://example.com/manage",
                    ),
                    onEvent = {},
                    onBack = {},
                )
            }
        }
    }
}
