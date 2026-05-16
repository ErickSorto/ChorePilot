package com.sortoapps.chorepilot.features.premium.presentation.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.sortoapps.chorepilot.core.designsystem.ChorePilotGradientBackground
import com.sortoapps.chorepilot.core.designsystem.GradientButton
import com.sortoapps.chorepilot.core.designsystem.SecondaryButton
import com.sortoapps.chorepilot.core.revenuecat.SubscriptionPlanSnapshot
import com.sortoapps.chorepilot.features.premium.presentation.viewmodel.PremiumCalloutUiModel
import com.sortoapps.chorepilot.features.premium.presentation.viewmodel.PremiumPlanOption
import com.sortoapps.chorepilot.features.premium.presentation.viewmodel.PremiumStageId
import com.sortoapps.chorepilot.features.premium.presentation.viewmodel.PremiumStageUiModel
import com.sortoapps.chorepilot.features.premium.presentation.viewmodel.PremiumTimelineItemUiModel
import com.sortoapps.chorepilot.features.premium.presentation.viewmodel.PremiumUiEvent
import com.sortoapps.chorepilot.features.premium.presentation.viewmodel.PremiumUiState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.delay

@Composable
fun PremiumScreen(
    state: PremiumUiState,
    onEvent: (PremiumUiEvent) -> Unit,
    onBack: () -> Unit,
    onContinueToAuth: () -> Unit,
) {
    LaunchedEffect(state.shouldCompleteFlow) {
        if (state.shouldCompleteFlow) {
            onContinueToAuth()
            onEvent(PremiumUiEvent.FlowCompletionHandled)
        }
    }

    val stage = state.currentStage
    val scrollState = rememberScrollState()
    val stageSpacing = stageSpacing(stage.id)
    val contentArrangement = when (stage.id) {
        PremiumStageId.Promise,
        PremiumStageId.TrialClarity,
        PremiumStageId.Unlocks -> Arrangement.spacedBy(stageSpacing, Alignment.CenterVertically)
        else -> Arrangement.spacedBy(stageSpacing)
    }
    LaunchedEffect(stage.id) {
        scrollState.scrollTo(0)
        if (stage.id == PremiumStageId.OfferSheet) {
            delay(3150)
            scrollState.animateScrollTo(
                value = scrollState.maxValue,
                animationSpec = tween(durationMillis = 2200),
            )
        }
    }
    ChorePilotGradientBackground {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(
                    top = if (stage.id == PremiumStageId.OfferSheet) 0.dp else 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
        ) {
            if (stage.id == PremiumStageId.OfferSheet) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .zIndex(2f)
                        .padding(start = 4.dp, top = 8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.42f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.36f),
                            shape = CircleShape,
                        )
                        .clickable {
                            onEvent(PremiumUiEvent.TriggerVibration)
                            onBack()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 560.dp)
                    .align(Alignment.Center)
                    .verticalScroll(scrollState)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = contentArrangement,
            ) {
                stage.image?.let { drawable ->
                    if (stage.id == PremiumStageId.TrialClarity) {
                        TrialClarityHero(
                            drawable = drawable,
                            contentDescription = stage.headline,
                        )
                    } else if (stage.id == PremiumStageId.Unlocks) {
                        UnlocksHero(
                            drawable = drawable,
                            contentDescription = stage.headline,
                        )
                    } else if (stage.id == PremiumStageId.OfferSheet) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth(0.96f)
                                    .height(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0x00F4A646),
                                                Color(0x1EF4A646),
                                                Color(0x46E9D6BE),
                                            ),
                                        ),
                                    ),
                            )
                            Image(
                                painter = painterResource(drawable),
                                contentDescription = stage.headline,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        translationY = -8f
                                    }
                                    .height(heroHeight(stage.id)),
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(stageVisualBrush(stage))
                                .padding(
                                    when (stage.id) {
                                        PremiumStageId.Promise -> 8.dp
                                        PremiumStageId.Unlocks -> 4.dp
                                        else -> 10.dp
                                    },
                                ),
                        ) {
                            Image(
                                painter = painterResource(drawable),
                                contentDescription = stage.headline,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(heroHeight(stage.id)),
                            )
                        }
                    }
                }

                if (stage.id != PremiumStageId.OfferSheet) {
                    PremiumHeading(stage = stage)
                }

                when (stage.id) {
                    PremiumStageId.Promise,
                    PremiumStageId.GiftReveal -> {
                        BalancedCalloutGrid(callouts = stage.callouts)
                    }

                    PremiumStageId.TrialClarity -> {
                        
                    }

                    PremiumStageId.Unlocks -> {
                        CompactUnlocksSection(callouts = stage.callouts)
                    }

                    PremiumStageId.OfferSheet -> {
                        OfferSheetSection(
                            selectedPlan = state.selectedPlan,
                            monthlyPlan = state.monthlyPlan,
                            yearlyPlan = state.yearlyPlan,
                            isSubscriptionLoading = state.isSubscriptionLoading,
                            hasChorePilotPro = state.hasChorePilotPro,
                            onPlanSelected = {
                                onEvent(PremiumUiEvent.TriggerVibration)
                                onEvent(PremiumUiEvent.PlanSelected(it))
                            },
                        )
                    }

                    PremiumStageId.OneTimeOffer -> {
                        OneTimeOfferSection(callouts = stage.callouts, priceLabel = stage.body)
                    }
                }

                if (stage.id == PremiumStageId.OfferSheet) {
                    OfferCtaButton(
                        text = premiumPrimaryLabel(
                            state = state,
                            stage = stage,
                        ),
                        brush = premiumBrush(stage),
                        showBanner = !state.isPurchaseInProgress,
                        onClick = {
                            onEvent(PremiumUiEvent.TriggerVibration)
                            onEvent(PremiumUiEvent.PrimaryActionClicked)
                        },
                    )
                } else {
                    GradientButton(
                        text = premiumPrimaryLabel(
                            state = state,
                            stage = stage,
                        ),
                        modifier = Modifier.padding(
                            top = 16.dp,
                            bottom = when (stage.id) {
                                PremiumStageId.Unlocks -> 6.dp
                                else -> 16.dp
                            },
                        ),
                        brush = premiumBrush(stage),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        textStyle = MaterialTheme.typography.labelLarge,
                        onClick = {
                            onEvent(PremiumUiEvent.TriggerVibration)
                            onEvent(PremiumUiEvent.PrimaryActionClicked)
                        },
                    )
                }

                state.subscriptionStatusMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                state.subscriptionErrorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                if (stage.id == PremiumStageId.OfferSheet) {
                    OfferAppliedPill()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                onEvent(PremiumUiEvent.TriggerVibration)
                                onEvent(PremiumUiEvent.RestorePurchasesClicked)
                            },
                        ) {
                            Text(
                                text = "Restore Purchases",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { }) {
                            Text(
                                text = "Terms",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { }) {
                            Text(
                                text = "Privacy",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfferCtaButton(
    text: String,
    brush: Brush,
    showBanner: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        GradientButton(
            text = text,
            modifier = Modifier.padding(top = 18.dp),
            brush = brush,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            textStyle = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            shape = RoundedCornerShape(
                topStart = 14.dp,
                bottomStart = 14.dp,
                topEnd = 8.dp,
                bottomEnd = 8.dp,
            ),
            onClick = onClick,
        )
        if (showBanner) {
            SwingingSavingsTag(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = 25.dp),
            )
        }
    }
}

@Composable
private fun SwingingSavingsTag(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(width = 110.dp, height = 38.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val topInset = 3f
            val bottomInset = height - 3f
            val pointStart = 18f
            val pointTip = 0f
            val rightInset = width - 3f

            val banner = Path().apply {
                moveTo(pointStart, topInset)
                lineTo(rightInset - 9f, topInset)
                quadraticTo(rightInset, topInset, rightInset, 12f)
                lineTo(rightInset, height - 12f)
                quadraticTo(rightInset, bottomInset, rightInset - 9f, bottomInset)
                lineTo(pointStart, bottomInset)
                lineTo(pointTip, height / 2f)
                close()
            }

            val shadow = Path().apply {
                moveTo(pointStart + 4f, topInset + 4f)
                lineTo(rightInset - 5f, topInset + 4f)
                quadraticTo(rightInset + 1f, topInset + 4f, rightInset + 1f, 14f)
                lineTo(rightInset + 1f, height - 10f)
                quadraticTo(rightInset + 1f, bottomInset + 4f, rightInset - 5f, bottomInset + 4f)
                lineTo(pointStart + 4f, bottomInset + 4f)
                lineTo(pointTip + 5f, (height / 2f) + 4f)
                close()
            }

            drawPath(
                path = shadow,
                color = Color(0x22160000),
            )

            drawPath(
                path = banner,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFFEA4A58),
                        Color(0xFFC81F36),
                    ),
                ),
            )
            drawPath(
                path = banner,
                color = Color.White.copy(alpha = 0.82f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
            )
        }

        Text(
            text = "SAVE 80%",
            modifier = Modifier.padding(start = 15.dp, end = 10.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.15.sp,
            ),
            color = Color.White,
        )
    }
}

@Composable
private fun PremiumHeading(stage: PremiumStageUiModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(if (stage.id == PremiumStageId.OfferSheet) 4.dp else 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (stage.eyebrow.isNotBlank()) {
            Text(
                text = stage.eyebrow,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = if (stage.id == PremiumStageId.OfferSheet) 12.sp else 13.sp,
                    lineHeight = if (stage.id == PremiumStageId.OfferSheet) 15.sp else 16.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = stage.headline,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = if (stage.id == PremiumStageId.OfferSheet) 21.sp else 24.sp,
                lineHeight = if (stage.id == PremiumStageId.OfferSheet) 25.sp else 29.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            textAlign = TextAlign.Center,
        )
        if (stage.body.isNotBlank() && stage.id != PremiumStageId.OneTimeOffer) {
            Text(
                text = stage.body,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TrialClarityHero(
    drawable: DrawableResource,
    contentDescription: String,
) {
    val heroMotion = rememberInfiniteTransition(label = "trial_clarity_hero")
    val floatOffset by heroMotion.animateFloat(
        initialValue = 0f,
        targetValue = -14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "trial_clarity_float",
    )
    val heroScale by heroMotion.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "trial_clarity_scale",
    )
    val sparkleAlpha by heroMotion.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "trial_clarity_sparkle_alpha",
    )
    val sparkleScale by heroMotion.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "trial_clarity_sparkle_scale",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFFCF6),
                        Color(0xFFFFF3DD),
                        Color(0xFFF7F6FF),
                    ),
                ),
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.9f)
                .height(184.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x5CFDC830),
                            Color(0x2CF6A64D),
                            Color(0x00FFFFFF),
                        ),
                    ),
                ),
        )
        PremiumSparkle(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 26.dp, top = 28.dp),
            alpha = sparkleAlpha,
            scale = sparkleScale,
            tint = Color(0xFFF6B746),
        )
        PremiumSparkle(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 36.dp, top = 56.dp),
            alpha = sparkleAlpha * 0.8f,
            scale = sparkleScale * 0.82f,
            tint = Color(0xFF6FD5FF),
        )
        PremiumSparkle(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 28.dp, top = 8.dp),
            alpha = sparkleAlpha * 0.9f,
            scale = sparkleScale * 0.72f,
            tint = Color(0xFFFDD05A),
        )
        Image(
            painter = painterResource(drawable),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight(PremiumStageId.TrialClarity))
                .graphicsLayer {
                    translationY = floatOffset
                    scaleX = heroScale
                    scaleY = heroScale
                },
        )
    }
}

@Composable
private fun PremiumSparkle(
    modifier: Modifier,
    alpha: Float,
    scale: Float,
    tint: Color,
) {
    Icon(
        imageVector = Icons.Rounded.AutoAwesome,
        contentDescription = null,
        tint = tint.copy(alpha = alpha),
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    )
}

@Composable
private fun UnlocksHero(
    drawable: DrawableResource,
    contentDescription: String,
) {
    val heroMotion = rememberInfiniteTransition(label = "unlocks_hero")
    val floatOffset by heroMotion.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "unlocks_float",
    )
    val glowAlpha by heroMotion.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "unlocks_glow_alpha",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFFCF5),
                        Color(0xFFFFF2DB),
                        Color(0xFFF5F7FF),
                    ),
                ),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.92f)
                .height(188.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFDC830).copy(alpha = glowAlpha),
                            Color(0xFFF37335).copy(alpha = glowAlpha * 0.42f),
                            Color(0x00FFFFFF),
                        ),
                    ),
                ),
        )
        FloatingWinBadge(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 32.dp),
            icon = Icons.Rounded.Insights,
            tint = Color(0xFFF08A36),
            background = Color(0xFFFFF2DE),
            index = 0,
        )
        FloatingWinBadge(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 20.dp, top = 38.dp),
            icon = Icons.Rounded.Shield,
            tint = Color(0xFF5B7CFF),
            background = Color(0xFFEEF2FF),
            index = 1,
        )
        FloatingWinBadge(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp, top = 26.dp),
            icon = Icons.Rounded.AutoAwesome,
            tint = Color(0xFF14A6B8),
            background = Color(0xFFE7FBFD),
            index = 2,
        )
        Image(
            painter = painterResource(drawable),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight(PremiumStageId.Unlocks))
                .graphicsLayer {
                    translationY = floatOffset
                },
        )
    }
}

@Composable
private fun FloatingWinBadge(
    modifier: Modifier,
    icon: ImageVector,
    tint: Color,
    background: Color,
    index: Int,
) {
    val transition = rememberInfiniteTransition(label = "floating_badge_$index")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (index % 2 == 0) -8f else -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700 + (index * 160)),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "badge_offset_$index",
    )
    Surface(
        modifier = modifier.graphicsLayer {
            translationY = offset
        },
        color = background,
        shape = CircleShape,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = tint.copy(alpha = 0.18f),
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .padding(10.dp)
                .size(18.dp),
        )
    }
}

@Composable
private fun PremiumCalloutCard(
    callout: PremiumCalloutUiModel,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (index % 2 == 0) {
        Color(0xFFFFFAF1)
    } else {
        Color(0xFFF7F8FF)
    }
    val borderColor = if (index % 2 == 0) {
        Color(0xFFF2D3A4)
    } else {
        Color(0xFFD9E0FF)
    }
    val iconTint = if (index % 2 == 0) {
        Color(0xFFF08B35)
    } else {
        Color(0xFF6F7CFF)
    }
    val iconBackground = if (index % 2 == 0) {
        Color(0xFFFFF0D7)
    } else {
        Color(0xFFEEF1FF)
    }
    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.25.dp,
            color = borderColor,
        ),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 126.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = iconBackground,
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = premiumCalloutIcon(callout.title),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(9.dp)
                            .size(18.dp),
                        tint = iconTint,
                    )
                }
                Text(
                    text = callout.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                    ),
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = callout.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BalancedCalloutGrid(
    callouts: List<PremiumCalloutUiModel>,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 2,
    ) {
        callouts.forEachIndexed { index, callout ->
            PremiumCalloutCard(
                modifier = Modifier.weight(1f, fill = true),
                callout = callout,
                index = index,
            )
        }
    }
}

@Composable
private fun CompactUnlocksSection(
    callouts: List<PremiumCalloutUiModel>,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 2,
    ) {
        callouts.forEachIndexed { index, callout ->
            PremiumUnlockCard(
                modifier = Modifier.weight(1f, fill = true),
                callout = callout,
                index = index,
            )
        }
    }
}

@Composable
private fun PremiumUnlockCard(
    callout: PremiumCalloutUiModel,
    index: Int,
    modifier: Modifier = Modifier,
) {
    val palette = unlockPalette(index)
    val motion = rememberInfiniteTransition(label = "unlock_card_$index")
    val floatOffset by motion.animateFloat(
        initialValue = 0f,
        targetValue = if (index % 2 == 0) -6f else -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800 + (index * 120)),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "unlock_card_float_$index",
    )
    val glowAlpha by motion.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500 + (index * 90)),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "unlock_card_glow_$index",
    )
    val iconScale by motion.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100 + (index * 70)),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "unlock_card_icon_$index",
    )

    Box(
        modifier = modifier.graphicsLayer {
            translationY = floatOffset
        },
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(MaterialTheme.shapes.medium)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            palette.accent.copy(alpha = glowAlpha),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(102.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(
                    Brush.verticalGradient(
                        listOf(palette.backgroundStart, palette.backgroundEnd),
                    ),
                )
                .border(
                    width = 1.25.dp,
                    color = palette.border,
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    Surface(
                        color = palette.iconBackground,
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = premiumCalloutIcon(callout.title),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(18.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                },
                            tint = palette.accent,
                        )
                    }
                    PremiumSparkle(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 2.dp, end = 2.dp),
                        alpha = glowAlpha + 0.25f,
                        scale = 0.62f,
                        tint = palette.accent,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = callout.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 15.sp,
                            lineHeight = 18.sp,
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = callout.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun OfferSheetSection(
    selectedPlan: PremiumPlanOption,
    monthlyPlan: SubscriptionPlanSnapshot?,
    yearlyPlan: SubscriptionPlanSnapshot?,
    isSubscriptionLoading: Boolean,
    hasChorePilotPro: Boolean,
    onPlanSelected: (PremiumPlanOption) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ReminderTimelineSection()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PremiumPlanCard(
                modifier = Modifier.weight(1f),
                title = "Yearly",
                priceLabel = yearlyPlan?.compactMonthlyEquivalentLabel() ?: "$2.50/mo",
                billingLabel = "Billed as ${yearlyPlan?.priceLabel ?: "$29.99"} yearly",
                selected = selectedPlan == PremiumPlanOption.Annual,
                onClick = { onPlanSelected(PremiumPlanOption.Annual) },
            )
            PremiumPlanCard(
                modifier = Modifier.weight(1f),
                title = "Monthly",
                priceLabel = monthlyPlan?.compactBillingLabel("/mo") ?: "$4.13/mo",
                billingLabel = "Billed as ${monthlyPlan?.priceLabel ?: "$4.13"} monthly",
                selected = selectedPlan == PremiumPlanOption.Monthly,
                onClick = { onPlanSelected(PremiumPlanOption.Monthly) },
            )
        }

        if (!isSubscriptionLoading) {
            Text(
                text = offerFootnoteText(
                    hasChorePilotPro = hasChorePilotPro,
                    selectedPlan = selectedPlan,
                    monthlyPlan = monthlyPlan,
                    yearlyPlan = yearlyPlan,
                ),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun OfferAppliedPill() {
    Surface(
        color = Color(0xFFF4FFF7),
        shape = CircleShape,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color(0xFFBFE9CB),
        ),
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF2CCB73),
                                Color(0xFF159957),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
            Text(
                text = "Offer Applied",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = Color(0xFF159957),
            )
        }
    }
}

private fun offerFootnoteText(
    hasChorePilotPro: Boolean,
    selectedPlan: PremiumPlanOption,
    monthlyPlan: SubscriptionPlanSnapshot?,
    yearlyPlan: SubscriptionPlanSnapshot?,
): String {
    if (hasChorePilotPro) {
        return "ChorePilot Pro is already active for this household."
    }

    return when (selectedPlan) {
        PremiumPlanOption.Annual -> {
            "Unlimited free access for 7 days, then ${yearlyPlan?.priceLabel ?: "$29.99"}/year"
        }

        PremiumPlanOption.Monthly -> {
            "Unlimited free access for 7 days, then ${monthlyPlan?.priceLabel ?: "$4.13"}/month"
        }
    }
}

private fun SubscriptionPlanSnapshot.compactBillingLabel(suffix: String): String =
    priceLabel.takeIf(String::isNotBlank)?.let { "$it$suffix" } ?: title

private fun SubscriptionPlanSnapshot.compactMonthlyEquivalentLabel(): String =
    perMonthLabel
        ?.substringBefore(" per month")
        ?.takeIf(String::isNotBlank)
        ?.let { "$it/mo" }
        ?: compactBillingLabel("/yr")

@Composable
private fun ReminderTimelineSection() {
    SimpleReminderTimelineSection(
        items = listOf(
            PremiumTimelineItemUiModel(
                label = "Today",
                title = "Start premium",
                description = "Get full, unlimited access right away.",
            ),
            PremiumTimelineItemUiModel(
                label = "In 5 days",
                title = "Reminder",
                description = "We’ll send a reminder before your trial ends.",
            ),
            PremiumTimelineItemUiModel(
                label = "In 7 days",
                title = "Trial ends",
                description = "You will be charged. You can still cancel anytime before.",
            ),
        ),
    )
}

@Composable
private fun SimpleReminderTimelineSection(
    items: List<PremiumTimelineItemUiModel>,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "We’ll send you a reminder\nbefore your FREE trial ends",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.forEachIndexed { index, item ->
                SimpleReminderTimelineRow(
                    index = index,
                    item = item,
                    isLast = index == items.lastIndex,
                    showGlow = index == items.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun SimpleReminderTimelineRow(
    index: Int,
    item: PremiumTimelineItemUiModel,
    isLast: Boolean,
    showGlow: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            SimpleReminderNode(
                index = index,
                icon = if (showGlow) Icons.Rounded.Stars else premiumTimelineIcon(item.title),
                showGlow = showGlow,
            )
            if (!isLast) {
                val lineProgress = androidx.compose.runtime.remember(index) { Animatable(0f) }
                LaunchedEffect(index) {
                    lineProgress.snapTo(0f)
                    delay(400L + (index * 980L))
                    lineProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 760),
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .widthIn(min = 10.dp, max = 10.dp)
                        .height(54.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(min = 4.dp, max = 4.dp)
                            .height(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFCBC6FF).copy(alpha = 0.36f)),
                    )
                    Box(
                        modifier = Modifier
                            .widthIn(min = 6.dp, max = 6.dp)
                            .height(54.dp * lineProgress.value)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF8B84FF),
                                        Color(0xFF6E69F8),
                                    ),
                                ),
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 42.dp * lineProgress.value)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF7B75F8)),
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (item.description.isNotBlank()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SimpleReminderNode(
    index: Int,
    icon: ImageVector,
    showGlow: Boolean,
) {
    val glowTransition = rememberInfiniteTransition(label = "premium_star_glow")
    val nodeScale = androidx.compose.runtime.remember(index) { Animatable(0.72f) }
    val nodeOffset = androidx.compose.runtime.remember(index) { Animatable(16f) }
    val nodeAlpha = androidx.compose.runtime.remember(index) { Animatable(0.25f) }
    LaunchedEffect(index) {
        val delayMillis = when (index) {
            0 -> 80L
            1 -> 1020L
            else -> 1840L
        }
        delay(delayMillis)
        nodeAlpha.animateTo(1f, animationSpec = tween(durationMillis = 220))
        nodeScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.5f,
                stiffness = 420f,
            ),
        )
        nodeOffset.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = 0.58f,
                stiffness = 360f,
            ),
        )
    }
    val glowScale by glowTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "premium_star_glow_scale",
    )
    val glowAlpha by glowTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.48f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "premium_star_glow_alpha",
    )
    Box(
        modifier = Modifier
            .size(46.dp)
            .graphicsLayer {
                scaleX = nodeScale.value
                scaleY = nodeScale.value
                alpha = nodeAlpha.value
                translationY = nodeOffset.value
            },
        contentAlignment = Alignment.Center,
    ) {
        if (showGlow) {
            Box(
                modifier = Modifier
                    .size((46.dp * glowScale).coerceAtLeast(46.dp))
                    .clip(CircleShape)
                    .background(Color(0xFFFDC830).copy(alpha = glowAlpha)),
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (showGlow) premiumTimelineAccentBrush() else Brush.horizontalGradient(
                        listOf(Color(0xFF747DFF), Color(0xFF646EF6)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(if (showGlow) 20.dp else 18.dp),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun PremiumTimelineSection(
    items: List<PremiumTimelineItemUiModel>,
    modifier: Modifier = Modifier,
    headline: String? = null,
    accentColor: Color = Color(0xFF8A77FF),
    containerColor: Color = Color(0xFFFFFEFA),
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFFF0E4CD),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            headline?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 19.sp,
                        lineHeight = 24.sp,
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PremiumTimelineBadge(
                            label = item.label,
                            accentColor = accentColor,
                        )
                        if (index != items.lastIndex) {
                            val lineProgress = animateFloatAsState(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 900),
                                label = "premium_timeline_line_$index",
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .widthIn(min = 2.dp)
                                    .height((54.dp * lineProgress.value).coerceAtLeast(0.dp))
                                    .background(premiumTimelineLineBrush()),
                            )
                        }
                    }
                    PremiumTimelineMilestoneCard(
                        item = item,
                        modifier = Modifier.weight(1f),
                        accentColor = accentColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumTimelineBadge(
    label: String,
    accentColor: Color,
) {
    Box(
        modifier = Modifier
            .widthIn(min = 88.dp, max = 88.dp)
            .clip(CircleShape)
            .background(premiumTimelineAccentBrush())
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.45f),
                shape = CircleShape,
            ),
    ) {
        Text(
            text = label,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PremiumTimelineMilestoneCard(
    item: PremiumTimelineItemUiModel,
    modifier: Modifier = Modifier,
    accentColor: Color,
) {
    val progress = animateFloatAsState(
        targetValue = premiumTimelineProgress(item.label),
        animationSpec = tween(durationMillis = 850),
        label = "premium_timeline_${item.label}",
    )
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFFF0E1C1),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = Color(0xFFFFF3DB),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = premiumTimelineIcon(item.title),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(7.dp)
                            .size(14.dp),
                        tint = Color(0xFFD79A2B),
                    )
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 16.sp,
                        lineHeight = 19.sp,
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = Color(0xFFD79A2B),
                trackColor = Color(0xFFF3E6C6),
            )
        }
    }
}

private fun premiumTimelineIcon(title: String): ImageVector = when {
    title.contains("start", ignoreCase = true) -> Icons.Rounded.LockOpen
    title.contains("reminder", ignoreCase = true) -> Icons.Rounded.NotificationsActive
    else -> Icons.Rounded.CreditCard
}

private fun premiumTimelineProgress(label: String): Float = when {
    label.contains("today", ignoreCase = true) -> 0.34f
    label.contains("25", ignoreCase = true) -> 0.68f
    else -> 1f
}

private data class UnlockPalette(
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val border: Color,
    val accent: Color,
    val iconBackground: Color,
)

private fun unlockPalette(index: Int): UnlockPalette = when (index % 3) {
    0 -> UnlockPalette(
        backgroundStart = Color(0xFFFFFCF5),
        backgroundEnd = Color(0xFFFFF1DE),
        border = Color(0xFFF0D1A7),
        accent = Color(0xFFF08A36),
        iconBackground = Color(0xFFFFF2DF),
    )

    1 -> UnlockPalette(
        backgroundStart = Color(0xFFF7F8FF),
        backgroundEnd = Color(0xFFEEF1FF),
        border = Color(0xFFD8E0FF),
        accent = Color(0xFF6479FF),
        iconBackground = Color(0xFFEEF2FF),
    )

    else -> UnlockPalette(
        backgroundStart = Color(0xFFF4FFFE),
        backgroundEnd = Color(0xFFE8FBFA),
        border = Color(0xFFCBEFEA),
        accent = Color(0xFF1AA7A2),
        iconBackground = Color(0xFFE8FBF8),
    )
}

@Composable
private fun PremiumPlanCard(
    title: String,
    priceLabel: String,
    billingLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = if (selected) Color(0xFFFFFCF7) else Color.White,
        shape = MaterialTheme.shapes.large,
        onClick = onClick,
        tonalElevation = 0.dp,
        shadowElevation = if (selected) 10.dp else 6.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) Color(0xFFE4BE74) else Color(0xFFE8E1D6),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 144.dp)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                premiumTimelineAccentBrush(),
                            ),
                    ) {
                        Text(
                            text = "80% OFF",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                            ),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                }
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color(0xFFF28A37) else Color.Transparent)
                        .border(
                            width = 1.5.dp,
                            color = if (selected) Color(0xFFF28A37) else Color(0xFFD8D2C8),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White,
                        )
                    }
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 19.sp,
                    lineHeight = 22.sp,
                ),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = priceLabel,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                ),
                color = Color(0xFFF28A37),
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = billingLabel,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OneTimeOfferSection(
    callouts: List<PremiumCalloutUiModel>,
    priceLabel: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = Color(0xFFFFF7EC),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = priceLabel,
                    style = MaterialTheme.typography.displaySmall,
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Simple annual option, clear next step, and the same premium system across the full household.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        BalancedCalloutGrid(callouts = callouts)
    }
}

@Composable
private fun premiumAccent(stage: PremiumStageUiModel): Color = if (stage.id == PremiumStageId.OfferSheet) {
    MaterialTheme.colorScheme.primary
} else {
    MaterialTheme.colorScheme.tertiary
}

@Composable
private fun premiumBrush(stage: PremiumStageUiModel): Brush = if (stage.id == PremiumStageId.OfferSheet) {
    Brush.linearGradient(
        listOf(
            Color(0xFFF37335),
            Color(0xFFFDC830),
        ),
    )
} else {
    Brush.linearGradient(
        listOf(
            Color(0xFF7D8CFF),
            Color(0xFFA8B4FF),
        ),
    )
}

@Composable
private fun stageVisualBrush(stage: PremiumStageUiModel): Brush = when {
    stage.id == PremiumStageId.OfferSheet -> Brush.verticalGradient(
        listOf(
            Color(0xFFF7F9FF),
            Color(0xFFEFF4FF),
        ),
    )

    stage.id == PremiumStageId.Promise -> Brush.verticalGradient(
        listOf(
            Color(0xFFFFFBF4),
            Color(0xFFFFF1DE),
            Color(0xFFF5F5FF),
        ),
    )

    stage.warmVisual -> Brush.verticalGradient(
        listOf(Color(0xFFFFFAF2), Color(0xFFFFF1DE)),
    )

    else -> Brush.verticalGradient(
        listOf(Color(0xFFFFFBF6), Color(0xFFF3F7FF)),
    )
}

private fun heroHeight(stageId: PremiumStageId): Dp = when (stageId) {
    PremiumStageId.Promise -> 240.dp
    PremiumStageId.TrialClarity -> 276.dp
    PremiumStageId.Unlocks -> 268.dp
    PremiumStageId.OfferSheet -> 156.dp
    PremiumStageId.GiftReveal -> 198.dp
    PremiumStageId.OneTimeOffer -> 164.dp
}

private fun stageSpacing(stageId: PremiumStageId): Dp = when (stageId) {
    PremiumStageId.Promise -> 8.dp
    PremiumStageId.OfferSheet -> 6.dp
    PremiumStageId.Unlocks -> 6.dp
    else -> 12.dp
}

private fun premiumPrimaryLabel(
    state: PremiumUiState,
    stage: PremiumStageUiModel,
): String = when {
    state.isPurchaseInProgress && stage.id == PremiumStageId.OfferSheet -> "Starting membership..."
    state.isPurchaseInProgress && stage.id == PremiumStageId.OneTimeOffer -> "Starting yearly membership..."
    stage.id == PremiumStageId.OfferSheet -> "Try for $0.00"
    else -> stage.primaryActionLabel
}

private fun premiumTimelineAccentBrush(): Brush = Brush.horizontalGradient(
    listOf(
        Color(0xFFF37335),
        Color(0xFFFDC830),
    ),
)

private fun premiumTimelineLineBrush(): Brush = Brush.verticalGradient(
    listOf(
        Color(0xFFF37335).copy(alpha = 0.35f),
        Color(0xFFFDC830).copy(alpha = 0.55f),
    ),
)

private fun premiumStageIcon(id: PremiumStageId): ImageVector = when (id) {
    PremiumStageId.Promise -> Icons.Rounded.AutoAwesome
    PremiumStageId.TrialClarity -> Icons.Rounded.EventAvailable
    PremiumStageId.Unlocks -> Icons.Rounded.Insights
    PremiumStageId.OfferSheet -> Icons.Rounded.CreditCard
    PremiumStageId.GiftReveal -> Icons.Rounded.CardGiftcard
    PremiumStageId.OneTimeOffer -> Icons.Rounded.Stars
}

private fun premiumCalloutIcon(title: String): ImageVector = when {
    title.contains("fair", ignoreCase = true) -> Icons.Rounded.Shield
    title.contains("week", ignoreCase = true) -> Icons.Rounded.EventAvailable
    title.contains("report", ignoreCase = true) -> Icons.Rounded.Insights
    title.contains("assignment", ignoreCase = true) -> Icons.Rounded.AutoAwesome
    title.contains("approval", ignoreCase = true) -> Icons.Rounded.Shield
    title.contains("reminder", ignoreCase = true) -> Icons.Rounded.Bolt
    title.contains("annual", ignoreCase = true) -> Icons.Rounded.CreditCard
    title.contains("cancel", ignoreCase = true) -> Icons.Rounded.Shield
    else -> Icons.Rounded.Stars
}
