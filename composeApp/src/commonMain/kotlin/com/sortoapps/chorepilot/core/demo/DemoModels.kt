package com.sortoapps.chorepilot.core.demo

enum class DemoTone {
    Primary,
    Secondary,
    Tertiary,
    Success,
    Warning,
    Neutral,
}

enum class DemoChoreStatus {
    DueSoon,
    PendingProof,
    Completed,
    Overdue,
    Scheduled,
}

data class DemoDay(
    val id: String,
    val label: String,
    val shortLabel: String,
    val completionLabel: String,
    val isToday: Boolean,
)

data class DemoMember(
    val id: String,
    val name: String,
    val role: String,
    val statusLabel: String,
    val activeChoresLabel: String,
    val avatarSeed: String,
    val tone: DemoTone,
)

data class DemoChore(
    val id: String,
    val title: String,
    val area: String,
    val cadence: String,
    val assignee: String,
    val assigneeId: String,
    val status: DemoChoreStatus,
    val durationLabel: String,
    val dueLabel: String,
    val description: String,
    val reminder: String,
    val tags: List<String>,
)

data class DemoMetric(
    val label: String,
    val value: String,
    val tone: DemoTone = DemoTone.Primary,
)

data class DemoChartPoint(
    val label: String,
    val value: Float,
)

data class DemoNotification(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val tone: DemoTone,
)

data class DemoInvoice(
    val id: String,
    val title: String,
    val dateLabel: String,
    val amountLabel: String,
)

data class DemoBenefit(
    val title: String,
    val description: String,
)

data class DemoPreviewCard(
    val id: String,
    val title: String,
    val description: String,
    val routeLabel: String,
)
