package com.wingedsheep.engine.mechanics.mana
import com.wingedsheep.engine.state.components.battlefield.chosenColor

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.LandControllerScope
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Inspects what colors a land's mana abilities *could* produce, regardless of whether
 * those abilities can currently be activated. Used by Fellwar Stone-style effects
 * (CR rulings: "checks the effects of all mana-producing abilities of lands ... but
 * doesn't check their costs").
 *
 * Reflection (CR 106.7): a land whose own mana ability is itself "could produce"-shaped
 * (Reflecting Pool, Exotic Orchard) reflects the lands it names, *excluding every land already
 * being inspected on this chain*. That is what makes two Reflecting Pools with no other lands
 * produce nothing ("Multiple Reflecting Pools won't help each other produce mana"), and keeps a
 * Pool from reflecting its own colors or its own {C}.
 *
 * Colorless production is intentionally ignored — Fellwar Stone-style abilities
 * produce only colored mana ("Fellwar Stone can't be tapped for colorless mana, even
 * if a land an opponent controls could produce colorless mana").
 */
object LandManaColorInspector {

    /**
     * The colors that any land in [landIds] could produce. Lands in [visiting] — the chain of
     * lands already being inspected — are skipped, so reflection never loops back on itself.
     */
    fun colorsLandsCouldProduce(
        state: GameState,
        projected: ProjectedState,
        landIds: Iterable<EntityId>,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
        visiting: Set<EntityId> = emptySet(),
    ): Set<Color> {
        val colors = mutableSetOf<Color>()
        for (landId in landIds) {
            if (landId in visiting) continue
            colors.addAll(colorsLandCouldProduce(state, projected, landId, cardRegistry, predicateEvaluator, visiting))
            if (colors.size == Color.entries.size) return colors
        }
        return colors
    }

    /**
     * The colors a single land could produce via any of its mana abilities.
     * Returns the empty set for non-lands.
     */
    fun colorsLandCouldProduce(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
        visiting: Set<EntityId> = emptySet(),
    ): Set<Color> {
        val container = state.getEntity(entityId) ?: return emptySet()
        val chain = visiting + entityId
        val reflect = { colorSet: ManaColorSet.LandsCouldProduce ->
            val lands = landsInScope(state, projected, entityId, colorSet.scope, colorSet.landFilter, predicateEvaluator)
            colorsLandsCouldProduce(state, projected, lands, cardRegistry, predicateEvaluator, chain)
        }
        val colors = mutableSetOf<Color>()
        for (ability in manaAbilities(state, projected, entityId, cardRegistry)) {
            collectColors(ability.effect, container, colors, reflect)
        }
        return colors
    }

    /**
     * Whether a single land could produce colorless mana ({C}) via any of its mana abilities — the
     * colorless half of "any **type** a land could produce" (colorless is a type of mana, CR 106.1b;
     * "could produce" is CR 106.7).
     * Costs (CR 106.7), activation restrictions and tapped state (Reflecting Pool rulings) are ignored, like [colorsLandCouldProduce] —
     * except the gate of a reflected {C} half (see [reflectedColorlessGate]), which *is* the
     * reflection: that {C} counts only when another land on the chain could produce {C}.
     * False for non-lands.
     */
    fun landCouldProduceColorless(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
        visiting: Set<EntityId> = emptySet(),
    ): Boolean {
        val chain = visiting + entityId
        return manaAbilities(state, projected, entityId, cardRegistry).any { ability ->
            if (!producesColorless(ability.effect)) return@any false
            val gate = reflectedColorlessGate(ability) ?: return@any true
            // A reflective gate whose player names no land scope fails closed rather than
            // letting the {C} half unlock itself.
            val scope = gate.scope ?: return@any false
            landsInScope(state, projected, entityId, scope, landFilter = null, predicateEvaluator).any {
                it !in chain && landCouldProduceColorless(state, projected, it, cardRegistry, predicateEvaluator, chain)
            }
        }
    }

    /**
     * The lands [scope] names, relative to the controller of [sourceId] (Reflecting Pool's "lands
     * you control", Exotic Orchard's "a land an opponent controls"), narrowed to [landFilter] when
     * one is given (Plaza of Harmony's "a Gate you control").
     */
    fun landsInScope(
        state: GameState,
        projected: ProjectedState,
        sourceId: EntityId,
        scope: LandControllerScope,
        landFilter: GameObjectFilter?,
        predicateEvaluator: PredicateEvaluator,
    ): List<EntityId> {
        val controllerId = projected.getController(sourceId) ?: return emptyList()
        return landsInScopeOf(state, projected, controllerId, scope, landFilter, predicateEvaluator)
    }

    /** The lands [scope] names, relative to [controllerId], narrowed to [landFilter] when one is given. */
    fun landsInScopeOf(
        state: GameState,
        projected: ProjectedState,
        controllerId: EntityId,
        scope: LandControllerScope,
        landFilter: GameObjectFilter?,
        predicateEvaluator: PredicateEvaluator,
    ): List<EntityId> {
        val opponents = if (scope == LandControllerScope.OPPONENTS) state.getOpponents(controllerId).toHashSet() else emptySet()
        val context = PredicateContext(controllerId = controllerId)
        return state.getBattlefield().filter { permId ->
            projected.hasType(permId, "LAND") && when (scope) {
                LandControllerScope.YOU -> projected.getController(permId) == controllerId
                LandControllerScope.OPPONENTS -> projected.getController(permId) in opponents
                LandControllerScope.ANY -> true
            } && (landFilter == null || predicateEvaluator.matches(state, projected, permId, landFilter, context))
        }
    }

    /**
     * Every mana ability the land has: intrinsic basic-land-type abilities, else its printed ones,
     * plus granted ones.
     */
    private fun manaAbilities(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        cardRegistry: CardRegistry,
    ): List<ActivatedAbility> {
        val container = state.getEntity(entityId) ?: return emptyList()
        val card = container.get<CardComponent>() ?: return emptyList()
        if (!projected.hasType(entityId, "LAND")) return emptyList()

        val abilities = mutableListOf<ActivatedAbility>()

        // A land that has lost all abilities (e.g. Imprisoned in the Moon) keeps its land
        // subtypes (only card types/abilities are overwritten, not subtypes — CR 205.4b) but per
        // ruling loses the intrinsic mana ability that subtype implies, and loses its printed
        // mana ability too. Only granted abilities (handled below) survive.
        // A face-down permanent has none of its card's abilities either (CR 708.2) — and it is a
        // land only if an effect made it one.
        val ownAbilitiesSuppressed = projected.hasLostAllAbilities(entityId) || projected.isFaceDown(entityId)

        if (!ownAbilitiesSuppressed) {
            // Intrinsic abilities derived from projected basic-land subtypes (Plains/Island/...).
            val intrinsic = IntrinsicManaAbilities.forEntity(state, projected, entityId)
            abilities.addAll(intrinsic)

            // Card-defined activated abilities (if any). Skip when intrinsic abilities are
            // present — basic land types replace the printed mana ability (matches
            // ManaAbilityEnumerator's behavior for shock lands etc.).
            if (intrinsic.isEmpty()) {
                cardRegistry.getCard(card.cardDefinitionId)?.script?.activatedAbilities
                    ?.filterTo(abilities) { it.isManaAbility }
            }
        }

        // Granted mana abilities (e.g., from auras / continuous effects)
        state.grantedActivatedAbilities
            .filter { it.entityId == entityId && it.ability.isManaAbility }
            .mapTo(abilities) { it.ability }

        return abilities
    }

    /**
     * The gate of a reflected {C} half, or null for an ordinary {C} ability. "Add one mana
     * of any **type** a land you control could produce" is authored as two mana abilities (Naga
     * Vitalist, Reflecting Pool): the colors through `ManaColorSet.LandsCouldProduce`, and `{C}`
     * gated on `Exists(<player>, battlefield, <land filter>.couldProduceColorlessMana())`. That
     * gate is the colorless reflection, not an activation legality check, so it is read here as
     * one.
     */
    private fun reflectedColorlessGate(ability: ActivatedAbility): ReflectedGate? =
        ability.restrictions.firstNotNullOfOrNull { restriction ->
            val gate = (restriction as? ActivationRestriction.OnlyIfCondition)?.condition as? Exists
            when {
                gate == null || gate.negate || gate.zone != Zone.BATTLEFIELD -> null
                CardPredicate.CouldProduceColorlessMana !in gate.filter.cardPredicates -> null
                else -> ReflectedGate(
                    when (gate.player) {
                        Player.You -> LandControllerScope.YOU
                        Player.EachOpponent -> LandControllerScope.OPPONENTS
                        Player.Any -> LandControllerScope.ANY
                        else -> null
                    }
                )
            }
        }

    /** A reflected {C} gate and the lands it reflects; a null [scope] names none (fails closed). */
    private data class ReflectedGate(val scope: LandControllerScope?)

    private fun producesColorless(effect: Effect): Boolean = when (effect) {
        is AddColorlessManaEffect -> true
        is CompositeEffect -> effect.effects.any(::producesColorless)
        else -> false
    }

    private fun collectColors(
        effect: Effect,
        sourceContainer: com.wingedsheep.engine.state.ComponentContainer,
        out: MutableSet<Color>,
        reflect: (ManaColorSet.LandsCouldProduce) -> Set<Color>,
    ) {
        when (effect) {
            is AddManaEffect -> out.add(effect.color)
            is AddColorlessManaEffect -> Unit
            is AddManaOfChoiceEffect -> collectChoiceColors(effect.colorSet, sourceContainer, out, reflect)
            is AddDynamicManaEffect -> out.addAll(effect.allowedColors)
            is CompositeEffect -> {
                for (sub in effect.effects) collectColors(sub, sourceContainer, out, reflect)
            }
            else -> Unit
        }
    }

    /**
     * Stateless approximation of [ManaColorSetResolver] used here because we only have a
     * source container, not full game state. [ManaColorSet.LandsCouldProduce] is resolved
     * through [reflect] (the land's own scope, minus the lands already on the chain). For the
     * other color sets that need state to resolve (commander identity, colors among permanents),
     * we conservatively report all five colors — matches the legacy `AddManaOfColorAmong`
     * behavior and is the safe choice for "could this land produce X?" lookups.
     */
    private fun collectChoiceColors(
        colorSet: ManaColorSet,
        sourceContainer: com.wingedsheep.engine.state.ComponentContainer,
        out: MutableSet<Color>,
        reflect: (ManaColorSet.LandsCouldProduce) -> Set<Color>,
    ) {
        when (colorSet) {
            is ManaColorSet.AnyColor -> out.addAll(Color.entries)
            is ManaColorSet.Specific -> out.addAll(colorSet.colors)
            is ManaColorSet.SourceChosenColor -> {
                sourceContainer.chosenColor()?.let { out.add(it) }
            }
            is ManaColorSet.Union -> colorSet.members.forEach { collectChoiceColors(it, sourceContainer, out, reflect) }
            // No resolution-time choice exists outside an effect, so nothing is removed.
            is ManaColorSet.ExceptChosenColor -> collectChoiceColors(colorSet.base, sourceContainer, out, reflect)
            is ManaColorSet.LandsCouldProduce -> out.addAll(reflect(colorSet))
            is ManaColorSet.CommanderIdentity,
            is ManaColorSet.AmongPermanents,
            is ManaColorSet.AmongCardsInGraveyard,
            is ManaColorSet.AmongLinkedExiledCards,
            is ManaColorSet.ColorsOf -> out.addAll(Color.entries)
        }
    }
}
