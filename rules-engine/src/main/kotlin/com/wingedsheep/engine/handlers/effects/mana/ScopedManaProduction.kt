package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.tagManaObligationProduction
import com.wingedsheep.sdk.core.Color

/** Runs once, after all production parts, before the separate triggered mana bonuses. */
fun finishScopedManaProduction(
    state: GameState,
    frame: ScopedManaProductionContinuation,
    events: List<GameEvent>,
    pipeline: ManaAbilityResolutionPipeline,
): ExecutionResult {
    val before = state.updateEntity(frame.playerId) { it.with(frame.poolBefore) }
    val dampening = if (frame.costsTap) pipeline.applyLandManaDampening(before, state, frame.sourceCard, frame.playerId,
        sourceIsLand = com.wingedsheep.sdk.core.CardType.LAND in frame.sourceTag.cardTypes)
        else ManaAbilityResolutionPipeline.Dampening(state, false)
    val pool = dampening.state.getEntity(frame.playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
    fun amount(color: Color): Int = pool.getAmount(color) - frame.poolBefore.getAmount(color) +
        pool.restrictedMana.count { it.color == color } - frame.poolBefore.restrictedMana.count { it.color == color }
    val production = ManaAddedEvent(
        playerId = frame.playerId, sourceId = frame.sourceId, sourceName = frame.sourceName,
        white = amount(Color.WHITE), blue = amount(Color.BLUE), black = amount(Color.BLACK),
        red = amount(Color.RED), green = amount(Color.GREEN),
        colorless = pool.colorless - frame.poolBefore.colorless + pool.restrictedMana.count { it.color == null } -
            frame.poolBefore.restrictedMana.count { it.color == null },
    )
    // Coalesce the executors' part reports into the whole activation's production.
    val carried = events.filterNot { it is ManaAddedEvent && it.sourceId == frame.sourceId && it.playerId == frame.playerId }
    val visible = carried + production
    val tracked = tagManaObligationProduction(before, dampening.state, frame.playerId, frame.sourceId, frame.sourceTag)
    val produced = production.white + production.blue + production.black + production.red + production.green + production.colorless
    return if (frame.costsTap && produced > 0)
        pipeline.finishTapBonuses(tracked, frame.sourceId, frame.sourceCard, frame.playerId, production, visible)
    else ExecutionResult.success(tracked, visible)
}
