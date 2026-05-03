package com.sortoapps.chorepilot.features.rewards.data

import com.sortoapps.chorepilot.core.platform.PlatformPreferences
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.rewards.domain.RewardsRepository
import kotlinx.coroutines.CoroutineScope

class PersistedRewardsRepository(
    platformPreferences: PlatformPreferences,
    sessionRepository: SessionRepository,
    choreRepository: ChoreRepository,
    appScope: CoroutineScope,
) : RewardsRepository by StorageBackedRewardsRepository(
    persistence = PlatformPreferencesRewardsStorePersistence(platformPreferences),
    sessionRepository = sessionRepository,
    choreRepository = choreRepository,
    appScope = appScope,
)
