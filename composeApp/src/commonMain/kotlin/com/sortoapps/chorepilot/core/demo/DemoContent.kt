package com.sortoapps.chorepilot.core.demo

object DemoContent {
    val householdName = "River House"

    val days = listOf(
        DemoDay("sun", "Sunday", "Sun", "3 of 5 complete", false),
        DemoDay("mon", "Monday", "Mon", "5 of 6 complete", false),
        DemoDay("tue", "Tuesday", "Tue", "Today", true),
        DemoDay("wed", "Wednesday", "Wed", "4 chores planned", false),
        DemoDay("thu", "Thursday", "Thu", "Lighter day", false),
        DemoDay("fri", "Friday", "Fri", "Reset night", false),
        DemoDay("sat", "Saturday", "Sat", "Weekend catch-up", false),
    )

    val members = listOf(
        DemoMember(
            id = "erick",
            name = "Erick",
            role = "Admin",
            statusLabel = "Owner",
            activeChoresLabel = "2 active chores, 1 approval pending",
            avatarSeed = "ER",
            tone = DemoTone.Primary,
        ),
        DemoMember(
            id = "emma",
            name = "Emma",
            role = "Member",
            statusLabel = "On track",
            activeChoresLabel = "3 chores today, streak 5",
            avatarSeed = "EM",
            tone = DemoTone.Secondary,
        ),
        DemoMember(
            id = "noah",
            name = "Noah",
            role = "Member",
            statusLabel = "Available",
            activeChoresLabel = "2 chores today, vacation off",
            avatarSeed = "NW",
            tone = DemoTone.Tertiary,
        ),
        DemoMember(
            id = "grandma",
            name = "Rosa",
            role = "Helper",
            statusLabel = "Pending invite",
            activeChoresLabel = "Available for meal prep and rides",
            avatarSeed = "RS",
            tone = DemoTone.Neutral,
        ),
    )

    val chores = listOf(
        DemoChore(
            id = "kitchen-cleanup",
            title = "Kitchen cleanup",
            area = "Kitchen",
            cadence = "Weekdays at 6:00 PM",
            assignee = "Emma",
            assigneeId = "emma",
            status = DemoChoreStatus.PendingProof,
            durationLabel = "15 min",
            dueLabel = "Due at 6:00 PM",
            description = "Wipe the counters, clear the table, and start the dishwasher before dinner resets the room again.",
            reminder = "Dinner lands better when the table is ready, so this one keeps the evening smooth for everyone.",
            tags = listOf("Kitchen", "Proof required", "Today"),
        ),
        DemoChore(
            id = "trash-duty",
            title = "Take out trash",
            area = "Outside bins",
            cadence = "Tuesday nights",
            assignee = "Erick",
            assigneeId = "erick",
            status = DemoChoreStatus.Overdue,
            durationLabel = "8 min",
            dueLabel = "Overdue",
            description = "Pull kitchen and bathroom bags, replace liners, and wheel the cans to the curb before truck pickup.",
            reminder = "The truck comes early tomorrow, so this one affects the whole morning if it slips.",
            tags = listOf("Outside", "No proof", "Urgent"),
        ),
        DemoChore(
            id = "bathroom-refresh",
            title = "Bathroom refresh",
            area = "Upstairs bath",
            cadence = "Every Friday",
            assignee = "Noah",
            assigneeId = "noah",
            status = DemoChoreStatus.DueSoon,
            durationLabel = "12 min",
            dueLabel = "Starts in 20 min",
            description = "Replace hand towels, wipe the mirror, and restock the cabinet before bedtime routines begin.",
            reminder = "Quick wins keep Friday from turning into a full catch-up session later.",
            tags = listOf("Bathroom", "Due soon", "Routine"),
        ),
        DemoChore(
            id = "entry-reset",
            title = "Entry reset",
            area = "Front hall",
            cadence = "Every school night",
            assignee = "Emma",
            assigneeId = "emma",
            status = DemoChoreStatus.Completed,
            durationLabel = "10 min",
            dueLabel = "Completed at 5:18 PM",
            description = "Backpacks, shoes, and lunch prep get staged so the morning starts calm instead of scrambled.",
            reminder = "When the entry is ready tonight, tomorrow feels lighter before anyone even wakes up.",
            tags = listOf("Entry", "Completed", "Streak"),
        ),
    )

    val todayMetrics = listOf(
        DemoMetric("6", "Assigned", DemoTone.Primary),
        DemoMetric("5", "Completed", DemoTone.Success),
        DemoMetric("1", "Pending", DemoTone.Warning),
        DemoMetric("92%", "Fair share", DemoTone.Secondary),
    )

    val reportMetrics = listOf(
        DemoMetric("31", "Completed", DemoTone.Primary),
        DemoMetric("89%", "Weekly rate", DemoTone.Success),
        DemoMetric("Emma", "Most consistent", DemoTone.Secondary),
        DemoMetric("83%", "Proof rate", DemoTone.Tertiary),
    )

    val reportSeries = listOf(
        DemoChartPoint("Mon", 0.42f),
        DemoChartPoint("Tue", 0.55f),
        DemoChartPoint("Wed", 0.61f),
        DemoChartPoint("Thu", 0.5f),
        DemoChartPoint("Fri", 0.7f),
        DemoChartPoint("Sat", 0.47f),
        DemoChartPoint("Sun", 0.64f),
    )

    val notifications = listOf(
        DemoNotification(
            id = "due-kitchen",
            title = "Kitchen cleanup is due at 6:00 PM",
            subtitle = "Emma · proof required",
            badge = "Due",
            tone = DemoTone.Warning,
        ),
        DemoNotification(
            id = "trash-soon",
            title = "Take out trash starts in 15 minutes",
            subtitle = "You · quick action available",
            badge = "Soon",
            tone = DemoTone.Primary,
        ),
        DemoNotification(
            id = "streak",
            title = "River House kept the streak alive",
            subtitle = "7-day household streak",
            badge = "Win",
            tone = DemoTone.Secondary,
        ),
    )

    val invoices = listOf(
        DemoInvoice("apr", "Annual premium", "Renews Apr 10, 2027", "$79.99"),
        DemoInvoice("mar", "Invoice export", "Mar 10, 2026", "$0.00"),
        DemoInvoice("trial", "7-day premium trial", "Completed successfully", "$0.00"),
    )

    val premiumBenefits = listOf(
        DemoBenefit("Smarter weekly planning", "See overloaded days, calmer handoffs, and recurring collisions before they build up."),
        DemoBenefit("Vacation and day-off logic", "Shift approvals and assignments automatically when someone is away."),
        DemoBenefit("Deeper reports", "Track fairness, proof completion, and yearly rhythm with much more context."),
    )

    val previewCards = listOf(
        DemoPreviewCard(
            id = "offline",
            title = "Offline state",
            description = "Connection lost, cached chores stay visible, and recovery options remain calm.",
            routeLabel = "Offline preview",
        ),
        DemoPreviewCard(
            id = "error",
            title = "Error state",
            description = "Useful retry copy with a soft fallback back into the household view.",
            routeLabel = "Error preview",
        ),
        DemoPreviewCard(
            id = "loading",
            title = "Loading state",
            description = "Skeleton-friendly dashboard load with no clipped labels or awkward jumps.",
            routeLabel = "Loading preview",
        ),
    )
}
