package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.zones.MoveToZoneEffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ReturnSpellOrPermanentToOwnersHandEffect
import kotlin.reflect.KClass

/**
 * Executor for [ReturnSpellOrPermanentToOwnersHandEffect].
 *
 * Resolves the single target and dispatches on what it turns out to be — the bounce
 * counterpart to [com.wingedsheep.engine.handlers.effects.library.PutOnTopOrBottomOfLibraryExecutor]
 * (Swat Away), which already handles the dual spell/permanent case for library placement:
 * - **Spell on the stack** → removed from the stack and put into its owner's hand (it does
 *   not resolve). This is not a counter (CR 701.27 / 701.5b), so "can't be countered" does
 *   not block it — mirrors [ReturnSpellToOwnersHandExecutor].
 * - **Permanent** → delegated to [MoveToZoneEffectExecutor] for a standard bounce to its
 *   owner's hand, so it shares all the leave-the-battlefield cleanup of `Effects.ReturnToHand`.
 *
 * If the target is no longer in a valid zone at resolution, the effect does nothing.
 */
class ReturnSpellOrPermanentToOwnersHandExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<ReturnSpellOrPermanentToOwnersHandEffect> {

    override val effectType: KClass<ReturnSpellOrPermanentToOwnersHandEffect> =
        ReturnSpellOrPermanentToOwnersHandEffect::class

    private val permanentBounce = MoveToZoneEffectExecutor(cardRegistry)

    override fun execute(
        state: GameState,
        effect: ReturnSpellOrPermanentToOwnersHandEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.success(state)

        // Spell on the stack: remove from the stack and put into its owner's hand.
        if (targetId in state.stack) {
            val container = state.getEntity(targetId)
                ?: return EffectResult.success(state)
            val cardComponent = container.get<CardComponent>()
            val spellComponent = container.get<SpellOnStackComponent>()
            val ownerId = cardComponent?.ownerId
                ?: spellComponent?.casterId
                ?: return EffectResult.error(state, "Cannot determine spell owner")

            var newState = state.removeFromStack(targetId)
            newState = newState.addToZone(ZoneKey(ownerId, Zone.HAND), targetId)
            newState = newState.updateEntity(targetId) { c ->
                c.without<SpellOnStackComponent>().without<TargetsComponent>()
            }

            return EffectResult.success(
                newState,
                listOf(
                    ZoneChangeEvent(
                        targetId,
                        cardComponent?.name ?: "Unknown",
                        null,
                        Zone.HAND,
                        ownerId
                    )
                )
            )
        }

        // Otherwise it is a permanent — delegate to the standard bounce executor.
        return permanentBounce.execute(state, MoveToZoneEffect(effect.target, Zone.HAND), context)
    }
}
