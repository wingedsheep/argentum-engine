package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.event.GrantedActivatedAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.embalmAbility
import com.wingedsheep.sdk.scripting.effects.GrantEmbalmEffect
import kotlin.reflect.KClass

/**
 * Executor for [GrantEmbalmEffect] — Cursecloth Wrappings: "{T}: Target creature card in your
 * graveyard gains embalm until end of turn. The embalm cost is equal to its mana cost." (CR 702.128.)
 *
 * Resolves the target card, derives the embalm cost (the effect's fixed cost if given, else the
 * card's own mana cost), and records a [GrantedActivatedAbility] keyed to the card entity holding
 * exactly the ability [embalmAbility] builds for a printed-embalm card. From then on the
 * zone-activated-ability enumerator surfaces it while the card sits in the graveyard, and
 * `ActivateAbilityHandler` accepts the activation through its existing granted-ability lookup. The
 * grant is removed at end of turn by the cleanup step.
 *
 * Unlike [GrantFlashbackExecutor] / [GrantHarmonizeExecutor] — which grant *cast* keywords and so
 * have to reach the cast pipeline through `GrantedKeywordAbility` — embalm is an ordinary activated
 * ability, so the plain activated-ability grant channel is all it needs.
 */
class GrantEmbalmExecutor : EffectExecutor<GrantEmbalmEffect> {

    override val effectType: KClass<GrantEmbalmEffect> = GrantEmbalmEffect::class

    override fun execute(
        state: GameState,
        effect: GrantEmbalmEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for embalm grant")

        val container = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target card no longer exists")
        val cardComponent = container.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")

        // Embalm is printed only on creature cards, and every effect that grants it says "target
        // creature card" — so a noncreature card here means the target requirement was bypassed.
        // The printed type line is authoritative off the battlefield, so no projection.
        if (!cardComponent.typeLine.isCreature) {
            return EffectResult.error(state, "Embalm can only be granted to a creature card")
        }
        val inAGraveyard = state.zones.any { (key, ids) ->
            key.zoneType == Zone.GRAVEYARD && targetId in ids
        }
        if (!inAGraveyard) {
            return EffectResult.error(state, "Target is not in a graveyard")
        }

        // "The embalm cost is equal to its mana cost" — unless the effect fixes one.
        val embalmCost = effect.cost ?: cardComponent.manaCost

        val grant = GrantedActivatedAbility(
            entityId = targetId,
            ability = embalmAbility(embalmCost),
            duration = effect.duration,
            sourceId = context.sourceId
        )

        return EffectResult.success(
            state.copy(grantedActivatedAbilities = state.grantedActivatedAbilities + grant)
        )
    }
}
