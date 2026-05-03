package com.sortoapps.chorepilot.features.auth.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import chorepilot.composeapp.generated.resources.Res
import chorepilot.composeapp.generated.resources.m1_splash_hero
import chorepilot.composeapp.generated.resources.onboarding_hero_mascot
import chorepilot.composeapp.generated.resources.onboarding_hero_scene
import com.sortoapps.chorepilot.core.designsystem.ChorePilotGradientBackground
import com.sortoapps.chorepilot.core.designsystem.ChorePilotScreen
import com.sortoapps.chorepilot.core.designsystem.IllustrationCard
import com.sortoapps.chorepilot.core.designsystem.PrimaryButton
import com.sortoapps.chorepilot.core.designsystem.SecondaryButton
import com.sortoapps.chorepilot.core.designsystem.ToneChip
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewTheme
import com.sortoapps.chorepilot.core.preview.PreviewDeviceFrame
import com.sortoapps.chorepilot.features.auth.domain.HouseholdRole
import com.sortoapps.chorepilot.features.auth.domain.InvitePreview
import com.sortoapps.chorepilot.features.auth.presentation.viewmodel.AuthUiEvent
import com.sortoapps.chorepilot.features.auth.presentation.viewmodel.AuthUiState
import org.jetbrains.compose.resources.painterResource

@Composable
fun AuthLandingScreen(
    state: AuthUiState,
    onEvent: (AuthUiEvent) -> Unit,
    onCreateHousehold: () -> Unit,
    onJoinHousehold: () -> Unit,
) {
    CompactAuthScreen(title = "Choose your setup") {
        CompactAuthHeroCard(
            title = state.displayName.takeIf { it.isNotBlank() }?.let { "Welcome, $it" } ?: "Welcome",
            supportingText = "Create a household or join with a code.",
        )
        AuthFormCard {
            SetupActionRow(
                icon = Icons.Rounded.Home,
                title = "Create household",
                summary = "Start a new home base.",
                detail = "You become the first owner.",
                primary = true,
                onClick = {
                    onEvent(AuthUiEvent.TriggerVibration)
                    onCreateHousehold()
                },
            )
            SetupActionRow(
                icon = Icons.Rounded.GroupAdd,
                title = "Join with invite code",
                summary = "Use the code your family shared.",
                detail = "Best when someone already created the group.",
                primary = false,
                onClick = {
                    onEvent(AuthUiEvent.TriggerVibration)
                    onJoinHousehold()
                },
            )
        }
    }
}

@Composable
fun SignInScreen(
    state: AuthUiState,
    onEvent: (AuthUiEvent) -> Unit,
    onGoogleSignIn: () -> Unit,
) {
    val sceneMotion = rememberInfiniteTransition(label = "sign-in-scene-motion")
    val sceneScale by sceneMotion.animateFloat(
        initialValue = 1.04f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sign-in-scene-scale",
    )
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bottomOverlayColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.background.copy(alpha = 0.96f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Image(
            painter = painterResource(Res.drawable.onboarding_hero_scene),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = sceneScale
                    scaleY = sceneScale
                },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            bottomOverlayColor.copy(alpha = 0.5f),
                            bottomOverlayColor,
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "ChorePilot",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AuthErrorCard(message = state.errorMessage)
            PrimaryButton(
                text = if (state.isSubmitting) "Opening Google..." else "Continue with Google",
                enabled = !state.isSubmitting && state.googleSignInAvailable,
                onClick = {
                    onEvent(AuthUiEvent.TriggerVibration)
                    onGoogleSignIn()
                },
            )
            if (!state.googleSignInAvailable) {
                AuthInlineStatus(
                    text = "Google sign-in is unavailable on this device.",
                )
            }
        }
    }
}

@Composable
fun CreateHouseholdScreen(
    state: AuthUiState,
    onEvent: (AuthUiEvent) -> Unit,
    onCreate: () -> Unit,
    @Suppress("UNUSED_PARAMETER")
    onOpenPremium: () -> Unit,
) {
    CompactAuthScreen(title = "Create household") {
        CompactAuthHeroCard(
            title = "Create your household",
            supportingText = "Set up the group you will manage.",
        )
        AuthErrorCard(message = state.errorMessage)
        AuthFormCard {
            AuthTextField(
                label = "Your first name",
                value = state.displayName,
                onValueChange = { onEvent(AuthUiEvent.DisplayNameChanged(it)) },
                capitalization = KeyboardCapitalization.Words,
            )
            AuthTextField(
                label = "Household name",
                value = state.householdName,
                onValueChange = { onEvent(AuthUiEvent.HouseholdNameChanged(it)) },
                capitalization = KeyboardCapitalization.Words,
            )
            PrimaryButton(
                text = if (state.isSubmitting) "Creating household..." else "Create household",
                enabled = state.displayName.isNotBlank() && state.householdName.isNotBlank() && !state.isSubmitting,
                onClick = {
                    onEvent(AuthUiEvent.TriggerVibration)
                    onCreate()
                },
            )
        }
    }
}

@Composable
fun JoinHouseholdScreen(
    state: AuthUiState,
    onEvent: (AuthUiEvent) -> Unit,
    onJoin: () -> Unit,
    onDontHavePlace: () -> Unit,
) {
    CompactAuthScreen(title = "Join with invite code") {
        CompactAuthHeroCard(
            title = "Join a group",
            supportingText = "Use the code your family shared.",
        )
        AuthErrorCard(message = state.errorMessage)
        AuthFormCard {
            AuthTextField(
                label = "Your first name",
                value = state.displayName,
                onValueChange = { onEvent(AuthUiEvent.DisplayNameChanged(it)) },
                capitalization = KeyboardCapitalization.Words,
            )
            AuthTextField(
                label = "Invite code",
                value = state.inviteCode,
                onValueChange = { onEvent(AuthUiEvent.InviteCodeChanged(it)) },
                capitalization = KeyboardCapitalization.Characters,
            )
            InvitePreviewCard(
                preview = state.invitePreview,
                isLoading = state.isInvitePreviewLoading,
                inviteCode = state.inviteCode,
            )
            PrimaryButton(
                text = if (state.isSubmitting) "Joining household..." else "Join household",
                enabled = state.displayName.isNotBlank() && state.inviteCode.isNotBlank() && !state.isSubmitting,
                onClick = {
                    onEvent(AuthUiEvent.TriggerVibration)
                    onJoin()
                },
            )
            SecondaryButton(
                text = "Don't have a place?",
                onClick = {
                    onEvent(AuthUiEvent.TriggerVibration)
                    onDontHavePlace()
                },
            )
        }
    }
}

@Composable
private fun CompactAuthScreen(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    ChorePilotGradientBackground {
        ChorePilotScreen(
            title = title,
            subtitle = "",
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun CompactAuthHeroCard(
    title: String,
    supportingText: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.onboarding_hero_mascot),
                contentDescription = "ChorePilot squirrel mascot",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(172.dp),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AuthFormCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun AuthInlineStatus(
    text: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun AuthSetupScreen(
    stepLabel: String,
    title: String,
    subtitle: String,
    toneChips: List<String>,
    heroTitle: String,
    heroBody: String,
    heroBadges: List<String>,
    content: @Composable ColumnScope.() -> Unit,
) {
    ChorePilotGradientBackground {
        ChorePilotScreen(
            title = title,
            subtitle = subtitle,
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    AuthHeroCard(
                        stepLabel = stepLabel,
                        title = heroTitle,
                        body = heroBody,
                        toneChips = toneChips,
                        heroBadges = heroBadges,
                    )
                    content()
                }
            }
        }
    }
}

@Composable
private fun AuthHeroCard(
    stepLabel: String,
    title: String,
    body: String,
    toneChips: List<String>,
    heroBadges: List<String>,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.24f),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                    shape = CircleShape,
                ) {}
            }
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
                        text = stepLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                toneChips.forEach { label ->
                    ToneChip(text = label)
                }
            }
            IllustrationCard(
                drawable = Res.drawable.m1_splash_hero,
                contentDescription = "ChorePilot setup artwork",
                aspectRatio = 1.28f,
            )
            heroBadges.forEach { badge ->
                AuthBadgeRow(text = badge)
            }
        }
    }
}

@Composable
private fun AuthBadgeRow(
    text: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.TaskAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AuthActionGroup(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun SetupActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    summary: String,
    detail: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = if (primary) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (primary) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (primary) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
                },
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (primary) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                    },
                )
            }
        }
    }
}

@Composable
private fun HouseholdSizeGroup(
    selectedOption: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Starting size",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        options.forEachIndexed { index, option ->
            SetupActionRow(
                icon = Icons.Rounded.Person,
                title = option,
                summary = when (index) {
                    0 -> "Good for one parent and two kids."
                    1 -> "Room for a fuller family rotation."
                    else -> "Built for bigger households without rethinking the structure later."
                },
                detail = if (selectedOption == option) "Currently selected" else "Tap to use this as your starting size",
                primary = selectedOption == option,
                onClick = { onSelect(option) },
            )
        }
    }
}

@Composable
private fun InvitePreviewCard(
    preview: InvitePreview?,
    isLoading: Boolean,
    inviteCode: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Invite preview",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = when {
                            isLoading -> "Checking code"
                            preview != null -> "Ready to join"
                            inviteCode.length >= 4 -> "Code not found"
                            else -> "Enter a code"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
            when {
                isLoading -> Text(
                    text = "Checking invite...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                preview != null -> {
                    Text(
                        text = preview.householdName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ToneChip(text = preview.role.name.lowercase().replaceFirstChar(Char::titlecase))
                        ToneChip(text = preview.memberCountLabel)
                        ToneChip(text = "Ready to join")
                    }
                }

                inviteCode.length >= 4 -> Text(
                    text = "That code is not available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> Text(
                    text = "Preview appears when the code looks complete.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AuthMessageCard(
    title: String,
    body: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Stars,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AuthErrorCard(
    message: String?,
) {
    if (message.isNullOrBlank()) return
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.68f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.18f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Something needs attention",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
        ),
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun SignInScreenPreview() {
    AuthPreviewFrame {
        SignInScreen(
            state = AuthUiState(),
            onEvent = {},
            onGoogleSignIn = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SignInLoadingPreview() {
    AuthPreviewFrame {
        SignInScreen(
            state = AuthUiState(isSubmitting = true),
            onEvent = {},
            onGoogleSignIn = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SignInUnavailableDarkPreview() {
    AuthPreviewFrame(dark = true) {
        SignInScreen(
            state = AuthUiState(
                googleSignInAvailable = false,
                errorMessage = "Firebase is missing on this build. Add the required config file and try again.",
            ),
            onEvent = {},
            onGoogleSignIn = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 900)
@Composable
private fun CreateHouseholdWidePreview() {
    AuthPreviewFrame {
        CreateHouseholdScreen(
            state = AuthUiState(
                displayName = "Alexandria",
                householdName = "The Very Long Summer Street Household",
                selectedHouseholdSize = "6 or more members",
            ),
            onEvent = {},
            onCreate = {},
            onOpenPremium = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateHouseholdSubmittingPreview() {
    AuthPreviewFrame {
        CreateHouseholdScreen(
            state = AuthUiState(
                displayName = "Erick",
                householdName = "Sorto Home",
                isSubmitting = true,
                errorMessage = "The household could not be created right now. Please try again.",
            ),
            onEvent = {},
            onCreate = {},
            onOpenPremium = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinHouseholdPreview() {
    AuthPreviewFrame {
        JoinHouseholdScreen(
            state = AuthUiState(
                displayName = "Erick",
                inviteCode = "ABCD12",
                invitePreview = InvitePreview(
                    householdId = "household-1",
                    householdName = "Sorto Family",
                    role = HouseholdRole.Member,
                    memberCountLabel = "4 active members",
                ),
            ),
            onEvent = {},
            onJoin = {},
            onDontHavePlace = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinHouseholdLoadingPreview() {
    AuthPreviewFrame(dark = true) {
        JoinHouseholdScreen(
            state = AuthUiState(
                displayName = "Erick",
                inviteCode = "QWERTY",
                isInvitePreviewLoading = true,
            ),
            onEvent = {},
            onJoin = {},
            onDontHavePlace = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthLandingPreview() {
    AuthPreviewFrame {
        AuthLandingScreen(
            state = AuthUiState(displayName = "Erick"),
            onEvent = {},
            onCreateHousehold = {},
            onJoinHousehold = {},
        )
    }
}

@Composable
private fun AuthPreviewFrame(
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    ChorePilotPreviewTheme {
        PreviewDeviceFrame { _ ->
            content()
        }
    }
}
