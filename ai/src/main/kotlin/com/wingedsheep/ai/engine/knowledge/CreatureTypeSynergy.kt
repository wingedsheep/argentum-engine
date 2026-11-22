package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.scripting.GrantChosenSubtype
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.serialization.CardSerialization
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * The creature-type half of [SynergyAxes]: which creature types a card **is or makes** (it feeds
 * them) and which it **asks for** (it rewards them) — Goblin King asks for Goblins; every Goblin,
 * and every card that makes Goblin tokens, feeds it.
 *
 * "Asks for" is read off the card's structure, never its Oracle text: a creature type named by
 * any object filter anywhere in its abilities — the "other Goblins" a lord pumps, the "another Elf
 * you control" a trigger watches, the "Goblin spells" a cost reducer discounts, the "Goblin" a
 * sacrifice cost or a tutor wants, the Pirate an Aura's "as long as enchanted creature is a Pirate"
 * gates its bonus on. A creature-type predicate under a `Not` ("non-Goblin") doesn't
 * count, nor does one in a filter scoped to an opponent's objects ("Merfolk an opponent controls"),
 * nor a static that shrinks the type ("all Goblins get -1/-1"), which [SynergyStrength.punished] reads.
 *
 * The filters live anywhere in the ability tree (statics, triggers, costs, effects, targets), so
 * this walks the card's serialized script — the same canonical form `CardValidator` and the card
 * snapshots read — rather than enumerating every SDK type that can hold a filter. A hoser that names
 * a type with no controller ("destroy all Goblins") would read as asking for it; such cards are
 * rare enough that the miss is cheaper than a hand-kept list of effect types.
 */
internal object CreatureTypeSynergy {

    /** Creature types [card] asks for, read from the filters in its abilities. */
    fun rewarded(card: CardDefinition): Set<Subtype> {
        val names = mutableSetOf<String>()
        for (script in scripts(card)) collect(CardSerialization.json.encodeToJsonElement(CardScript.serializer(), script), names)
        return names.filter { it in CREATURE_TYPES }.mapTo(mutableSetOf(), ::Subtype)
    }

    /** Creature types [card] is (its type line, creature or kindred) or makes as tokens. */
    fun fed(card: CardDefinition): Set<Subtype> = buildSet {
        card.typeLine.subtypes.filterTo(this) { it.value in CREATURE_TYPES }
        for (script in scripts(card)) {
            val effects = listOfNotNull(script.spellEffect) +
                script.triggeredAbilities.map { it.effect } +
                script.activatedAbilities.filterNot { it.isManaAbility }.map { it.effect }
            for (effect in effects) for (leaf in EffectWalker.leaves(effect)) {
                if (leaf is CreateTokenEffect) leaf.creatureTypes.filter { it in CREATURE_TYPES }.mapTo(this, ::Subtype)
            }
        }
        // A creature land is its animation's types while it fights (Treetop Village is an Ape).
        for (animation in selfAnimations(card)) animation.creatureTypes.filter { it in CREATURE_TYPES }.mapTo(this, ::Subtype)
    }

    /** Broad own-team conversion needs both off-tribe bodies and a tribal payoff. */
    fun grantsChosenTribe(card: CardDefinition): Boolean = scripts(card).any { script ->
        script.staticAbilities.any { it is GrantChosenSubtype && it.filter == GroupFilter.AllCreaturesYouControl }
    }

    /**
     * A payoff for "the chosen creature type": a static that helps your creatures of the type named
     * as it entered ("creatures you control of the chosen type get +1/+1", Banner of Kinship, Door
     * of Destinies) or makes your spells of that type cheaper. Which type is a game choice, so it
     * rewards whichever tribe the deck has most of ([DeckSynergy.Counts.bestTribeMembers]).
     */
    fun rewardsChosenTribe(card: CardDefinition): Boolean = scripts(card).any { script ->
        script.staticAbilities.flatMap(::unwrap).any(::helpsChosenType)
    }

    private fun helpsChosenType(ability: StaticAbility): Boolean {
        if (ability is ModifySpellCost) {
            val filter = (ability.target as? SpellCostTarget.YouCast)?.filter ?: return false
            return CardPredicate.HasChosenSubtype in filter.cardPredicates
        }
        val group: GroupFilter = when (ability) {
            is ModifyStats -> ability.filter.takeIf { ability.powerBonus + ability.toughnessBonus > 0 }
            is GrantDynamicStats -> ability.filter
            is GrantKeyword -> ability.filter
            else -> null
        } ?: return false
        return group.chosenSubtypeKey != null && group.scope is Scope.Battlefield && !opponents(group.baseFilter)
    }

    private fun opponents(filter: GameObjectFilter): Boolean =
        filter.controllerPredicate == ControllerPredicate.ControlledByOpponent || filter.anyOf.any(::opponents)

    private fun unwrap(ability: StaticAbility): List<StaticAbility> = when (ability) {
        is ConditionalStaticAbility -> unwrap(ability.ability)
        is CompositeStaticAbility -> ability.abilities.flatMap(::unwrap)
        else -> listOf(ability)
    }

    /**
     * A changeling is every creature type, so it feeds every tribe — printed, or granted by the
     * card's own animation ("becomes a 2/2 creature with all creature types", Mutavault).
     */
    fun everyType(card: CardDefinition): Boolean = Keyword.CHANGELING in card.keywords ||
        selfAnimations(card).any { Keyword.CHANGELING in it.keywords }

    /** "This land becomes a … creature" effects the card's own non-mana activated abilities make. */
    private fun selfAnimations(card: CardDefinition): List<BecomeCreatureEffect> = scripts(card).flatMap { script ->
        script.activatedAbilities.filterNot { it.isManaAbility }.flatMap { EffectWalker.leaves(it.effect) }
            .filterIsInstance<BecomeCreatureEffect>().filter { it.target == EffectTarget.Self }
    }

    private fun scripts(card: CardDefinition): List<CardScript> = listOf(card.script) + card.cardFaces.map { it.script }

    private fun collect(element: JsonElement, out: MutableSet<String>) {
        when (element) {
            is JsonArray -> element.forEach { collect(it, out) }
            is JsonObject -> {
                val type = element.string("type")
                if (type == "Not") return
                // "All Goblins get -1/-1" names Goblins to shrink them: it punishes the tribe
                // ([SynergyStrength.punished]), it doesn't ask for it.
                if (type == "ModifyStats" && (element.int("powerBonus") ?: 0) + (element.int("toughnessBonus") ?: 0) < 0) return
                if ((element["controllerPredicate"] as? JsonObject)?.string("type") in OPPONENT_SCOPES) return
                // "If defending player controls no Walls" (Siege Dragon): a count of an opponent's
                // permanents names the tribe it fights, the way an opponent-scoped filter does.
                if (type in PLAYER_SCOPED_COUNTS && (element["player"] as? JsonObject)?.string("type") in OPPONENT_PLAYERS) return
                // "Must be blocked by an Eldrazi if able" (Slayer's Cleaver), "All Walls able to
                // block it do so" (Marble Priest): the blocker filter always names the defending
                // player's creatures — a tribe it fights, never one it asks for.
                if (type == "MustBeBlockedStatic") {
                    element.forEach { (key, value) -> if (key != "blockers") collect(value, out) }
                    return
                }
                // "Can block Dragons as though it had reach" (Dragon Hunter): the attacker filter
                // names the creatures it blocks — a tribe it walls, never one it asks for.
                if (type == "CanBlockAsThoughHadReach") {
                    element.forEach { (key, value) -> if (key != "attackers") collect(value, out) }
                    return
                }
                when (type) {
                    // "As long as enchanted creature is a Pirate" (Sea Legs, Gift of Fangs) wants the
                    // Aura on one of that tribe; its "otherwise" half sits under the `Not` above.
                    "HasSubtype", "EnchantedCreatureHasSubtype" -> element.string("subtype")?.let(out::add)
                    "HasAnyOfSubtypes" -> (element["subtypes"] as? JsonArray)
                        ?.mapNotNullTo(out) { (it as? JsonPrimitive)?.contentOrNull }
                }
                element.values.forEach { collect(it, out) }
            }
            else -> Unit
        }
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.int(key: String): Int? = string(key)?.toIntOrNull()

    private val CREATURE_TYPES: Set<String> = Subtype.ALL_CREATURE_TYPES.toSet()

    private val PLAYER_SCOPED_COUNTS = setOf("Count", "AggregateBattlefield")

    private val OPPONENT_PLAYERS = setOf(
        "AnOpponent", "DefendingPlayer", "EachDefendingPlayer", "EachAttackedPlayer", "EachOpponent",
        "TargetOpponent", "ChosenOpponent",
    )

    private val OPPONENT_SCOPES = setOf(
        "ControlledByOpponent", "OwnedByOpponent", "ControlledByDefendingPlayer", "ControlledByTargetOpponent",
    )
}
