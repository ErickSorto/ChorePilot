package com.sortoapps.chorepilot.features.feedbackpal.domain

import kotlinx.serialization.Serializable

@Serializable
enum class FeedbackPalKind {
    Squirrel,
    Penguin,
    Cat,
}

enum class FeedbackPalPose {
    Left,
    Right,
    Front,
    Jump,
}

@Serializable
enum class FeedbackIntent {
    Bug,
    FeatureIdea,
    ConfusingFlow,
    Praise,
}
