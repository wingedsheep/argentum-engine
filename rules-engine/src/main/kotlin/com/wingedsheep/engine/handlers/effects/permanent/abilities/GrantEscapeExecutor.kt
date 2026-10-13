package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.GrantedKeywordAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.GrantEscapeEffect
import kotlin.reflect.KClass

/** Records an individual escape ability for graveyard casting; normal cast and zone changes end it. */
class GrantEscapeExecutor(private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry) : EffectExecutor<GrantEscapeEffect> {

    override val effectType: KClass<GrantEscapeEffect> = GrantEscapeEffect::class

    override fun execute(
        state: GameState,
        effect: GrantEscapeEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for escape grant")

        val cardComponent = state.getEntity(targetId)?.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")

        val inAGraveyard = state.zones.any { (key, ids) ->
            key.zoneType == Zone.GRAVEYARD && targetId in ids
        }
        if (!inAGraveyard) {
            return EffectResult.error(state, "Target is not in a graveyard")
        }

        // "The escape cost is equal to its mana cost" — a card with no mana cost has an unpayable
        // escape cost (the Underworld Breach ruling), so it gains nothing castable.
        val escapeCost = effect.cost ?: cardComponent.manaCost.takeUnless { cardRegistry.getCard(cardComponent.cardDefinitionId)?.hasNoManaCost == true }
            ?: return EffectResult.success(state)

        val grant = GrantedKeywordAbility(
            entityId = targetId,
            ability = KeywordAbility.Escape(escapeCost, effect.additionalCost),
            duration = effect.duration
        )

        return EffectResult.success(
            state.copy(grantedKeywordAbilities = state.grantedKeywordAbilities + grant),
            listOf(com.wingedsheep.engine.core.KeywordGrantedEvent(
                targetId, cardComponent.name, grant.ability.description, context.sourceId
                    ?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Effect"
            ))
        )
    }
}
