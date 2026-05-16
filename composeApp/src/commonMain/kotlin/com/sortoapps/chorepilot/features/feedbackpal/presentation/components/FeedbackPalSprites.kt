package com.sortoapps.chorepilot.features.feedbackpal.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chorepilot.composeapp.generated.resources.Res
import chorepilot.composeapp.generated.resources.feedback_pal_cat_front
import chorepilot.composeapp.generated.resources.feedback_pal_cat_jump
import chorepilot.composeapp.generated.resources.feedback_pal_cat_left
import chorepilot.composeapp.generated.resources.feedback_pal_cat_right
import chorepilot.composeapp.generated.resources.feedback_pal_penguin_front
import chorepilot.composeapp.generated.resources.feedback_pal_penguin_jump
import chorepilot.composeapp.generated.resources.feedback_pal_penguin_left
import chorepilot.composeapp.generated.resources.feedback_pal_penguin_right
import chorepilot.composeapp.generated.resources.feedback_pal_squirrel_front
import chorepilot.composeapp.generated.resources.feedback_pal_squirrel_jump
import chorepilot.composeapp.generated.resources.feedback_pal_squirrel_left
import chorepilot.composeapp.generated.resources.feedback_pal_squirrel_right
import com.sortoapps.chorepilot.core.preview.ChorePilotPreviewTheme
import com.sortoapps.chorepilot.features.feedbackpal.domain.FeedbackPalKind
import com.sortoapps.chorepilot.features.feedbackpal.domain.FeedbackPalPose
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun FeedbackPalSprite(
    kind: FeedbackPalKind,
    pose: FeedbackPalPose,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    animated: Boolean = true,
    shadowElevation: Dp = 10.dp,
) {
    val transition = rememberInfiniteTransition(label = "feedback-pal-bitmap-float")
    val bob by transition.animateFloat(
        initialValue = -2f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 920, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "feedback-pal-bitmap-bob",
    )
    val lift = when (pose) {
        FeedbackPalPose.Left,
        FeedbackPalPose.Right,
        FeedbackPalPose.Front,
        -> 0f
        FeedbackPalPose.Jump -> -6f
    }

    Image(
        painter = painterResource(kind.resourceFor(pose)),
        contentDescription = "${kind.displayName()} ${pose.name.lowercase()} sprite",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .then(
                if (shadowElevation > 0.dp) {
                    Modifier.shadow(elevation = shadowElevation)
                } else {
                    Modifier
                },
            )
            .graphicsLayer {
                translationY = if (animated) bob + lift else lift
            },
    )
}

private fun FeedbackPalKind.resourceFor(pose: FeedbackPalPose): DrawableResource = when (this) {
    FeedbackPalKind.Squirrel -> when (pose) {
        FeedbackPalPose.Left -> Res.drawable.feedback_pal_squirrel_left
        FeedbackPalPose.Right -> Res.drawable.feedback_pal_squirrel_right
        FeedbackPalPose.Front -> Res.drawable.feedback_pal_squirrel_front
        FeedbackPalPose.Jump -> Res.drawable.feedback_pal_squirrel_jump
    }
    FeedbackPalKind.Penguin -> when (pose) {
        FeedbackPalPose.Left -> Res.drawable.feedback_pal_penguin_left
        FeedbackPalPose.Right -> Res.drawable.feedback_pal_penguin_right
        FeedbackPalPose.Front -> Res.drawable.feedback_pal_penguin_front
        FeedbackPalPose.Jump -> Res.drawable.feedback_pal_penguin_jump
    }
    FeedbackPalKind.Cat -> when (pose) {
        FeedbackPalPose.Left -> Res.drawable.feedback_pal_cat_left
        FeedbackPalPose.Right -> Res.drawable.feedback_pal_cat_right
        FeedbackPalPose.Front -> Res.drawable.feedback_pal_cat_front
        FeedbackPalPose.Jump -> Res.drawable.feedback_pal_cat_jump
    }
}

@Preview(name = "Feedback Pal Bitmap Sprites", widthDp = 360, heightDp = 120)
@Composable
private fun FeedbackPalSpritePreview() {
    ChorePilotPreviewTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FeedbackPalSprite(FeedbackPalKind.Squirrel, FeedbackPalPose.Left, animated = false)
            FeedbackPalSprite(FeedbackPalKind.Squirrel, FeedbackPalPose.Right, animated = false)
            FeedbackPalSprite(FeedbackPalKind.Squirrel, FeedbackPalPose.Front, animated = false)
            FeedbackPalSprite(FeedbackPalKind.Squirrel, FeedbackPalPose.Jump, animated = false)
        }
    }
}
