package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.TurnHijackedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.PlayerTurnHijackedComponent
import com.wingedsheep.sdk.scripting.effects.HijackNextTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for HijackNextTurnEffect.
 *
 * Attaches a [PlayerTurnHijackedComponent] to the targeted player in [SCHEDULED]
 * state, carrying the effect's [HijackNextTurnEffect.scope]. [TurnManager] transitions
 * it to [ACTIVE] when that player's next real turn (NextTurn scope) or next combat phase
 * (NextCombatPhase scope) begins — skipped turns/phases wait per the Scryfall ruling —
 * and removes it when that window ends.
 *
 * Multiple hijacks affecting the same player overwrite each other (latest wins);
 * we replace any existing component unconditionally.
 *
 * The accompanying [TurnHijackedEvent] is emitted twice in the lifecycle:
 *  - here, on resolution, so the log shows the hijack was scheduled;
 *  - again from [TurnManager.startTurn] when control actually engages.
 */
class HijackNextTurnExecutor : EffectExecutor<HijackNextTurnEffect> {

    override val effectType: KClass<HijackNextTurnEffect> = HijackNextTurnEffect::class

    override fun execute(
        state: GameState,
        effect: HijackNextTurnEffect,
        context: EffectContext
    ): EffectResult {
        val hijackedPlayerId = context.resolvePlayerTarget(effect.target)
            ?: return EffectResult.error(state, "No target player for HijackNextTurnEffect")

        val sourceId = context.sourceId ?: hijackedPlayerId
        val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Hijack"

        val newState = state.updateEntity(hijackedPlayerId) { container ->
            container.with(
                PlayerTurnHijackedComponent(
                    controllerId = context.controllerId,
                    state = PlayerTurnHijackedComponent.HijackState.SCHEDULED,
                    scope = effect.scope
                )
            )
        }

        return EffectResult.success(
            newState,
            listOf(
                TurnHijackedEvent(
                    controllerId = context.controllerId,
                    hijackedPlayerId = hijackedPlayerId,
                    sourceId = sourceId,
                    sourceName = sourceName
                )
            )
        )
    }
}
