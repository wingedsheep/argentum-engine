package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.liveStaticAbilities
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantOffspringToOwnSpells
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Offspring granted to a spell as it is cast (CR 702.175, [GrantOffspringToOwnSpells] — Zinnia,
 * Valley's Voice).
 *
 * The grant joins the spell's printed optional additional costs as one more
 * [KeywordAbility.OptionalAdditionalCost], declared under [ChoiceSlot.OFFSPRING] so it is paid
 * separately from any printed offspring or kicker (CR 702.175b). Every stage that offers or charges
 * an optional cost — the cast enumerators, the validator, the cost totaller — reads
 * [effectiveOptionalCosts], so the price offered is the price charged.
 */
object OffspringGrants {

    /**
     * The offspring grants live for spells [casterId] casts: every [GrantOffspringToOwnSpells] on a
     * battlefield permanent the caster controls, a `staticAbility { condition = … }` one only while
     * its condition holds. A granter that has lost all its abilities, or is face down, grants
     * nothing. Computed once per enumeration pass and matched per spell by [effectiveOptionalCosts].
     */
    fun grants(
        state: GameState,
        casterId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
    ): List<GrantOffspringToOwnSpells> {
        val projected = state.projectedState
        return buildList {
            for (granterId in state.controlledBattlefield(casterId)) {
                val granter = state.getEntity(granterId) ?: continue
                if (granter.has<FaceDownComponent>() || projected.hasLostAllAbilities(granterId)) continue
                val def = granter.get<CardComponent>()?.let { cardRegistry.getCard(it.cardDefinitionId) } ?: continue
                for (ability in def.liveStaticAbilities(state, granterId)) {
                    when (ability) {
                        is GrantOffspringToOwnSpells -> add(ability)
                        is ConditionalStaticAbility -> {
                            val inner = ability.ability as? GrantOffspringToOwnSpells ?: continue
                            val context = EffectContext(sourceId = granterId, controllerId = casterId)
                            if (predicateEvaluator.conditions.evaluate(state, ability.condition, context)) add(inner)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    /**
     * Every optional additional cost [casterId] may declare while casting [cardId]: the card's
     * printed ones, in printed order, then one granted offspring at the first matching grant's cost
     * (a grant applies when its `spellFilter` matches the spell). [grants] defaults to a fresh scan;
     * an enumerator looping over many cards passes the one it computed up front.
     */
    fun effectiveOptionalCosts(
        state: GameState,
        cardId: EntityId,
        cardDef: CardDefinition?,
        casterId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
        grants: List<GrantOffspringToOwnSpells> = grants(state, casterId, cardRegistry, predicateEvaluator),
    ): List<KeywordAbility.OptionalAdditionalCost> {
        val printed = cardDef?.keywordAbilities?.filterIsInstance<KeywordAbility.OptionalAdditionalCost>().orEmpty()
        // Offspring only does anything on a permanent spell; granting it to an instant would let
        // the declared slot pick kicker-only effects, which key off any declared slot.
        if (grants.isEmpty() || cardDef?.typeLine?.isPermanent != true) return printed
        val context = PredicateContext(controllerId = casterId)
        val grant = grants.firstOrNull {
            predicateEvaluator.matches(state, state.projectedState, cardId, it.spellFilter, context)
        } ?: return printed
        return printed + grantedKeyword(grant)
    }

    private fun grantedKeyword(grant: GrantOffspringToOwnSpells): KeywordAbility.OptionalAdditionalCost =
        KeywordAbility.OptionalAdditionalCost(
            manaCost = grant.cost,
            displayPrefix = "Offspring",
            keyword = Keyword.OFFSPRING,
            declaredSlot = ChoiceSlot.OFFSPRING,
        )
}
