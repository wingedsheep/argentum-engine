package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.ai.engine.LimitedCardRater
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells
import com.wingedsheep.sdk.scripting.GrantProtection
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * How much a payoff gives back per card that feeds it — the weight [DeckSynergy] puts on a fed
 * payoff, so a +2/+2 lord pulls harder on its tribe than a +1/+1 one, and a "draw a card whenever a
 * creature dies" pulls harder than a "gain 1 life whenever a creature dies".
 *
 * Read off the abilities that cash the deck in, each priced with the limited rater's own effect and
 * keyword values ([LimitedCardRater.effectValue], [LimitedCardRater.keywordValue]) so the two never
 * disagree about what an effect is worth:
 *
 *  - **triggers** watching our own objects or spells (not "when this…", not an opponent's): the
 *    value of what the trigger does, which it does once per feeder;
 *  - **group statics** that help a battlefield group — "+1/+1 and mountainwalk" — priced per body:
 *    the stats the way a pump is priced, each keyword at its rating bonus;
 *  - **spell statics** — a cost reduction, flash, a keyword for your spells;
 *  - **prowess**, and an activated ability paid for by sacrificing a narrow kind of card (Auratog).
 *
 * The sum is divided by [REFERENCE] — Goblin King's +1/+1 and mountainwalk — and clamped to
 * [MIN, MAX]: 1.0 is a typical payoff. A payoff nothing here prices reads as 1.0, the unweighted
 * count [DeckSynergy] used before, so an unread card moves exactly as it did.
 *
 * [punished] is the other sign: creature types a static of the card *shrinks* ("all Goblins get
 * -1/-1"). Such a card does not ask for those creatures, and [DeckSynergy] counts every one of them
 * beside it as a loss.
 *
 * Memoized per card name.
 */
internal object SynergyStrength {

    /** Effect value of a typical payoff's per-feeder return: a +1/+1 lord with one small keyword. */
    const val REFERENCE = 0.5

    const val MIN = 0.25
    const val MAX = 2.5

    private val strengths = ConcurrentHashMap<String, Double>()
    private val punishments = ConcurrentHashMap<String, Map<Subtype, Double>>()

    fun of(card: CardDefinition): Double = strengths.getOrPut(card.name) {
        val value = scripts(card).sumOf(::payoffValue) + if (Keyword.PROWESS in card.keywords) PROWESS else 0.0
        if (value <= 0.0) 1.0 else (value / REFERENCE).coerceIn(MIN, MAX)
    }

    /** Creature types [card]'s statics shrink, each with how hard, on [of]'s scale. */
    fun punished(card: CardDefinition): Map<Subtype, Double> = punishments.getOrPut(card.name) {
        val out = HashMap<Subtype, Double>()
        for (static in scripts(card).flatMap { it.staticAbilities }.flatMap(::unwrap)) {
            if (static !is ModifyStats || static.powerBonus + static.toughnessBonus >= 0) continue
            val group = battlefieldGroup(static.filter) ?: continue
            if (opponents(group)) continue
            val weight = (statValue(static.powerBonus, static.toughnessBonus) / REFERENCE).coerceIn(MIN, MAX)
            for (tribe in creatureTypes(group)) out.merge(tribe, weight, ::maxOf)
        }
        out
    }

    private fun scripts(card: CardDefinition): List<CardScript> = listOf(card.script) + card.cardFaces.map { it.script }

    private fun payoffValue(script: CardScript): Double {
        var value = 0.0
        for (ability in script.triggeredAbilities) {
            if (watchesOurs(ability.trigger, ability.binding)) value += LimitedCardRater.effectValue(ability.effect)
        }
        for (static in script.staticAbilities.flatMap(::unwrap)) value += staticValue(static)
        for (ability in script.activatedAbilities) {
            // Per card fed: Piper of the Swarm's steal costs three Rats, so each Rat buys a third.
            val sacrificed = narrowSacrifices(ability.cost)
            if (sacrificed > 0) {
                value += LimitedCardRater.effectValue(ability.effect) * ACTIVATED_DISCOUNT / sacrificed
            }
        }
        return value
    }

    /** Whether a trigger fires off our own objects, spells or life — the events a deck feeds. */
    private fun watchesOurs(pattern: EventPattern, binding: TriggerBinding): Boolean = when (pattern) {
        is EventPattern.AnyOf -> pattern.events.any { watchesOurs(it, binding) }
        // "Whenever +1/+1 counters are put on this creature" is fed by the deck's counter makers.
        is EventPattern.CountersPlacedEvent -> pattern.counterType == CounterType.PLUS_ONE_PLUS_ONE
        else -> binding != TriggerBinding.SELF && when (pattern) {
            is EventPattern.ZoneChangeEvent -> !opponents(pattern.filter)
            is EventPattern.PermanentsEnteredEvent -> !opponents(pattern.filter)
            is EventPattern.CreaturesYouControlDiedEvent -> !opponents(pattern.filter)
            is EventPattern.AttackEvent -> pattern.filter?.let { !opponents(it) } ?: false
            is EventPattern.PermanentsSacrificedEvent, is EventPattern.TokenCreationEvent,
            is EventPattern.LifeGainEvent, is EventPattern.SpellCastEvent, is EventPattern.NthSpellCastEvent,
            -> true
            else -> false
        }
    }

    /** What a static gives each body or spell it helps; 0 for a static that helps no group. */
    private fun staticValue(static: StaticAbility): Double {
        val group = when (static) {
            is ModifyStats -> static.filter.takeIf { static.powerBonus + static.toughnessBonus > 0 }
            is GrantDynamicStats -> static.filter
            is GrantKeyword -> static.filter
            is GrantTriggeredAbility -> static.filter
            is GrantActivatedAbility -> static.filter
            is GrantWard -> static.filter
            is GrantProtection -> static.filter
            is CantBeBlocked -> static.filter
            else -> null
        }
        if (group != null) {
            val base = battlefieldGroup(group) ?: return 0.0
            if (opponents(base)) return 0.0
            return when (static) {
                is ModifyStats -> statValue(static.powerBonus, static.toughnessBonus)
                is GrantKeyword -> Keyword.entries.firstOrNull { it.name == static.keyword }
                    ?.let(LimitedCardRater::keywordValue)?.takeIf { it > 0.0 } ?: GRANT
                else -> GRANT
            }
        }
        return when (static) {
            is ModifySpellCost -> if (static.target is SpellCostTarget.YouCast && reduces(static.modification)) COST_REDUCTION else 0.0
            is GrantFlashToSpellType -> LimitedCardRater.keywordValue(Keyword.FLASH)
            is GrantKeywordToOwnSpells -> LimitedCardRater.keywordValue(static.keyword).takeIf { it > 0.0 } ?: GRANT
            else -> 0.0
        }
    }

    private fun reduces(modification: CostModification): Boolean = when (modification) {
        is CostModification.ReduceGeneric, is CostModification.ReduceGenericBy, is CostModification.ReduceColored,
        is CostModification.ReduceColoredPerUnit, is CostModification.ReduceColoredIfAnyTargetMatches -> true
        else -> false
    }

    /** A pump's value per body, as the rater prices `ModifyStatsEffect`. */
    private fun statValue(power: Int, toughness: Int): Double = abs(power + toughness) * STAT_POINT

    private fun battlefieldGroup(group: GroupFilter): GameObjectFilter? =
        group.baseFilter.takeIf { group.scope is Scope.Battlefield && group.chosenSubtypeKey == null }

    private fun creatureTypes(filter: GameObjectFilter): Set<Subtype> =
        (filter.cardPredicates + filter.anyOf.flatMap { it.cardPredicates }).flatMap { predicate ->
            when (predicate) {
                is CardPredicate.HasSubtype -> listOf(predicate.subtype)
                is CardPredicate.HasAnyOfSubtypes -> predicate.subtypes
                else -> emptyList()
            }
        }.filterTo(mutableSetOf()) { it.value in CREATURE_TYPES }

    /** How many permanents of a narrow kind [cost] sacrifices; 0 when it sacrifices none. */
    private fun narrowSacrifices(cost: AbilityCost): Int = when (cost) {
        is AbilityCost.Atom -> (cost.atom as? CostAtom.Sacrifice)
            ?.takeIf { PrintedFilter.isNarrow(it.filter, spells = false) == true }?.count ?: 0
        is AbilityCost.Composite -> cost.costs.sumOf(::narrowSacrifices)
        else -> 0
    }

    private fun unwrap(ability: StaticAbility): List<StaticAbility> = when (ability) {
        is ConditionalStaticAbility -> unwrap(ability.ability)
        is CompositeStaticAbility -> ability.abilities.flatMap(::unwrap)
        else -> listOf(ability)
    }

    private fun opponents(filter: GameObjectFilter): Boolean =
        filter.controllerPredicate in OPPONENT_SCOPES || filter.anyOf.any(::opponents)

    /** One point of power or toughness on one body — the rater's `ModifyStatsEffect` price. */
    private const val STAT_POINT = 0.15

    /** Prowess: +1/+1 until end of turn per noncreature spell. */
    private const val PROWESS = 2 * STAT_POINT

    /** A granted ability, ward or protection no keyword table prices. */
    private const val GRANT = 0.3

    /** "Spells you cast cost {1} less." */
    private const val COST_REDUCTION = 0.4

    /** The rater's discount on an activated ability, which costs resources every time. */
    private const val ACTIVATED_DISCOUNT = 0.6

    private val CREATURE_TYPES: Set<String> = Subtype.ALL_CREATURE_TYPES.toSet()

    private val OPPONENT_SCOPES: Set<ControllerPredicate> = setOf(
        ControllerPredicate.ControlledByOpponent,
        ControllerPredicate.OwnedByOpponent,
        ControllerPredicate.ControlledByDefendingPlayer,
        ControllerPredicate.ControlledByTargetOpponent,
    )
}
