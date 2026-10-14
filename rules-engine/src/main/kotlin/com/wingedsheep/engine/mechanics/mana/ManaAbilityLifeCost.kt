package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.tapForMana
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.costs.CostAtomAmounts
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.handlers.effects.life.LifePaymentService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaSpendOnChosenTypeEffect
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * The life a mana ability costs to activate — the one place that adds it up:
 *  - the **printed** part: a [CostAtom.PayLife] in the ability's own cost (Mana Confluence's
 *    "{T}, Pay 1 life: Add one mana of any color");
 *  - the **tax**: "mana abilities of this land cost an additional N life to activate" (Thran Portal,
 *    [com.wingedsheep.sdk.scripting.ManaAbilitiesCostAdditionalLife]), a projected Layer 6 value
 *    ([com.wingedsheep.engine.mechanics.layers.ProjectedState.getManaAbilityLifeTax]) that
 *    disappears with the permanent's abilities and covers printed, intrinsic and granted abilities.
 *
 * Every place a mana ability is activated consults it here:
 *  - the manual pipeline (legal-action enumeration and the activation lookup) sees the printed
 *    atom already and folds the tax in via [withTax], so the ordinary cost payer charges the life
 *    and refuses the activation when it can't be paid (CR 119.4);
 *  - the auto-pay solver prices life-costing sources as pain sources and drops an ability whose
 *    life its controller can't pay ([ManaSolver.findAvailableManaSources]);
 *  - every auto-pay path that taps a solver- or player-chosen source charges it with [pay] (or
 *    [tapForManaPayingLife], the tap-and-charge pair) — the fast path skips the activation
 *    pipeline, so without this the life is silently never paid.
 */
object ManaAbilityLifeCost {

    /** The additional life each mana ability of [entityId] costs right now (0 for nearly everything). */
    fun tax(state: GameState, entityId: EntityId): Int =
        state.projectedState.getManaAbilityLifeTax(entityId)

    /**
     * The life [cost] itself asks [payerId] for — the sum of its [CostAtom.PayLife] atoms, each
     * priced through [CostAtomAmounts.life] (a state-priced amount reads the payer's game state).
     */
    fun printed(
        state: GameState,
        cost: AbilityCost,
        payerId: EntityId,
        predicateEvaluator: PredicateEvaluator,
        sourceId: EntityId? = null,
    ): Int = when (cost) {
        is AbilityCost.Atom -> (cost.atom as? CostAtom.PayLife)
            ?.let { CostAtomAmounts.life(state, it, payerId, predicateEvaluator, sourceId) } ?: 0
        is AbilityCost.Composite -> cost.costs.sumOf { printed(state, it, payerId, predicateEvaluator, sourceId) }
        else -> 0
    }

    /** Total life activating [ability] (null: an intrinsic, cost-free one) of [sourceId] costs [payerId]: printed + tax. */
    fun total(
        state: GameState,
        sourceId: EntityId,
        ability: ActivatedAbility?,
        payerId: EntityId,
        predicateEvaluator: PredicateEvaluator,
    ): Int = (ability?.let { printed(state, it.cost, payerId, predicateEvaluator, sourceId) } ?: 0) + tax(state, sourceId)

    /** [abilities] of [entityId] with the tax folded into each mana ability's cost. */
    fun withTax(state: GameState, entityId: EntityId, abilities: List<ActivatedAbility>): List<ActivatedAbility> {
        val tax = tax(state, entityId)
        if (tax <= 0) return abilities
        return abilities.map { withTax(it, tax) }
    }

    /** [ability] with [tax] additional life in its cost, when it is a mana ability. */
    fun withTax(ability: ActivatedAbility, tax: Int): ActivatedAbility {
        if (tax <= 0 || !ability.isManaAbility) return ability
        val life = AbilityCost.Atom(CostAtom.PayLife(tax))
        val cost = when (val c = ability.cost) {
            is AbilityCost.Composite -> AbilityCost.Composite(c.costs + life)
            else -> AbilityCost.Composite(listOf(c, life))
        }
        return ability.copy(cost = cost)
    }

    /**
     * The mana ability of [sourceId] an auto-pay tap activated: a printed one first, then one a
     * resolved effect granted it (`GameState.grantedActivatedAbilities` — e.g. Emrakul, the
     * Exigent Doom's "{T}: Add {C}{C}"). With a known [production] it is the ability that makes
     * that kind of mana; with none (an explicit source list that never says which ability) it is
     * the one costing the least life, the one a player would pick. Null when no printed or granted
     * ability applies (a basic land's intrinsic ability).
     */
    fun activatedManaAbility(
        cardRegistry: CardRegistry,
        state: GameState,
        sourceId: EntityId,
        production: ManaProduction?,
        payerId: EntityId,
        predicateEvaluator: PredicateEvaluator,
    ): ActivatedAbility? {
        val card = state.getEntity(sourceId)?.get<CardComponent>() ?: return null
        val printedAbilities = cardRegistry.getCard(card.cardDefinitionId)?.script?.activatedAbilities.orEmpty()
        val granted = state.grantedActivatedAbilities.asSequence()
            .filter { it.entityId == sourceId }
            .map { it.ability }
        val manaAbilities = (printedAbilities.asSequence() + granted).filter { it.isManaAbility }
        return if (production == null) manaAbilities.minByOrNull { printed(state, it.cost, payerId, predicateEvaluator, sourceId) }
        else manaAbilities.firstOrNull { abilityProducesColor(it, production.color) }
    }

    /**
     * Charge [payerId] the life activating [ability] of [sourceId] costs ([total]). A no-op (no
     * events) when it costs no life.
     */
    fun pay(
        zones: ZoneTransitionService,
        state: GameState,
        sourceId: EntityId,
        payerId: EntityId,
        ability: ActivatedAbility?,
    ): Pair<GameState, List<GameEvent>> {
        val life = total(state, sourceId, ability, payerId, zones.predicateEvaluator)
        if (life <= 0) return state to emptyList()
        return LifePaymentService.pay(zones, state, payerId, life) ?: (state to emptyList())
    }

    /**
     * [tapForMana] plus the ability's life cost — for the auto-pay paths that tap a chosen source
     * directly rather than through [ManaAbilitySideEffectExecutor.tapSourcesWithSideEffects].
     * [production] is the mana the tap is credited with, when the caller knows it. Nothing is
     * charged when the source was already tapped (no activation happened).
     */
    fun tapForManaPayingLife(
        zones: ZoneTransitionService,
        state: GameState,
        sourceId: EntityId,
        tapperId: EntityId,
        production: ManaProduction? = null,
    ): Pair<GameState, List<GameEvent>> {
        val (tapped, tapEvents) = tapForMana(state, sourceId, tapperId, production?.producedColors.orEmpty())
        if (tapEvents.isEmpty()) return tapped to tapEvents
        val ability = activatedManaAbility(zones.cardRegistry, tapped, sourceId, production, tapperId, zones.predicateEvaluator)
        val (paid, lifeEvents) = pay(zones, tapped, sourceId, tapperId, ability)
        return paid to (tapEvents + lifeEvents)
    }

    /** The mana a player-selected source is credited with: its first color, else colorless. */
    fun creditedProduction(producesColors: Collection<Color>): ManaProduction =
        producesColors.firstOrNull()?.let { ManaProduction(color = it) } ?: ManaProduction(color = null, colorless = 1)

    private fun abilityProducesColor(ability: ActivatedAbility, color: Color?): Boolean =
        manaSubEffects(ability.effect).any { effect -> effectProduces(effect, color) }

    private fun effectProduces(effect: Effect, color: Color?): Boolean = when (effect) {
        is AddManaEffect -> effect.color == color
        is AddColorlessManaEffect -> color == null
        is AddManaOfChoiceEffect,
        is AddAnyColorManaSpendOnChosenTypeEffect -> color != null  // any non-null color
        is AddDynamicManaEffect -> color != null && color in effect.allowedColors
        else -> false
    }

    private fun manaSubEffects(effect: Effect): List<Effect> = when (effect) {
        is CompositeEffect -> effect.effects.filter { isManaEffect(it) }
        else -> if (isManaEffect(effect)) listOf(effect) else emptyList()
    }

    /** Whether [effect] is one of the mana-adding effects (vs. a pain land's damage rider). */
    internal fun isManaEffect(effect: Effect): Boolean = effect is AddManaEffect ||
        effect is AddColorlessManaEffect ||
        effect is AddManaOfChoiceEffect ||
        effect is AddAnyColorManaSpendOnChosenTypeEffect ||
        effect is AddDynamicManaEffect
}
