package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.effects.PutOntoBattlefieldAsNonAuraEffect
import kotlin.reflect.KClass

/**
 * Executor for [PutOntoBattlefieldAsNonAuraEffect] — "return this card to the battlefield under
 * your control as a non-Aura enchantment. It loses \"enchant creature\" and gains \"…\""
 * (Takklemaggot).
 *
 * The card enters under the effect controller's control, attached to nothing and already the
 * non-Aura [NonAuraEntry] describes. The effect's chosen player is resolved now and recorded as
 * the permanent's [ChoiceSlot.PLAYER] choice as it enters, where `Player.ChosenPlayer` reads it.
 */
class PutOntoBattlefieldAsNonAuraExecutor(
    private val zones: ZoneTransitionService,
    private val cardRegistry: CardRegistry,
) : EffectExecutor<PutOntoBattlefieldAsNonAuraEffect> {

    override val effectType: KClass<PutOntoBattlefieldAsNonAuraEffect> = PutOntoBattlefieldAsNonAuraEffect::class

    override fun execute(
        state: GameState,
        effect: PutOntoBattlefieldAsNonAuraEffect,
        context: EffectContext
    ): EffectResult {
        val cardId = context.resolveTarget(effect.target, state) ?: return EffectResult.success(state)
        if (state.getEntity(cardId)?.get<CardComponent>() == null) return EffectResult.success(state)
        val zone = state.zones.keys.firstOrNull { cardId in state.getZone(it) }
            ?: return EffectResult.success(state)
        if (zone.zoneType == Zone.BATTLEFIELD || zone.zoneType == Zone.STACK) return EffectResult.success(state)
        if (effect.fromZone != null && zone.zoneType != effect.fromZone) return EffectResult.success(state)
        if (EntryLocks.cantEnter(state, cardId, zone.zoneType, cardRegistry, zones.predicateEvaluator)) {
            return EffectResult.success(state)
        }

        val chosenPlayer = effect.chosenPlayer?.let { TargetResolutionUtils.resolvePlayerTarget(it, context, state) }
        val moved = zones.moveToZone(
            state, cardId, Zone.BATTLEFIELD,
            ZoneEntryOptions(
                controllerId = context.controllerId,
                asNonAura = NonAuraEntry(effect.grantedTriggeredAbilities),
                entryChoices = chosenPlayer?.let { mapOf(ChoiceSlot.PLAYER to ChoiceValue.EntityChoice(it)) }
                    ?: emptyMap(),
            ),
            zone
        )
        return EffectResult.success(moved.state, moved.events)
    }
}
