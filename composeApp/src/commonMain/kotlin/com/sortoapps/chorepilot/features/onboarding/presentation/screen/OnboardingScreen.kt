package com.sortoapps.chorepilot.features.onboarding.presentation.screen

import chorepilot.composeapp.generated.resources.Res
import chorepilot.composeapp.generated.resources.onboarding_background_light
import chorepilot.composeapp.generated.resources.onboarding_hero_mascot
import chorepilot.composeapp.generated.resources.onboarding_hero_scene
import androidx.compose.foundation.Image
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.ViewTimeline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.sortoapps.chorepilot.core.demo.DemoTone
import com.sortoapps.chorepilot.core.designsystem.ChorePilotGradientBackground
import com.sortoapps.chorepilot.core.designsystem.ChorePilotTheme
import com.sortoapps.chorepilot.core.designsystem.ProgressPill
import com.sortoapps.chorepilot.core.designsystem.SecondaryButton
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewData
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewTheme
import com.sortoapps.chorepilot.core.preview.PreviewDeviceFrame
import com.sortoapps.chorepilot.features.settings.domain.ThemeMode
import com.sortoapps.chorepilot.features.onboarding.presentation.viewmodel.OnboardingCalloutUiModel
import com.sortoapps.chorepilot.features.onboarding.presentation.viewmodel.OnboardingStepKind
import com.sortoapps.chorepilot.features.onboarding.presentation.viewmodel.OnboardingStepUiModel
import com.sortoapps.chorepilot.features.onboarding.presentation.viewmodel.OnboardingUiEvent
import com.sortoapps.chorepilot.features.onboarding.presentation.viewmodel.OnboardingUiState
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onEvent: (OnboardingUiEvent) -> Unit,
    onOpenAuth: () -> Unit,
    onOpenPremium: () -> Unit,
    onFinish: () -> Unit,
) {
    val step = state.currentStep
    val accentColor = onboardingAccent(step)
    val screenSpacing = when (step.kind) {
        OnboardingStepKind.PremiumBridge,
        OnboardingStepKind.Snapshot,
        OnboardingStepKind.Timeline -> 10.dp
        else -> 16.dp
    }
    val primaryLabel = if (step.kind == OnboardingStepKind.NameCapture && state.displayName.isBlank()) {
        "Continue"
    } else {
        step.primaryActionLabel
    }
    val handlePrimaryAction = {
        onEvent(OnboardingUiEvent.TriggerVibration)
        when (step.id) {
            "premium_bridge" -> onOpenPremium()
            else -> onEvent(OnboardingUiEvent.ContinueClicked)
        }
    }
    val handleSecondaryAction = {
        onEvent(OnboardingUiEvent.TriggerVibration)
        when (step.id) {
            "dream" -> onOpenAuth()
            "review" -> onEvent(OnboardingUiEvent.ContinueClicked)
            "premium_bridge" -> {
                onEvent(OnboardingUiEvent.ContinueClicked)
                onFinish()
            }

            else -> onEvent(OnboardingUiEvent.ContinueClicked)
        }
    }

    if (step.kind == OnboardingStepKind.Hero) {
        OnboardingHeroLandingStep(
            step = step,
            primaryLabel = primaryLabel,
            canContinue = state.canContinue,
            progress = state.progressFraction,
            onPrimary = handlePrimaryAction,
            onSecondary = handleSecondaryAction,
        )
        return
    }

    Scaffold {
        ChorePilotGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(alpha = 0.98f),
                    painter = painterResource(Res.drawable.onboarding_background_light),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(screenSpacing),
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 560.dp)
                                .heightIn(min = maxHeight)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(
                                space = screenSpacing,
                                alignment = Alignment.CenterVertically,
                            ),
                        ) {
                                when (step.kind) {
                                    OnboardingStepKind.Hero,
                                    OnboardingStepKind.NameCapture,
                                    OnboardingStepKind.Timeline,
                                    OnboardingStepKind.Trust,
                                    OnboardingStepKind.PremiumBridge -> {
                                        StepArtwork(step = step)
                                    }

                                    else -> Unit
                                }

                                StepHeading(step = step)

                                when (step.kind) {
                                    OnboardingStepKind.NameCapture -> {
                                        NameCaptureSection(
                                            value = state.displayName,
                                            onValueChange = { onEvent(OnboardingUiEvent.DisplayNameChanged(it)) },
                                        )
                                    }

                                    OnboardingStepKind.ChoiceList -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            step.choices.forEach { choice ->
                                                OnboardingChoiceCard(
                                                    stepId = step.id,
                                                    title = choice.title,
                                                    description = choice.description,
                                                    leadingLabel = choice.leadingLabel,
                                                    selected = state.selectedChoices[step.id] == choice.id,
                                                    accentColor = accentColor,
                                                    onClick = {
                                                        onEvent(OnboardingUiEvent.TriggerVibration)
                                                        onEvent(
                                                            OnboardingUiEvent.StepChoiceSelected(
                                                                stepId = step.id,
                                                                choiceId = choice.id,
                                                            ),
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    }

                                    OnboardingStepKind.ScaleList -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            step.choices.forEachIndexed { index, choice ->
                                                OnboardingScaleCard(
                                                    stepId = step.id,
                                                    choiceId = choice.id,
                                                    title = choice.title,
                                                    description = choice.description,
                                                    intensity = (index + 1).toFloat() / step.choices.size.coerceAtLeast(1),
                                                    selected = state.selectedChoices[step.id] == choice.id,
                                                    accentColor = accentColor,
                                                    onClick = {
                                                        onEvent(OnboardingUiEvent.TriggerVibration)
                                                        onEvent(
                                                            OnboardingUiEvent.StepChoiceSelected(
                                                                stepId = step.id,
                                                                choiceId = choice.id,
                                                            ),
                                                        )
                                                    },
                                                )
                                            }
                                        }
                                    }

                                    OnboardingStepKind.Snapshot -> {
                                        SnapshotSection(callouts = step.callouts)
                                    }

                                    OnboardingStepKind.InsightSummary -> {
                                        InsightSummarySection(callouts = step.callouts, accentColor = accentColor)
                                    }

                                    OnboardingStepKind.Timeline -> {
                                        TimelineSection(
                                            items = step.timeline.map { Triple(it.dayLabel, it.title, it.body) },
                                            accentColor = accentColor,
                                        )
                                    }

                                    OnboardingStepKind.Trust -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            step.callouts.forEach { callout ->
                                                CalloutCard(
                                                    callout = callout,
                                                    accentColor = accentColor,
                                                )
                                            }
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                step.chips.forEach { (text, tone) ->
                                                    ProgressPill(
                                                        text = text,
                                                        active = true,
                                                        accentColor = toneColor(tone),
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    OnboardingStepKind.Review -> {
                                        ReviewSection()
                                    }

                                    OnboardingStepKind.PremiumBridge -> {
                                        PremiumBridgeBenefitsSection(callouts = step.callouts)
                                    }

                                    OnboardingStepKind.Hero -> Unit
                                }

                                StepActionSection(
                                    step = step,
                                    primaryLabel = primaryLabel,
                                    canContinue = state.canContinue,
                                    progress = state.progressFraction,
                                    accentBrush = primaryBrush(step),
                                    onPrimary = handlePrimaryAction,
                                    onSecondary = handleSecondaryAction,
                                )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeroLandingStep(
    step: OnboardingStepUiModel,
    primaryLabel: String,
    canContinue: Boolean,
    progress: Float,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    val restingHeroSceneScale = 1.08f
    var panelVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        panelVisible = true
    }
    val heroMotion = rememberInfiniteTransition(label = "onboarding-hero-motion")
    val heroSceneScale by heroMotion.animateFloat(
        initialValue = restingHeroSceneScale,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "onboarding-hero-scale",
    )
    val panelEntranceProgress by animateFloatAsState(
        targetValue = if (panelVisible) 0f else 1f,
        animationSpec = tween(
            durationMillis = 680,
            easing = FastOutSlowInEasing,
        ),
        label = "onboarding-panel-entrance",
    )
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val panelColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    }
    val panelOutline = MaterialTheme.colorScheme.outline.copy(alpha = if (isDarkTheme) 0.16f else 0.1f)
    val heroAccent = if (isDarkTheme) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }
    val headlineColor = MaterialTheme.colorScheme.onSurface
    val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant
    val overlayMiddle = if (isDarkTheme) {
        panelColor.copy(alpha = 0.18f)
    } else {
        panelColor.copy(alpha = 0.06f)
    }
    val overlayEnd = if (isDarkTheme) {
        panelColor.copy(alpha = 0.94f)
    } else {
        panelColor.copy(alpha = 0.92f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
            ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(1.14f)
                .fillMaxHeight(0.66f)
                .align(Alignment.TopCenter)
                .offset(y = (-54).dp)
                .clip(RectangleShape),
        ) {
            Image(
                painter = painterResource(Res.drawable.onboarding_hero_scene),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = heroSceneScale
                        scaleY = heroSceneScale
                    },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            overlayMiddle,
                            overlayEnd,
                        ),
                    ),
                ),
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.64f)
                .align(Alignment.BottomCenter)
                .offset(y = (panelEntranceProgress * 220f).dp),
            color = panelColor,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, panelOutline),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Spacer(modifier = Modifier.height(72.dp))
                Text(
                    text = step.eyebrow,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = heroAccent,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 34.sp,
                        lineHeight = 39.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = headlineColor,
                    textAlign = TextAlign.Center,
                )
                HeroConfidenceStrip(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = heroAccent,
                    isDarkTheme = isDarkTheme,
                )
                StepActionSection(
                    step = step,
                    primaryLabel = primaryLabel,
                    canContinue = canContinue,
                    progress = progress,
                    accentBrush = primaryBrush(step),
                    onPrimary = onPrimary,
                    onSecondary = onSecondary,
                )
            }
        }
        Image(
            painter = painterResource(Res.drawable.onboarding_hero_mascot),
            contentDescription = "ChorePilot squirrel mascot",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .align(Alignment.Center)
                .offset(y = (-104).dp + (panelEntranceProgress * 104f).dp),
        )
    }
}

@Composable
private fun HeroConfidenceStrip(
    accentColor: Color,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = if (isDarkTheme) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
        },
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = if (isDarkTheme) 0.16f else 0.1f),
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HeroConfidenceItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Checklist,
                label = "Clear owners",
                accentColor = accentColor,
            )
            HeroConfidenceItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.AutoAwesome,
                label = "Visible proof",
                accentColor = accentColor,
            )
            HeroConfidenceItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.ViewTimeline,
                label = "Weekly rhythm",
                accentColor = accentColor,
            )
        }
    }
}

@Composable
private fun HeroConfidenceItem(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            color = accentColor.copy(alpha = 0.16f),
            shape = RoundedCornerShape(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accentColor,
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StepActionSection(
    step: OnboardingStepUiModel,
    primaryLabel: String,
    canContinue: Boolean,
    progress: Float,
    accentBrush: Brush,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    val actionSpacing = when {
        step.secondaryActionLabel == null -> 0.dp
        step.kind == OnboardingStepKind.Hero -> 18.dp
        step.kind == OnboardingStepKind.PremiumBridge -> 10.dp
        else -> 12.dp
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(actionSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProgressionButton(
            text = primaryLabel,
            progress = progress,
            enabled = canContinue,
            brush = accentBrush,
            contentColor = MaterialTheme.colorScheme.onSurface,
            onClick = onPrimary,
        )
        step.secondaryActionLabel?.let { secondary ->
            if (step.id == "dream" || step.id == "premium_bridge") {
                Text(
                    text = secondary,
                    style = if (step.id == "premium_bridge") {
                        MaterialTheme.typography.labelLarge
                    } else {
                        MaterialTheme.typography.labelMedium
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(CircleShape)
                        .clickable(onClick = onSecondary)
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                )
            } else {
                SecondaryButton(
                    text = secondary,
                    onClick = onSecondary,
                )
            }
        }
    }
}

@Composable
private fun OnboardingChoiceCard(
    stepId: String,
    title: String,
    description: String,
    leadingLabel: String?,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val containerColor = if (selected) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surface
    val iconTint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
    val iconContainerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val indicatorFill = if (selected) accentColor else MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        color = containerColor,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                color = iconContainerColor,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = onboardingChoiceIcon(stepId, title),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = iconTint,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (selected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        shape = CircleShape,
                    )
                    .background(
                        color = indicatorFill,
                        shape = CircleShape,
                    ),
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingScaleCard(
    stepId: String,
    choiceId: String,
    title: String,
    description: String,
    intensity: Float,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val containerColor = if (selected) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surface
    val iconContainerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val entrance = rememberEntranceProgress(key = "scale_${stepId}_$choiceId", delayMillis = (intensity * 220f).roundToInt())
    val animatedIntensity by animateFloatAsState(
        targetValue = lerp(0.12f, intensity.coerceIn(0.18f, 1f), entrance),
        animationSpec = tween(durationMillis = 700),
        label = "scale_fill_${stepId}_$choiceId",
    )
    val animatedIconScale = lerp(0.92f, 1f, entrance)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        color = containerColor,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier
                        .size(52.dp)
                        .graphicsLayer {
                            scaleX = animatedIconScale
                            scaleY = animatedIconScale
                            alpha = entrance
                        },
                    color = iconContainerColor,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = onboardingChoiceIcon(stepId, choiceId),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = if (selected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
                            shape = CircleShape,
                        )
                        .background(
                            color = if (selected) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surface,
                            shape = CircleShape,
                        ),
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(accentColor),
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedIntensity)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.7f),
                                    accentColor,
                                ),
                            ),
                        ),
                )
            }
        }
    }
}

@Composable
private fun StepHeading(step: OnboardingStepUiModel) {
    val headingSpacing = if (step.kind == OnboardingStepKind.Hero) 6.dp else 10.dp
    val centeredHeading = step.kind == OnboardingStepKind.Hero || step.kind == OnboardingStepKind.Snapshot
    val showEyebrow = when (step.kind) {
        OnboardingStepKind.NameCapture,
        OnboardingStepKind.ChoiceList,
        OnboardingStepKind.ScaleList,
        OnboardingStepKind.Snapshot,
        OnboardingStepKind.PremiumBridge -> false
        else -> step.kind != OnboardingStepKind.Hero
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(headingSpacing),
        horizontalAlignment = if (centeredHeading) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        if (showEyebrow) {
            Text(
                text = step.eyebrow,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = step.title,
            style = if (step.kind == OnboardingStepKind.Hero) {
                MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 31.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            } else {
                MaterialTheme.typography.headlineMedium.copy(
                    fontSize = if (step.kind == OnboardingStepKind.Snapshot) 28.sp else 26.sp,
                    lineHeight = if (step.kind == OnboardingStepKind.Snapshot) 33.sp else 31.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            },
            color = MaterialTheme.colorScheme.onSurface,
            modifier = if (centeredHeading) Modifier.widthIn(max = 420.dp) else Modifier,
            textAlign = if (centeredHeading) TextAlign.Center else TextAlign.Start,
        )
        if (step.body.isNotBlank()) {
            Text(
                text = step.body,
                style = if (step.kind == OnboardingStepKind.Hero) {
                    MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Medium,
                    )
                } else {
                    MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Medium,
                    )
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (centeredHeading) Modifier.widthIn(max = 360.dp) else Modifier,
                textAlign = if (centeredHeading) TextAlign.Center else TextAlign.Start,
            )
        }
    }
}

@Composable
private fun ProgressionButton(
    text: String,
    progress: Float,
    brush: Brush,
    contentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    val buttonHeight = 56.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(buttonHeight)
            .clip(shape),
        color = Color.Transparent,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (enabled) 1f else 0.56f),
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun CalloutGrid(
    callouts: List<OnboardingCalloutUiModel>,
    columns: Int,
    accentColor: Color,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = columns,
    ) {
        callouts.forEach { callout ->
            CalloutCard(
                callout = callout,
                modifier = Modifier.weight(1f, fill = true),
                accentColor = toneColor(callout.tone),
            )
        }
    }
}

@Composable
private fun SnapshotSection(
    callouts: List<OnboardingCalloutUiModel>,
) {
    if (callouts.isEmpty()) return

    val researchCallout = callouts.getOrNull(0)
    val startCallout = callouts.getOrNull(1)
    val automationCallout = callouts.getOrNull(2)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        researchCallout?.let { callout ->
            SnapshotPlanCard(
                modifier = Modifier.fillMaxWidth(),
                label = "Research says",
                callout = callout,
                icon = Icons.Rounded.TipsAndUpdates,
                targetProgress = 0.92f,
                animationKey = "snapshot_research",
                featured = true,
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 2,
        ) {
            startCallout?.let { callout ->
                SnapshotPlanCard(
                    modifier = Modifier.weight(1f, fill = true),
                    label = "Start with",
                    callout = callout,
                    icon = Icons.Rounded.ViewTimeline,
                    targetProgress = 0.78f,
                    animationKey = "snapshot_start",
                )
            }
            automationCallout?.let { callout ->
                SnapshotPlanCard(
                    modifier = Modifier.weight(1f, fill = true),
                    label = "ChorePilot will do",
                    callout = callout,
                    icon = Icons.Rounded.AutoAwesome,
                    targetProgress = 0.84f,
                    animationKey = "snapshot_help",
                )
            }
        }
    }
}

@Composable
private fun SnapshotPlanCard(
    modifier: Modifier = Modifier,
    label: String,
    callout: OnboardingCalloutUiModel,
    icon: ImageVector,
    targetProgress: Float,
    animationKey: String,
    featured: Boolean = false,
) {
    val accent = when (callout.tone) {
        DemoTone.Primary -> MaterialTheme.colorScheme.primary
        DemoTone.Secondary -> MaterialTheme.colorScheme.secondary
        DemoTone.Tertiary -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val entrance = rememberEntranceProgress(key = animationKey, delayMillis = if (featured) 40 else 120)
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 820, delayMillis = if (featured) 80 else 160),
        label = "${animationKey}_progress",
    )
    Surface(
        modifier = modifier.graphicsLayer {
            alpha = entrance
            scaleX = lerp(0.97f, 1f, entrance)
            scaleY = lerp(0.97f, 1f, entrance)
            translationY = (1f - entrance) * 20.dp.toPx()
        },
        color = if (featured) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = if (featured) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        shadowElevation = if (featured) 2.dp else 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accent.copy(alpha = if (featured) 0.22f else 0.18f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = if (featured) 16.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = accent.copy(alpha = if (featured) 0.14f else 0.12f),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(if (featured) 17.dp else 15.dp),
                        tint = accent,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = callout.title,
                        style = if (featured) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleSmall
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            Text(
                text = callout.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (featured) 8.dp else 6.dp)
                    .clip(CircleShape),
                color = accent,
                trackColor = accent.copy(alpha = 0.12f),
            )
        }
    }
}

@Composable
private fun InsightSummarySection(
    callouts: List<OnboardingCalloutUiModel>,
    accentColor: Color,
) {
    if (callouts.isEmpty()) return

    val emphasisProgress = animateFloatAsState(
        targetValue = 0.78f,
        animationSpec = tween(durationMillis = 900),
        label = "starting_point_progress",
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                accentColor.copy(alpha = 0.16f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val lead = callouts.first()
                Text(
                    text = lead.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InsightStatPill(
                        text = "76% clearer ownership",
                        accentColor = accentColor,
                    )
                    InsightStatPill(
                        text = "Built from your answers",
                        accentColor = MaterialTheme.colorScheme.secondary,
                    )
                }
                Text(
                    text = lead.description,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                )
                LinearProgressIndicator(
                    progress = { emphasisProgress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(CircleShape),
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.14f),
                )
            }
        }
        if (callouts.size > 1) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2,
            ) {
                callouts.drop(1).forEach { callout ->
                    CompactInsightCard(
                        callout = callout,
                        modifier = Modifier.weight(1f, fill = true),
                        accentColor = toneColor(callout.tone),
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightStatPill(
    text: String,
    accentColor: Color,
) {
    Surface(
        color = accentColor.copy(alpha = 0.12f),
        shape = CircleShape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = accentColor,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CompactInsightCard(
    callout: OnboardingCalloutUiModel,
    modifier: Modifier = Modifier,
    accentColor: Color,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accentColor.copy(alpha = 0.18f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = callout.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
            Text(
                text = callout.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalloutCard(
    callout: OnboardingCalloutUiModel,
    modifier: Modifier = Modifier,
    accentColor: Color,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = onboardingCalloutIcon(callout.title, callout.tone),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp),
                        tint = accentColor,
                    )
                }
                Text(
                    text = callout.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = callout.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PremiumBridgeBenefitsSection(
    callouts: List<OnboardingCalloutUiModel>,
) {
    val benefitAccent = Color(0xFF18C77A)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 1,
    ) {
        callouts.forEachIndexed { index, callout ->
            val entrance = rememberEntranceProgress(key = "premium_benefit_$index", delayMillis = 70 + (index * 70))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = entrance
                        scaleX = lerp(0.97f, 1f, entrance)
                        scaleY = lerp(0.97f, 1f, entrance)
                        translationY = (1f - entrance) * 18.dp.toPx()
                    },
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    benefitAccent.copy(alpha = 0.18f),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = benefitAccent,
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = callout.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = callout.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepArtwork(step: OnboardingStepUiModel) {
    val drawable = step.image ?: return
    val entrance = rememberEntranceProgress(key = "artwork_${step.id}", delayMillis = 30)
    val artworkHeight = when (step.kind) {
        OnboardingStepKind.Hero -> 320.dp
        OnboardingStepKind.NameCapture -> 264.dp
        OnboardingStepKind.Snapshot -> 208.dp
        OnboardingStepKind.Timeline -> 152.dp
        OnboardingStepKind.Trust -> 244.dp
        OnboardingStepKind.PremiumBridge -> 188.dp
        else -> 228.dp
    }
    val imageScale = when (step.kind) {
        OnboardingStepKind.Hero -> 1.1f
        OnboardingStepKind.NameCapture -> 1.06f
        OnboardingStepKind.Snapshot -> 1f
        OnboardingStepKind.Timeline -> 1.16f
        OnboardingStepKind.Trust -> 1.16f
        OnboardingStepKind.PremiumBridge -> 1f
        else -> 1f
    }
    val floatingTransition = rememberInfiniteTransition(label = "onboarding_artwork_${step.id}")
    val floatOffset by floatingTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (step.kind == OnboardingStepKind.NameCapture) -10f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "onboarding_artwork_float_${step.id}",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                when (step.kind) {
                    OnboardingStepKind.Hero -> 0.dp
                    OnboardingStepKind.Timeline -> 0.dp
                    else -> 10.dp
                },
            ),
    ) {
        val artworkShape = if (step.kind == OnboardingStepKind.Hero || step.kind == OnboardingStepKind.PremiumBridge) {
            MaterialTheme.shapes.extraLarge
        } else if (step.kind == OnboardingStepKind.Snapshot) {
            MaterialTheme.shapes.large
        } else {
            RectangleShape
        }
        Image(
            painter = painterResource(drawable),
            contentDescription = step.title,
            alignment = if (step.kind == OnboardingStepKind.Hero) {
                Alignment.TopCenter
            } else {
                Alignment.Center
            },
            contentScale = if (
                step.kind == OnboardingStepKind.Hero ||
                step.kind == OnboardingStepKind.Timeline ||
                step.kind == OnboardingStepKind.Trust
            ) {
                ContentScale.Crop
            } else if (step.kind == OnboardingStepKind.Snapshot) {
                ContentScale.Fit
            } else if (step.kind == OnboardingStepKind.PremiumBridge) {
                ContentScale.Fit
            } else {
                ContentScale.Fit
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(artworkHeight)
                .clip(artworkShape)
                .border(
                    width = 0.dp,
                    color = Color.Transparent,
                    shape = artworkShape,
                )
                .graphicsLayer {
                    alpha = entrance
                    scaleX = lerp(imageScale - 0.06f, imageScale, entrance)
                    scaleY = lerp(imageScale - 0.06f, imageScale, entrance)
                    translationY = ((1f - entrance) * 22.dp.toPx()) + if (step.kind == OnboardingStepKind.NameCapture) {
                        floatOffset
                    } else {
                        0f
                    }
                },
        )
    }
}

@Composable
private fun rememberEntranceProgress(
    key: String,
    delayMillis: Int = 0,
    durationMillis: Int = 420,
): Float {
    var visible by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        visible = false
        if (delayMillis > 0) {
            delay(delayMillis.toLong())
        }
        visible = true
    }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = durationMillis),
        label = "${key}_entrance",
    )
    return progress
}

@Composable
private fun NameCaptureSection(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "First name",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = MaterialTheme.shapes.small,
                            )
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        if (value.isBlank()) {
                            Text(
                                text = "Erick",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}

@Composable
private fun TimelineSection(
    items: List<Triple<String, String, String>>,
    accentColor: Color,
) {
    val lineProgress = animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 950),
        label = "timeline_line_progress",
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SolidTimelineBadge(
                        label = item.first,
                        accentColor = accentColor,
                    )
                    if (index != items.lastIndex) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .widthIn(min = 2.dp)
                                .height((54.dp * lineProgress.value).coerceAtLeast(0.dp))
                                .background(accentColor.copy(alpha = 0.28f)),
                        )
                    }
                }
                TimelineMilestoneCard(
                    dayLabel = item.first,
                    title = item.second,
                    description = item.third,
                    modifier = Modifier.weight(1f),
                    accentColor = accentColor,
                )
            }
        }
    }
}

@Composable
private fun SolidTimelineBadge(
    label: String,
    accentColor: Color,
) {
    Surface(
        color = accentColor,
        shape = CircleShape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TimelineMilestoneCard(
    dayLabel: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    accentColor: Color,
) {
    val progress = animateFloatAsState(
        targetValue = when (dayLabel) {
            "Day 1" -> 0.34f
            "Day 3" -> 0.68f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 850),
        label = "timeline_$dayLabel",
    )
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accentColor.copy(alpha = 0.18f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = timelineIcon(title),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(16.dp),
                        tint = accentColor,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape),
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.14f),
            )
        }
    }
}

@Composable
private fun ReviewSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        text = "How does this first pass feel so far?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = "If ChorePilot already feels thoughtful, you can leave a quick rating now or keep moving.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp, horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(5) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                Text(
                    text = "Only if it feels true",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun primaryBrush(step: OnboardingStepUiModel): Brush = when (step.kind) {
    OnboardingStepKind.PremiumBridge,
    OnboardingStepKind.Review -> Brush.linearGradient(
        listOf(
            Color(0xFFF59E0B),
            Color(0xFFF7C35F),
        ),
    )

    OnboardingStepKind.Trust,
    OnboardingStepKind.Snapshot -> Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.78f),
        ),
    )

    else -> Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            Color(0xFF7C74FF),
        ),
    )
}

@Composable
private fun stepArtworkBrush(step: OnboardingStepUiModel): Brush = when (step.kind) {
    OnboardingStepKind.PremiumBridge -> Brush.verticalGradient(
        listOf(Color(0xFF172746), Color(0xFF0F1A31)),
    )

    OnboardingStepKind.Trust,
    OnboardingStepKind.Snapshot -> Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
        ),
    )

    else -> Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        ),
    )
}

@Composable
private fun onboardingAccent(step: OnboardingStepUiModel): Color = when (step.kind) {
    OnboardingStepKind.Trust,
    OnboardingStepKind.Snapshot -> MaterialTheme.colorScheme.secondary

    OnboardingStepKind.Review,
    OnboardingStepKind.PremiumBridge -> MaterialTheme.colorScheme.tertiary

    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun toneColor(tone: DemoTone): Color = when (tone) {
    DemoTone.Primary -> MaterialTheme.colorScheme.primary
    DemoTone.Secondary -> MaterialTheme.colorScheme.secondary
    DemoTone.Tertiary -> MaterialTheme.colorScheme.tertiary
    DemoTone.Success -> Color(0xFF16A34A)
    DemoTone.Warning -> MaterialTheme.colorScheme.tertiary
    DemoTone.Neutral -> MaterialTheme.colorScheme.primary
}

private fun onboardingStepIcon(kind: OnboardingStepKind): ImageVector = when (kind) {
    OnboardingStepKind.Hero -> Icons.Rounded.AutoAwesome
    OnboardingStepKind.NameCapture -> Icons.Rounded.Person
    OnboardingStepKind.ChoiceList -> Icons.Rounded.TipsAndUpdates
    OnboardingStepKind.ScaleList -> Icons.Rounded.Psychology
    OnboardingStepKind.Snapshot -> Icons.Rounded.Insights
    OnboardingStepKind.InsightSummary -> Icons.Rounded.Lightbulb
    OnboardingStepKind.Timeline -> Icons.Rounded.ViewTimeline
    OnboardingStepKind.Trust -> Icons.Rounded.Shield
    OnboardingStepKind.Review -> Icons.Rounded.Star
    OnboardingStepKind.PremiumBridge -> Icons.Rounded.AutoAwesome
}

private fun onboardingCalloutIcon(title: String, tone: DemoTone): ImageVector = when {
    title.contains("fair", ignoreCase = true) -> Icons.Rounded.Groups
    title.contains("calm", ignoreCase = true) -> Icons.Rounded.Schedule
    title.contains("tailored", ignoreCase = true) -> Icons.Rounded.AutoAwesome
    title.contains("default", ignoreCase = true) -> Icons.Rounded.TipsAndUpdates
    title.contains("solve", ignoreCase = true) -> Icons.Rounded.Checklist
    title.contains("start", ignoreCase = true) -> Icons.Rounded.Lightbulb
    title.contains("protect", ignoreCase = true) -> Icons.Rounded.Shield
    title.contains("private", ignoreCase = true) -> Icons.Rounded.Lock
    title.contains("parent", ignoreCase = true) -> Icons.Rounded.Person
    title.contains("weekly", ignoreCase = true) -> Icons.Rounded.ViewTimeline
    tone == DemoTone.Secondary -> Icons.Rounded.Insights
    tone == DemoTone.Tertiary -> Icons.Rounded.Star
    else -> Icons.Rounded.Checklist
}

private fun timelineIcon(title: String): ImageVector = when {
    title.contains("owner", ignoreCase = true) -> Icons.Rounded.Person
    title.contains("friction", ignoreCase = true) -> Icons.Rounded.AutoAwesome
    title.contains("calmer", ignoreCase = true) -> Icons.Rounded.Schedule
    title.contains("week", ignoreCase = true) -> Icons.Rounded.ViewTimeline
    else -> Icons.Rounded.Checklist
}

private fun onboardingChoiceIcon(stepId: String, key: String): ImageVector = when {
    stepId == "household_type" && key.contains("family", ignoreCase = true) -> Icons.Rounded.Person
    stepId == "household_type" && key.contains("shared", ignoreCase = true) -> Icons.Rounded.Groups
    stepId == "household_type" && key.contains("dorm", ignoreCase = true) -> Icons.Rounded.Star
    stepId == "household_type" && key.contains("office", ignoreCase = true) -> Icons.Rounded.Checklist

    stepId == "near_outcome" && key.contains("fair", ignoreCase = true) -> Icons.Rounded.Groups
    stepId == "near_outcome" && key.contains("reminder", ignoreCase = true) -> Icons.Rounded.Checklist
    stepId == "near_outcome" && key.contains("calm", ignoreCase = true) -> Icons.Rounded.Schedule
    stepId == "near_outcome" && key.contains("independence", ignoreCase = true) -> Icons.Rounded.Star

    stepId == "identity" && key.contains("mental", ignoreCase = true) -> Icons.Rounded.Psychology
    stepId == "identity" && key.contains("team", ignoreCase = true) -> Icons.Rounded.Groups
    stepId == "identity" && key.contains("argument", ignoreCase = true) -> Icons.Rounded.Shield
    stepId == "identity" && key.contains("rhythm", ignoreCase = true) -> Icons.Rounded.ViewTimeline

    stepId == "mental_load" && key == "light" -> Icons.Rounded.Lightbulb
    stepId == "mental_load" && key == "busy" -> Icons.Rounded.Schedule
    stepId == "mental_load" && key == "heavy" -> Icons.Rounded.Checklist
    stepId == "mental_load" && key == "chaotic" -> Icons.Rounded.TipsAndUpdates
    stepId == "mental_load" && key == "burnout" -> Icons.Rounded.Psychology

    stepId == "breakdown" && key.contains("forgot", ignoreCase = true) -> Icons.Rounded.Checklist
    stepId == "breakdown" && key.contains("uneven", ignoreCase = true) -> Icons.Rounded.Groups
    stepId == "breakdown" && key.contains("ownership", ignoreCase = true) -> Icons.Rounded.Person
    stepId == "breakdown" && key.contains("accountability", ignoreCase = true) -> Icons.Rounded.Shield

    stepId == "feature_relief" && key.contains("assign", ignoreCase = true) -> Icons.Rounded.Groups
    stepId == "feature_relief" && key.contains("proof", ignoreCase = true) -> Icons.Rounded.Shield
    stepId == "feature_relief" && key.contains("reminder", ignoreCase = true) -> Icons.Rounded.Schedule
    stepId == "feature_relief" && key.contains("report", ignoreCase = true) -> Icons.Rounded.Insights

    stepId == "attribution" && key.contains("tiktok", ignoreCase = true) -> Icons.Rounded.AutoAwesome
    stepId == "attribution" && key.contains("youtube", ignoreCase = true) -> Icons.Rounded.ViewTimeline
    stepId == "attribution" && key.contains("friend", ignoreCase = true) -> Icons.Rounded.Groups
    stepId == "attribution" && key.contains("store", ignoreCase = true) -> Icons.Rounded.Star

    else -> Icons.Rounded.Lightbulb
}

@Composable
private fun OnboardingStepPreview(
    stepIndex: Int,
    displayName: String = "Erick",
) {
    ChorePilotPreviewTheme {
        PreviewDeviceFrame { modifier ->
            Box(modifier = modifier.fillMaxSize()) {
                OnboardingScreen(
                    state = ChorePilotPreviewData.onboardingState(stepIndex = stepIndex, displayName = displayName),
                    onEvent = {},
                    onOpenAuth = {},
                    onOpenPremium = {},
                    onFinish = {},
                )
            }
        }
    }
}

@Preview(name = "Onboarding 01 Dream", widthDp = 393, heightDp = 852)
@Composable
private fun OnboardingDreamPreview() = OnboardingStepPreview(stepIndex = 0)

@Preview(name = "Onboarding 01 Dream Dark", widthDp = 393, heightDp = 852)
@Composable
private fun OnboardingDreamDarkPreview() {
    ChorePilotTheme(themeMode = ThemeMode.Dark) {
        PreviewDeviceFrame { modifier ->
            Box(modifier = modifier.fillMaxSize()) {
                OnboardingScreen(
                    state = ChorePilotPreviewData.onboardingState(stepIndex = 0, displayName = "Erick"),
                    onEvent = {},
                    onOpenAuth = {},
                    onOpenPremium = {},
                    onFinish = {},
                )
            }
        }
    }
}

@Preview(name = "Onboarding 02 Household Type", widthDp = 393, heightDp = 852)
@Composable
private fun OnboardingHouseholdTypePreview() = OnboardingStepPreview(stepIndex = 1)

@Preview(name = "Onboarding 03 Name", widthDp = 393, heightDp = 852)
@Composable
private fun OnboardingNamePreview() = OnboardingStepPreview(stepIndex = 2)

@Preview(name = "Onboarding 04 Outcome", widthDp = 393, heightDp = 852)
@Composable
private fun OnboardingOutcomePreview() = OnboardingStepPreview(stepIndex = 3)

@Preview(name = "Onboarding 05 Identity", widthDp = 393, heightDp = 852)
@Composable
private fun OnboardingIdentityPreview() = OnboardingStepPreview(stepIndex = 4)

@Preview(name = "Onboarding 06 Mental Load", widthDp = 393, heightDp = 852)
@Composable
private fun OnboardingMentalLoadPreview() = OnboardingStepPreview(stepIndex = 5)

@Preview(name = "Onboarding 07 Breakdown", widthDp = 393, heightDp = 852)
@Composable
private fun OnboardingBreakdownPreview() = OnboardingStepPreview(stepIndex = 6)

@Preview(name = "Onboarding 08 Feature Relief", widthDp = 393, heightDp = 852)
@Composable
private fun OnboardingFeatureReliefPreview() = OnboardingStepPreview(stepIndex = 7)

@Preview(name = "Onboarding 09 Attribution", widthDp = 393, heightDp = 852)
@Composable
private fun OnboardingAttributionPreview() = OnboardingStepPreview(stepIndex = 8)

@Preview(name = "Onboarding 10 Snapshot", widthDp = 393, heightDp = 852)
@Composable
private fun OnboardingSnapshotPreview() = OnboardingStepPreview(stepIndex = 9)

@Preview(name = "Onboarding 11 Starting Point", widthDp = 393, heightDp = 852)
@Composable
private fun OnboardingStartingPointPreview() = OnboardingStepPreview(stepIndex = 10)

@Preview(name = "Onboarding 12 Roadmap", widthDp = 393, heightDp = 852)
@Composable
private fun OnboardingRoadmapPreview() = OnboardingStepPreview(stepIndex = 11)

@Preview(name = "Onboarding 13 Trust", widthDp = 393, heightDp = 852)
@Composable
private fun OnboardingTrustPreview() = OnboardingStepPreview(stepIndex = 12)

@Preview(name = "Onboarding 14 Review", widthDp = 393, heightDp = 852)
@Composable
private fun OnboardingReviewPreview() = OnboardingStepPreview(stepIndex = 13)

@Preview(name = "Onboarding 15 Premium", widthDp = 393, heightDp = 852)
@Composable
private fun OnboardingPremiumPreview() = OnboardingStepPreview(stepIndex = 14)
