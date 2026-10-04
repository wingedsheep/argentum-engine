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
import com.wingedsheep.sdk.scripting.effects.OwnerGainsLifeEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Clauses that refer **back** — "Untap it.", "~ deals 2 damage to that creature."
 *
 * A sentence like these cannot start a line: the subject is an anaphor, and the thing it names was
 * introduced by an earlier sentence's `target` requirement. So these rules produce an effect bound
 * to [Targets.SLOT] while declaring **no** requirement of their own, and they are reachable only
 * from a later position in a [Steps] clause run. Registering them as ordinary clauses would let a
 * card's whole text be a dangling reference, which is a reading no printed card supports; a run
 * that reads the slot without declaring it is refused by [Steps.merge] for the same reason.
 *
 * ### One pronoun, two referents, decided by position
 *
 * Oracle spells both anaphors "it" and they point at different things. In a *first* clause "it" is
 * the source — "When this creature dies, put **it** on top of its owner's library" — which is
 * [EffectTarget.Self] and lives in [SelfSteps.anaphoric], because a source needs no earlier
 * sentence to introduce it. In a *later* one it is the target the spell already chose. Position is
 * the whole of the distinction, which is why the two vocabularies are two instantiations of one
 * shape rather than two lists of rules: [SelfSteps.anaphoric] is offered first and
 * [SelfSteps.continuing] only after, so no text has both readings.
 *
 * "That creature" and "that permanent" are the demonstratives Oracle prints where "it" would read
 * badly. They mean exactly what "it" means here, so they parse and the pronoun prints — see
 * [Primitives.targetPronoun], where the choice of canonical is a corpus measurement.
 *
 * ### The vocabulary is not written here
 *
 * The two anaphors point at different things; they do not have different *verbs*. So the clause
 * vocabulary is [SelfSteps.retargetable] — one shape over a subject spelling and an
 * [com.wingedsheep.sdk.scripting.targets.EffectTarget] — and this position is one instantiation of
 * it, [SelfSteps.continuing]. What stays in this file is the handful of clauses that have no
 * source-side twin at all. Anything else written here would be a second copy of a rule that already
 * exists, which is how a family becomes three hundred one-offs.
 */
object Continuations {

    /**
     * "~ deals 2 damage to that creature." — the counted verb over the anaphor.
     *
     * Its own rule rather than a row in [SelfSteps.retargetable] because the anaphor is its
     * *recipient*, not its subject, and it carries a number, which changes both halves of the
     * inversion; the same reason [Steps] keeps its counted verbs apart from its uncounted ones.
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

    /**
     * The vocabulary, and the reason this file is now short.
     *
     * [SelfSteps.continuing] is the bulk of it: the whole retargetable shape aimed at
     * [Targets.bound] instead of at the source, which is what "untap it", "it gets +2/+4 and gains
     * reach until end of turn", "put two +1/+1 counters on it", "regenerate it", "transform it" and
     * "it can't block this turn" all are. Five rules used to stand here, one printed sentence at a
     * time — and everything nobody had copied was simply unreadable: untap existed only as "untap
     * that creature", and regenerate, transform, gets-and-gains, the keyword grants, the animate and
     * the combat restrictions did not exist in this position at all. That is why "Target creature
     * gets +2/+2 and gains reach until end of turn. **Untap it.**" died on its own full stop, on
     * ninety-four lines of the `.` decline family.
     *
     * What is left here is what genuinely has no source-side twin: a verb whose *recipient* rather
     * than whose subject is the anaphor, and two clauses whose "it" is not an object at all. The
     * rider on a tap ("It doesn't untap during its controller's next untap step.") used to be a row
     * here too, frozen to "it"; it is a [SelfSteps.retargetable] member now, so "That creature
     * doesn't untap …" reads and "~ doesn't untap during your next untap step." reads beside it.
     */
    val all: List<Phrase<CardScript>> = listOf(
        damageToThatCreature,
        ownerGainsLife,
        drawForEachInHand,
    ) + SelfSteps.continuing + Prevention.continuationClauses

    val clause: Phrase<CardScript> = oneOf("a clause referring to the target", all)
}
