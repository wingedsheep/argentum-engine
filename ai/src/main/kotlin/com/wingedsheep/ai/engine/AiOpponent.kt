package com.wingedsheep.ai.engine

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * The single opponent of [playerId], for the built-in AI's 1v1 model.
 *
 * The AI is deliberately two-player only: board evaluation is a me-minus-opponent
 * differential, search models exactly one responding opponent, and the advisors
 * reason about "the" opponent. Multiplayer pods launch without AI seats; making the
 * AI pod-competent is its own project (see `backlog/multiplayer.md`, "AI pod
 * players"). Engine code must never use this — it resolves opponents via targets,
 * iteration, or the per-creature defending player.
 */
internal fun GameState.soleOpponent(playerId: EntityId): EntityId? =
    getOpponents(playerId).firstOrNull()
