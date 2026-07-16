package com.wingedsheep.engine.handlers.effects.linkedexile

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ExileUntilLeavesEffect
import kotlin.reflect.KClass

/**
 * Executor for ExileUntilLeavesEffect.
 *
 * Exiles the target and links it to the source permanent via LinkedExileComponent. The source's
 * LeavesBattlefield trigger (defined on the card) handles returning the exiled card via
 * ReturnLinkedExile (typically [com.wingedsheep.sdk.dsl.Effects.ReturnLinkedExileUnderOwnersControl],
 * which puts it back onto the battlefield under its owner's control).
 *
 * The target is normally a battlefield permanent (O-Ring style — Driftgloom Coyote, Liminal Hold),
 * but a graveyard **card** is also accepted: Savior of Ollenbock exiles "up to one other target
 * creature from the battlefield or creature card from a graveyard" and returns the exiled card to
 * the battlefield when it leaves. Because [ZoneTransitionService.moveToZone] locates the source zone
 * itself, one code path moves the target to exile regardless of which of those two zones it started
 * in. A target in any other zone (hand, library, stack) is never a legal exile-until-leaves target,
 * so it is left alone.
 *
 * If the source is no longer on the battlefield when this effect resolves
 * (modern template safety), the target is not exiled.
 *
 * Delegates zone movement to [ZoneTransitionService] for consistent cleanup
 * (now includes cleanupCombatReferences, cleanupReverseAttachmentLink, and
 * removeFloatingEffectsTargeting which were previously missing).
 */
class ExileUntilLeavesExecutor : EffectExecutor<ExileUntilLeavesEffect> {

    override val effectType: KClass<ExileUntilLeavesEffect> = ExileUntilLeavesEffect::class

    override fun execute(
        state: GameState,
        effect: ExileUntilLeavesEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId
            ?: return EffectResult.success(state)

        // Modern template: if source is no longer on the battlefield, do nothing
        if (sourceId !in state.getBattlefield()) {
            return EffectResult.success(state)
        }

        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)

        // Verify the target is somewhere this effect can exile from: a battlefield permanent
        // (O-Ring style) or a card in some player's graveyard (Savior of Ollenbock's "creature card
        // from a graveyard"). Target validation at resolution already fizzles illegal targets, so a
        // target in any other zone is a defensive no-op.
        val inBattlefield = targetId in state.getBattlefield()
        val inGraveyard = !inBattlefield && state.turnOrder.any { targetId in state.getGraveyard(it) }
        if (!inBattlefield && !inGraveyard) {
            return EffectResult.success(state)
        }

        // Delegate zone movement to ZoneTransitionService
        val transitionResult = ZoneTransitionService.moveToZone(
            state, targetId, Zone.EXILE,
            ZoneEntryOptions(skipZoneChangeRedirect = true)
        )

        var newState = transitionResult.state

        // Store exiled ID on the source permanent as LinkedExileComponent
        val sourceContainer = newState.getEntity(sourceId)
        if (sourceContainer != null) {
            val existingLinked = sourceContainer.get<LinkedExileComponent>()
            val allExiled = (existingLinked?.exiledIds ?: emptyList()) + listOf(targetId)
            newState = newState.updateEntity(sourceId) { c ->
                c.with(LinkedExileComponent(allExiled))
            }
        }

        return EffectResult(
            state = newState,
            events = transitionResult.events
        )
    }
}
