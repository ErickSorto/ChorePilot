package com.sortoapps.chorepilot.features.rewards.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sortoapps.chorepilot.core.demo.DemoTone
import com.sortoapps.chorepilot.core.designsystem.AnimatedProgressBar
import com.sortoapps.chorepilot.core.designsystem.AvatarBadge
import com.sortoapps.chorepilot.core.designsystem.ChorePilotGradientBackground
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopAppBar
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTopBarIconButton
import com.sortoapps.chorepilot.core.designsystem.GradientButton
import com.sortoapps.chorepilot.core.designsystem.SecondaryButton
import com.sortoapps.chorepilot.core.designsystem.ToneChip
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewData
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewTheme
import com.sortoapps.chorepilot.features.rewards.presentation.viewmodel.RewardsCatalogItemUiModel
import com.sortoapps.chorepilot.features.rewards.presentation.viewmodel.RewardsEditorUiModel
import com.sortoapps.chorepilot.features.rewards.presentation.viewmodel.RewardsHistoryRowUiModel
import com.sortoapps.chorepilot.features.rewards.presentation.viewmodel.RewardsMemberProgressUiModel
import com.sortoapps.chorepilot.features.rewards.presentation.viewmodel.RewardsRulesUiModel
import com.sortoapps.chorepilot.features.rewards.presentation.viewmodel.RewardsUiEvent
import com.sortoapps.chorepilot.features.rewards.presentation.viewmodel.RewardsUiState

@Composable
fun RewardsScreen(
    state: RewardsUiState,
    onEvent: (RewardsUiEvent) -> Unit,
    onBack: () -> Unit,
) {
    ChorePilotGradientBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                ChorePilotTopAppBar(
                    sectionTitle = "Rewards",
                    navigationIcon = {
                        ChorePilotTopBarIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBack,
                        )
                    },
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    RewardsHeroCard(
                        state = state,
                        onRefresh = { onEvent(RewardsUiEvent.Refresh) },
                    )
                }

                if (state.isLoading) {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                        )
                    }
                }

                state.celebration?.let { celebration ->
                    item {
                        MessageCard(
                            title = celebration.title,
                            body = celebration.body,
                            tone = DemoTone.Success,
                            primaryActionLabel = "Keep going",
                            onPrimaryAction = { onEvent(RewardsUiEvent.DismissCelebration) },
                        )
                    }
                }

                state.errorMessage?.let { error ->
                    item {
                        MessageCard(
                            title = "Rewards need attention",
                            body = error,
                            tone = DemoTone.Warning,
                            primaryActionLabel = "Dismiss",
                            onPrimaryAction = { onEvent(RewardsUiEvent.DismissError) },
                        )
                    }
                }

                if (state.emptyTitle.isNotBlank()) {
                    item {
                        EmptyStateCard(
                            title = state.emptyTitle,
                            body = state.emptyBody,
                            canManageRewards = state.canManageRewards,
                        )
                    }
                }

                if (state.leaderboard.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = if (state.canManageRewards) "Household points" else "Leaderboard",
                        )
                    }
                    items(state.leaderboard, key = { it.memberId }) { member ->
                        LeaderboardCard(member = member)
                    }
                }

                item {
                    SectionHeader(
                        title = "Rewards",
                    )
                }
                if (state.rewards.isEmpty()) {
                    item {
                        SubtleInfoCard(
                            title = if (state.canManageRewards) "Your first reward starts here" else "No rewards live yet",
                            body = if (state.canManageRewards) {
                                "Add one simple reward below so points have a real payoff right away."
                            } else {
                                "Keep finishing chores and submitting proof. Your points will be ready when the first reward goes live."
                            },
                        )
                    }
                } else {
                    items(state.rewards, key = { it.id }) { reward ->
                        RewardCatalogCard(
                            reward = reward,
                            canManageRewards = state.canManageRewards,
                            onRedeem = { onEvent(RewardsUiEvent.RedeemReward(reward.id)) },
                            onDelete = { onEvent(RewardsUiEvent.DeleteRewardDefinition(reward.id)) },
                        )
                    }
                }

                if (state.canManageRewards) {
                    item {
                        SectionHeader(
                            title = "Points",
                        )
                    }
                    item {
                        RulesCard(
                            rules = state.rules,
                            onCompletionChange = { onEvent(RewardsUiEvent.CompletionPointsChanged(it)) },
                            onProofSubmittedChange = { onEvent(RewardsUiEvent.ProofSubmissionBonusChanged(it)) },
                            onProofApprovedChange = { onEvent(RewardsUiEvent.ProofApprovalBonusChanged(it)) },
                        )
                    }

                    item {
                        SectionHeader(
                            title = "Add reward",
                        )
                    }
                    item {
                        RewardEditorCard(
                            editor = state.editor,
                            onTitleChange = { onEvent(RewardsUiEvent.DraftTitleChanged(it)) },
                            onDescriptionChange = { onEvent(RewardsUiEvent.DraftDescriptionChanged(it)) },
                            onCostChange = { onEvent(RewardsUiEvent.DraftCostChanged(it)) },
                            onSave = { onEvent(RewardsUiEvent.SaveRewardDefinition) },
                        )
                    }
                }

                if (state.history.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Recent",
                        )
                    }
                    items(state.history, key = { "${it.title}-${it.body}-${it.pointsLabel}" }) { history ->
                        HistoryCard(history = history)
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardsHeroCard(
    state: RewardsUiState,
    onRefresh: () -> Unit,
) {
    val progress = state.hero.nextRewardLabel.substringBefore(" ").toFloatOrNull()?.let { gap ->
        if (gap <= 0f) 1f else 1f - (gap / (gap + 24f))
    } ?: if (state.rewards.any { it.canRedeem }) 1f else 0.24f
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ToneChip(
                        text = state.householdName,
                        tone = if (state.canManageRewards) DemoTone.Secondary else DemoTone.Primary,
                    )
                    Text(
                        text = state.hero.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = state.hero.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = if (state.canManageRewards) Icons.Rounded.Tune else Icons.Rounded.Redeem,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp),
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HeroMetricChip(label = "Available", value = state.hero.pointsLabel, tone = DemoTone.Primary)
                HeroMetricChip(label = "Streak", value = state.hero.streakLabel, tone = DemoTone.Secondary)
                HeroMetricChip(
                    label = if (state.canManageRewards) "Manager" else "Viewer",
                    value = state.viewerName,
                    tone = DemoTone.Tertiary,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimatedProgressBar(progress = progress)
                Text(
                    text = state.hero.nextRewardLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = state.hero.supportingLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SecondaryButton(
                text = "Refresh rewards",
                onClick = onRefresh,
            )
        }
    }
}

@Composable
private fun HeroMetricChip(
    label: String,
    value: String,
    tone: DemoTone,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ToneChip(text = label, tone = tone)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun LeaderboardCard(
    member: RewardsMemberProgressUiModel,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarBadge(seed = member.avatarSeed, tone = member.tone)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = member.memberName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (member.isViewer) {
                            ToneChip(text = "You", tone = DemoTone.Primary)
                        }
                    }
                    Text(
                        text = member.supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    ToneChip(text = member.pointsLabel, tone = member.tone)
                    Text(
                        text = member.streakLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RewardCatalogCard(
    reward: RewardsCatalogItemUiModel,
    canManageRewards: Boolean,
    onRedeem: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = reward.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = reward.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ToneChip(
                    text = reward.costLabel,
                    tone = if (reward.canRedeem) DemoTone.Success else DemoTone.Secondary,
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ToneChip(
                    text = reward.availabilityLabel,
                    tone = if (reward.canRedeem) DemoTone.Success else DemoTone.Tertiary,
                )
            }

            if (reward.canRedeem) {
                GradientButton(
                    text = "Redeem now",
                    onClick = onRedeem,
                )
            } else {
                SecondaryButton(
                    text = reward.availabilityLabel,
                    enabled = false,
                    onClick = {},
                )
            }

            if (reward.canDelete) {
                SecondaryButton(
                    text = "Remove reward",
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
private fun RulesCard(
    rules: RewardsRulesUiModel,
    onCompletionChange: (Int) -> Unit,
    onProofSubmittedChange: (Int) -> Unit,
    onProofApprovedChange: (Int) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RuleStepperRow(
                title = "Chore completion",
                value = rules.pointsPerCompletion,
                onValueChange = onCompletionChange,
            )
            HorizontalDivider()
            RuleStepperRow(
                title = "Proof submitted",
                value = rules.proofSubmissionBonus,
                onValueChange = onProofSubmittedChange,
            )
            HorizontalDivider()
            RuleStepperRow(
                title = "Proof approved",
                value = rules.proofApprovalBonus,
                onValueChange = onProofApprovedChange,
            )
        }
    }
}

@Composable
private fun RuleStepperRow(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        StepButton(label = "-", onClick = { onValueChange(value - 1) })
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        StepButton(label = "+", onClick = { onValueChange(value + 1) })
    }
}

@Composable
private fun StepButton(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RewardEditorCard(
    editor: RewardsEditorUiModel,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCostChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = editor.title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reward title") },
                singleLine = true,
            )
            OutlinedTextField(
                value = editor.description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reward description") },
                minLines = 2,
            )
            OutlinedTextField(
                value = editor.costText,
                onValueChange = onCostChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cost in points") },
                singleLine = true,
            )
            GradientButton(
                text = "Save reward",
                enabled = editor.saveEnabled,
                onClick = onSave,
            )
        }
    }
}

@Composable
private fun HistoryCard(
    history: RewardsHistoryRowUiModel,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f),
            ) {
                Icon(
                    imageVector = when (history.tone) {
                        DemoTone.Success -> Icons.Rounded.AutoAwesome
                        DemoTone.Warning -> Icons.Rounded.CardGiftcard
                        DemoTone.Secondary -> Icons.Rounded.Redeem
                        DemoTone.Tertiary -> Icons.Rounded.MilitaryTech
                        else -> Icons.Rounded.LocalFireDepartment
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(18.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = history.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = history.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = history.timestampLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ToneChip(text = history.pointsLabel, tone = history.tone)
        }
    }
}

@Composable
private fun MessageCard(
    title: String,
    body: String,
    tone: DemoTone,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ToneChip(text = title, tone = tone)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SecondaryButton(
                text = primaryActionLabel,
                onClick = onPrimaryAction,
            )
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    body: String,
    canManageRewards: Boolean,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ToneChip(
                text = if (canManageRewards) "First reward" else "Start earning",
                tone = DemoTone.Secondary,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SubtleInfoCard(
    title: String,
    body: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "Rewards Admin Empty", widthDp = 393, heightDp = 852)
@Composable
private fun RewardsAdminEmptyPreview() {
    ChorePilotPreviewTheme {
        RewardsScreen(
            state = ChorePilotPreviewData.rewardsAdminEmptyState(),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Rewards Celebration", widthDp = 393, heightDp = 852)
@Composable
private fun RewardsCelebrationPreview() {
    ChorePilotPreviewTheme {
        RewardsScreen(
            state = ChorePilotPreviewData.rewardsCelebrationState(),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Rewards Member Empty", widthDp = 393, heightDp = 852)
@Composable
private fun RewardsMemberEmptyPreview() {
    ChorePilotPreviewTheme {
        RewardsScreen(
            state = ChorePilotPreviewData.rewardsMemberEmptyState(),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Rewards First Reward", widthDp = 393, heightDp = 852)
@Composable
private fun RewardsFirstRewardPreview() {
    ChorePilotPreviewTheme {
        RewardsScreen(
            state = ChorePilotPreviewData.rewardsFirstRewardState(),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Rewards Ready To Redeem", widthDp = 393, heightDp = 852)
@Composable
private fun RewardsReadyToRedeemPreview() {
    ChorePilotPreviewTheme {
        RewardsScreen(
            state = ChorePilotPreviewData.rewardsRedemptionState(),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Rewards Wide Ready", widthDp = 900, heightDp = 720)
@Composable
private fun RewardsWidePreview() {
    ChorePilotPreviewTheme {
        RewardsScreen(
            state = ChorePilotPreviewData.rewardsRedemptionState(),
            onEvent = {},
            onBack = {},
        )
    }
}
