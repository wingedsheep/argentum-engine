package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.CipherEncodeChoiceComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.EncodeOnCreatureEffect
import kotlin.reflect.KClass

/**
 * Executor for [EncodeOnCreatureEffect] — cipher's "you may exile this card encoded on a creature
 * you control" (CR 702.99a), the last step of a cipher spell's resolution.
 *
 * Records the chosen creature on the resolving spell ([CipherEncodeChoiceComponent]); the spell
 * finishes resolving immediately afterwards, and `NonPermanentSpellResolver` sends the card from
 * the stack straight to exile and encodes it there. Doing the move at that seam keeps it one zone
 * change through the spell's normal destination path (redirect replacements, the zone-change event,
 * the resolved event) rather than a second, earlier move of a spell that is still resolving.
 *
 * Nothing is recorded when the controller declined, when the resolving spell is a copy (CR 702.99a:
 * "If this spell is represented by a card"), or when the chosen object is no longer a creature on
 * the battlefield under the spell's controller.
 */
class EncodeOnCreatureExecutor : EffectExecutor<EncodeOnCreatureEffect> {

    override val effectType: KClass<EncodeOnCreatureEffect> = EncodeOnCreatureEffect::class

    override fun execute(
        state: GameState,
        effect: EncodeOnCreatureEffect,
        context: EffectContext
    ): EffectResult {
        val spellId = context.sourceId ?: return EffectResult.success(state)
        val creatureId = context.pipeline.storedCollections[effect.from]?.firstOrNull()
            ?: return EffectResult.success(state)
        val spell = state.getEntity(spellId) ?: return EffectResult.success(state)
        if (spell.has<CopyOfComponent>() || state.logicalZone(spellId)?.zoneType != Zone.STACK) {
            return EffectResult.success(state)
        }
        val projected = state.projectedState
        if (creatureId !in state.getBattlefield() || !projected.isCreature(creatureId) ||
            projected.getController(creatureId) != context.controllerId
        ) {
            return EffectResult.success(state)
        }
        return EffectResult.success(
            state.updateEntity(spellId) { it.with(CipherEncodeChoiceComponent(creatureId)) }
        )
    }
}
