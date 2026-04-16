package com.wingedsheep.gym.service

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Everything needed to spin up a new gym environment.
 *
 * Designed so a JSON payload can drive the HTTP layer in Phase 3 without
 * any translation.
 */
@Serializable
data class EnvConfig(
    val players: List<PlayerSpec>,

    /** Opening-hand size. Standard MTG = 7. */
    val startingHandSize: Int = 7,

    /**
     * Skip mulligan phase. Default `true` — training loops rarely care about
     * mulligans and the extra decision points slow rollouts down.
     */
    val skipMulligans: Boolean = true,

    /** MTGA-style hand smoothing (only useful for realistic play-feel runs). */
    val useHandSmoother: Boolean = false,

    /**
     * Which player goes first (0-indexed). `null` = random — which should be
     * the default for training diversity. Set explicitly only for reproducible
     * scenarios.
     */
    val startingPlayerIndex: Int? = null,

    /**
     * Which player's information-set the default [com.wingedsheep.gym.contract.TrainingObservation]
     * represents. Callers can still override per-request when observing.
     */
    val perspectivePlayerIndex: Int = 0,

    /**
     * If `true`, opponent hand and libraries are revealed — debug only,
     * must never be enabled in production self-play.
     */
    val revealAll: Boolean = false
) {
    init {
        require(players.size >= 2) { "Need at least 2 players" }
        require(perspectivePlayerIndex in players.indices) {
            "perspectivePlayerIndex=$perspectivePlayerIndex out of range for ${players.size} players"
        }
    }
}

/** A single player's identity + deck. */
@Serializable
data class PlayerSpec(
    val name: String,
    val deck: DeckSpec,
    val startingLife: Int = 20,
    val playerId: EntityId? = null
)

/** A single environment's `step()` input — batched into [com.wingedsheep.gym.service.MultiEnvService.stepBatch]. */
@Serializable
data class StepRequest(
    val envId: EnvId,
    val actionId: Int
)

/** Result of deck validation. Surfaced by [DeckResolver.validate]. */
@Serializable
data class DeckValidation(
    val ok: Boolean,
    val errors: List<String> = emptyList(),
    val totalCards: Int = 0
)
