package com.sortoapps.chorepilot.features.rewards.data

import com.sortoapps.chorepilot.core.firebase.FirebaseHouseholdStore
import com.sortoapps.chorepilot.features.auth.domain.SessionRepository
import com.sortoapps.chorepilot.features.chores.domain.ChoreRepository
import com.sortoapps.chorepilot.features.rewards.domain.RewardsRepository
import kotlinx.coroutines.CoroutineScope

class FirebaseRewardsRepository(
    householdStore: FirebaseHouseholdStore,
    sessionRepository: SessionRepository,
    choreRepository: ChoreRepository,
    appScope: CoroutineScope,
) : RewardsRepository by StorageBackedRewardsRepository(
    persistence = FirebaseRewardsStorePersistence(householdStore),
    sessionRepository = sessionRepository,
    choreRepository = choreRepository,
    appScope = appScope,
)
