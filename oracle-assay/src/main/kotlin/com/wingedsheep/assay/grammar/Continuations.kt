package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.normalize.Normalizer
import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.OwnerGainsLifeEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Clauses that refer **back** — "Untap that creature.", "~ deals 2 damage to that creature."
 *
 * A sentence like these cannot start a line: "that creature" is an anaphor, and the thing it names
 * was introduced by an earlier sentence's `target` requirement. So these rules produce an effect
 * bound to [Targets.SLOT] while declaring **no** requirement of their own, and they are reachable
 * only from a later position in [Steps.sequence]. Registering them as ordinary clauses would let a
 * card's whole text be a dangling reference, which is a reading no printed card supports.
 *
 * ### Why "that creature" and not "it"
 *
 * Oracle uses two anaphors and they point at different things. "It" is the *source* — "When this
 * creature dies, put **it** on top of its owner's library" — which is [EffectTarget.Self] and lives
 * in [SelfSteps] as an ordinary clause, because a source needs no earlier sentence to introduce it.
 * "That creature" is the *target* the spell already chose. Keeping them in separate vocabularies is
 * what stops a sequence reading one as the other.
 */
object Continuations {

    /**
     * The shape: a verb over the slot an earlier sentence declared, and nothing else in the script.
     *
     * `match` reconstructs and compares like every other rule here, so a script that also carries a
     * requirement — the very thing this clause must not have — refuses to print.
     */
    private fun referringStep(
        template: String,
        name: String,
        effect: () -> Effect,
    ): Phrase<CardScript> {
        val script = CardScript(spellEffect = effect())
        return phrase(template, name = name) {
            build { script }
            match { if (it == script) bind() else null }
        }
    }

    private val untapThatCreature: Phrase<CardScript> =
        referringStep("untap that creature", "untap that creature") { Effects.Untap(Targets.bound()) }

    private val tapThatCreature: Phrase<CardScript> =
        referringStep("tap that creature", "tap that creature") { Effects.Tap(Targets.bound()) }

    /**
     * "~ deals 2 damage to that creature." — the counted verb over the anaphor.
     *
     * Its own rule rather than a row in [referringStep] because it carries a number, which changes
     * both halves of the inversion; the same reason [Steps] keeps its counted verbs apart from its
     * uncounted ones.
     */
    private val damageToThatCreature: Phrase<CardScript> = run {
        fun scriptFor(amount: Int) = CardScript(spellEffect = Effects.DealDamage(amount, Targets.bound()))
        phrase(
            "${Normalizer.SELF} deals {n} damage to that creature",
            name = "deals damage to that creature",
        ) {
            slot("n", Primitives.cardinal)
            build { scriptFor(it.int("n")) }
            match { script ->
                val amount = Steps.damageDealt(script.spellEffect ?: return@match null) ?: return@match null
                if (script != scriptFor(amount)) return@match null
                bind("n" to amount)
            }
        }
    }

    /**
     * "It gets +2/+4 until end of turn." — Inspirit, after "Untap target creature."
     *
     * The same surface form [SelfSteps.anaphoric] reads as the *source*, and the reason the two
     * vocabularies exist: once a clause has introduced a target, "it" is that target. This rule is
     * reachable only from a later position in a sequence and [SelfSteps]' is reachable only from the
     * first, so no text has both readings.
     *
     * That split was found by the differential rather than by reading: the line round-tripped
     * perfectly while meaning the wrong creature, which the touchstone structurally cannot see.
     */
    private val itGets: Phrase<CardScript> = run {
        fun scriptFor(modifiers: Pair<Int, Int>) = CardScript(
            spellEffect = Effects.ModifyStats(modifiers.first, modifiers.second, Targets.bound())
        )
        phrase("it gets {mod} until end of turn", name = "the target gets") {
            frontedDuration()
            slot("mod", Primitives.statModifiers)
            build { scriptFor(it.value("mod")) }
            match { script ->
                val modifiers = Steps.fixedModifiers(script.spellEffect) ?: return@match null
                if (script != scriptFor(modifiers)) return@match null
                bind("mod" to modifiers)
            }
        }
    }

    /**
     * "…and put a stun counter on it." — the counter verb over the target an earlier clause chose.
     *
     * [SelfSteps.putCountersOnSelf] is the identical English about the *source*, and this is the
     * second sentence to need both readings after "it gets +2/+4". The split costs a rule and buys
     * the guarantee: "When ~ enters, tap target creature an opponent controls and put a stun counter
     * on it" stuns the creature it tapped, not the permanent that tapped it, and both readings
     * round-trip byte-perfectly so nothing else in the module could tell them apart.
     */
    private val putCountersOnThatPermanent: List<Phrase<CardScript>> = run {
        fun scriptFor(kind: String, count: Int) =
            CardScript(spellEffect = Effects.AddCounters(kind, count, Targets.bound()))
        fun rule(template: String, name: String, quantity: Phrase<*>?) =
            phrase(template, name = name) {
                slot("kind", if (quantity == null) Primitives.singularCounterKind else Primitives.counterKind)
                if (quantity != null) slot("n", quantity)
                build { scriptFor(it.value("kind"), if (quantity == null) 1 else it.int("n")) }
                match { script ->
                    val (kind, count) =
                        Steps.countersAdded(script.spellEffect, Targets.bound()) ?: return@match null
                    if (quantity == null && count != 1) return@match null
                    if (quantity != null && !(count >= 2 && Cardinals.spellable(count))) return@match null
                    if (script != scriptFor(kind, count)) return@match null
                    bind("kind" to kind, "n" to count)
                }
            }
        listOf(
            rule("put {kind} counter on it", "put a counter on the target", null),
            rule("put {n} {kind} counters on it", "put counters on the target", Cardinals.word),
        )
    }

    /** "Its owner gains 4 life." — Path of Peace, referring to the creature the first clause destroyed. */
    private val ownerGainsLife: Phrase<CardScript> = run {
        fun scriptFor(amount: Int) = CardScript(spellEffect = OwnerGainsLifeEffect(amount))
        phrase("its owner gains {n} life", name = "its owner gains life") {
            slot("n", Primitives.cardinal)
            build { scriptFor(it.int("n")) }
            match { script ->
                val amount = (script.spellEffect as? OwnerGainsLifeEffect)?.amount ?: return@match null
                if (script != scriptFor(amount)) return@match null
                bind("n" to amount)
            }
        }
    }

    /**
     * "You draw a card for each Mountain and red card in it." — Baleful Stare, after the sentence
     * that revealed a hand.
     *
     * The "it" is the revealed hand, which the model names as a zone rather than as a slot: the
     * count is `DynamicAmount.Count(TargetOpponent, HAND, …)`, so the anaphor is carried by the
     * player and the zone together and nothing here reads the target. That is why it is a
     * continuation and not an ordinary clause — the sentence only means something after the reveal.
     *
     * The two-quality filter is an `or` of a land subtype and a colour, which is the printed
     * "**Mountain** and **red** card" read as a disjunction; Oracle's "and" here joins two ways for
     * a card to qualify rather than two requirements, and the model says so.
     */
    private val drawForEachInHand: Phrase<CardScript> = run {
        fun scriptFor(land: GameObjectFilter, colour: Color) = CardScript(
            spellEffect = Effects.DrawCards(
                DynamicAmount.Count(
                    Player.TargetOpponent,
                    Zone.HAND,
                    land or GameObjectFilter.Any.withColor(colour),
                )
            )
        )
        phrase(
            "you draw a card for each {land} and {color} card in it",
            name = "draw for each matching card in the revealed hand",
        ) {
            slot("land", Filters.filter)
            slot("color", Primitives.color)
            build { scriptFor(it.value("land"), it.value("color")) }
            match { script ->
                val count = (script.spellEffect as? DrawCardsEffect)?.count as? DynamicAmount.Count
                    ?: return@match null
                val (land, colour) = splitQualities(count.filter) ?: return@match null
                if (script != scriptFor(land, colour)) return@match null
                bind("land" to land, "color" to colour)
            }
        }
    }

    /**
     * The two halves of the `or` above, or null when the filter is anything else.
     *
     * `GameObjectFilter.or` folds two filters into a single `CardPredicate.Or` rather than into the
     * `anyOf` list, so the two qualities have to be read back out of that predicate — and only the
     * *subtype* and the *colour* are read, with the whole filter reconstructed and compared
     * afterwards. That is what keeps the rule fail-closed over a shape whose two halves are not
     * symmetric: the land side is an `And` of a type and a subtype, the colour side one predicate.
     */
    private fun splitQualities(filter: GameObjectFilter): Pair<GameObjectFilter, Color>? {
        val alternatives = (filter.cardPredicates.singleOrNull() as? CardPredicate.Or)
            ?.predicates?.takeIf { it.size == 2 } ?: return null
        val subtype = subtypeIn(alternatives[0]) ?: return null
        val colour = (alternatives[1] as? CardPredicate.HasColor)?.color ?: return null
        val land = GameObjectFilter.Land.withSubtype(subtype)
        return (land to colour).takeIf { filter == land or GameObjectFilter.Any.withColor(colour) }
    }

    /** The single subtype a predicate names, looking one level into a conjunction. */
    private fun subtypeIn(predicate: CardPredicate): com.wingedsheep.sdk.core.Subtype? = when (predicate) {
        is CardPredicate.HasSubtype -> predicate.subtype
        is CardPredicate.And -> predicate.predicates.filterIsInstance<CardPredicate.HasSubtype>()
            .singleOrNull()?.subtype

        else -> null
    }

    val all: List<Phrase<CardScript>> = listOf(
        untapThatCreature,
        tapThatCreature,
        damageToThatCreature,
        itGets,
        ownerGainsLife,
        drawForEachInHand,
    ) + putCountersOnThatPermanent + Prevention.continuationClauses + Combat.restrictionContinuationClauses

    val clause: Phrase<CardScript> = oneOf("a clause referring to the target", all)
}
