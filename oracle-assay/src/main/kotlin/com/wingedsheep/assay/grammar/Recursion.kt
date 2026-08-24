package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.alternate
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ReturnSelfFromZoneTransformedEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Recursion — the card that moves **itself**, out of a zone the sentence names: "Return ~ from your
 * graveyard to your hand.", "{1}{B}: Return ~ from your graveyard to the battlefield tapped."
 *
 * Every other self-move in the grammar is written from the battlefield, where the source zone is not
 * printed because there is only one it could be ([SelfSteps.retargetable]'s "exile it", "return it
 * to its owner's hand", "put it on top of its owner's library"). Here it *is* printed, and that one
 * phrase is the whole family — 131 declined lines, second on the tail ranking, and the construct
 * behind them is not a verb the grammar was missing.
 *
 * ## What the printed source zone does to the model
 *
 * `MoveToZoneEffect` is one parameterized type — a destination, a [ZonePlacement], a library
 * position, a counter to land with, and a [MoveToZoneEffect.fromZone] guard — and the grammar was
 * reaching it with `target = Self` through exactly two frozen constants. The family is its product,
 * which is why this file is a table and three riders rather than sixty-three sentences.
 *
 * The part that is *not* on the effect is the reason the family declined rather than merely printing
 * a shorter sentence. "From your graveyard" says two things at once:
 *
 *  - at resolution, that the move happens only if the card is still there — the effect's `fromZone`;
 *  - at activation, that the ability can be used at all while the card sits in the graveyard —
 *    `ActivatedAbility.activateFromZone`, `TriggeredAbility.activeZones`.
 *
 * [Activated.abilityFor] reconstructs and compares the whole ability, so until now *any* ability
 * whose activation zone was not the battlefield refused to print, whatever its effect clause said.
 * That is what 74 sole-blocked cards were waiting on.
 *
 * ## The second field is derived, not spelled — CR 113.6m
 *
 * One printed phrase filling two SDK fields is the shape this module's rule "a value the SDK carries
 * twice is derived, not spelled" exists for, and here the rules text does the deriving:
 *
 * > **113.6m** An ability whose cost or effect specifies that it moves the object it's on out of a
 * > particular zone functions only in that zone, unless its trigger condition or a previous part of
 * > its cost or effect specifies that the object is put into that zone…
 *
 * So [functionsIn] reads the ability's activation zone off its own effect, exactly as
 * [Activated.producesMana] reads mana-ability-ness off it for CR 605.1a, and no value has to travel
 * from a clause up to the ability that contains it. That mattered more than it looks: the trigger
 * members of this family print the move **inside a gate** ("Whenever a land you control enters, you
 * may return ~ from your graveyard to the battlefield." — Bloodghast), so a zone carried as a
 * clause-level value would have had to thread through every wrapper in [Steps]' cascade. A
 * derivation walks down to it instead, and the trigger half came free with the activated one.
 *
 * The rule's "unless" clause is not decoration — it is the Ojer cycle. "When Ojer Taq dies, return
 * it to the battlefield transformed" carries the same `fromZone = GRAVEYARD` and functions on the
 * battlefield, because the trigger condition is what put the card in the graveyard. [functionsIn]
 * therefore takes the cost and the trigger event beside the effect, and each can cancel the
 * derivation.
 *
 * ## What the corpus said back
 *
 * 26 hand-written cards carry a self-return from the graveyard. Six write the `fromZone` guard and
 * twenty do not, and the twenty have a live bug rather than a shorter model: `ActivateAbilityHandler`
 * checks `activateFromZone` when the ability is *activated* and nothing re-checks it on resolution,
 * so a Reassembling Skeleton exiled from the graveyard in response to its own ability still came
 * back. [Graveyard]'s own note records the same guard being tried and kept. The differential named
 * all twenty and they are fixed in this change.
 */
object Recursion {

    /**
     * One printed "{verb} ~ from your X to Y" span, whole.
     *
     * **The verb is part of the row, not a word in front of a destination slot.** English agrees on
     * the pair: a card comes back "from your graveyard **to** the battlefield" and goes "from your
     * hand **onto** the battlefield", and the same destination takes both spellings depending on
     * which verb opens the clause. A grammar that slotted the destination would leave the verb and
     * its preposition undetermined by the model, which is the target-quantifier band's argument for
     * a table one axis over.
     *
     * @param riders whether the row takes [landings] and [CounterForm] after it. A row *declares
     *   which riders it takes*: "to your hand" is never printed tapped and never lands with
     *   counters, so offering it those would let the printer spell sentences Oracle does not have.
     * @param canonical false for the second spelling of a move already in the table — it parses and
     *   never prints. "Put ~ from your graveyard onto the battlefield" and "Return ~ from your
     *   graveyard to the battlefield" are one model, and the corpus prints the second 48 times
     *   against the first's 1.
     */
    private data class Move(
        val surface: String,
        val from: Zone,
        val destination: Zone,
        val placement: ZonePlacement = ZonePlacement.Default,
        val riders: Boolean = false,
        val canonical: Boolean = true,
    )

    private val moves: List<Move> = listOf(
        Move("return {self} from your graveyard to your hand", Zone.GRAVEYARD, Zone.HAND),
        Move(
            "return {self} from your graveyard to the battlefield",
            Zone.GRAVEYARD,
            Zone.BATTLEFIELD,
            riders = true,
        ),
        Move(
            "put {self} from your graveyard onto the battlefield",
            Zone.GRAVEYARD,
            Zone.BATTLEFIELD,
            riders = true,
            canonical = false,
        ),
        Move("put {self} from your hand onto the battlefield", Zone.HAND, Zone.BATTLEFIELD, riders = true),
        // "Put ~ from your graveyard on top of your library." — Krark-Clan Ogre's Cranial Plating
        // sibling. A placement rather than a destination of its own, and one the riders never reach:
        // a card on a library is not tapped and does not land with counters.
        Move(
            "put {self} from your graveyard on top of your library",
            Zone.GRAVEYARD,
            Zone.LIBRARY,
            placement = ZonePlacement.Top,
        ),
    )

    /**
     * How the permanent arrives — the [ZonePlacement] rows English spells after the destination.
     *
     * The empty row is the whole reason this is a table: `Default` is what Oracle says by saying
     * nothing, so a rule that spelled " tapped" as required template text is exactly the shape that
     * put 19 bare "…to the battlefield." lines in the final-period family. The three rows take
     * disjoint models, so the model still decides which prints.
     */
    private data class Landing(val surface: String, val placement: ZonePlacement)

    private val landings: List<Landing> = listOf(
        Landing("", ZonePlacement.Default),
        Landing(" tapped", ZonePlacement.Tapped),
        Landing(" tapped and attacking", ZonePlacement.TappedAndAttacking),
    )

    /**
     * What it lands with — "…to the battlefield with a finality counter on it", "…tapped with two
     * +1/+1 counters on it".
     *
     * The corpus spells this as a `Composite` of the move and an `AddCounters` aimed at the source,
     * not as [MoveToZoneEffect.addCounterType]: eight cards write the composite, none writes the
     * field, and the field holds one counter where the sentence can print several. So the field is a
     * second SDK spelling this rule deliberately does not emit — see the note on [functionsIn] for
     * the same judgement made the other way.
     *
     * Singular and plural are separate rows over disjoint counts for [Steps]' reason, and the
     * article rides inside [Primitives.singularCounterKind] rather than sitting in the template.
     */
    private enum class CounterForm(val surface: String) {
        NONE(""),
        ONE(" with {kind} counter on it"),
        MANY(" with {n} {kind} counters on it"),
    }

    /** One rule: a move, how it lands, and what it lands with. */
    private fun moveRule(move: Move, landing: Landing, counters: CounterForm): Phrase<CardScript> {
        fun scriptFor(kind: String?, count: Int): CardScript {
            val moved = Effects.Move(
                EffectTarget.Self,
                move.destination,
                placement = if (move.riders) landing.placement else move.placement,
                fromZone = move.from,
            )
            return CardScript(
                spellEffect = if (kind == null) {
                    moved
                } else {
                    Effects.Composite(listOf(moved, Effects.AddCounters(kind, count, EffectTarget.Self)))
                }
            )
        }
        // The rule name is the printed span with its slots left as their nouns, so the explorer's
        // rule tree and an ambiguity diagnostic both name the row rather than the family.
        val name = (move.surface + landing.surface + counters.surface)
            .replace("{self}", "the source")
            .replace("{kind}", "a counter kind")
            .replace("{n}", "n")
        return phrase(move.surface + landing.surface + counters.surface, name = name) {
            slot("self", Primitives.self)
            when (counters) {
                CounterForm.NONE -> Unit
                CounterForm.ONE -> slot("kind", Primitives.singularCounterKind)
                CounterForm.MANY -> {
                    slot("n", Cardinals.word)
                    slot("kind", Primitives.counterKind)
                }
            }
            build { bindings ->
                when (counters) {
                    CounterForm.NONE -> scriptFor(null, 0)
                    CounterForm.ONE -> scriptFor(bindings.value("kind"), 1)
                    CounterForm.MANY -> scriptFor(bindings.value("kind"), bindings.int("n"))
                }
            }
            match { script ->
                val landed = counterRider(script.spellEffect)
                if ((landed == null) != (counters == CounterForm.NONE)) return@match null
                val kind = landed?.first
                val count = landed?.second ?: 0
                when (counters) {
                    CounterForm.NONE -> Unit
                    CounterForm.ONE -> if (count != 1) return@match null
                    CounterForm.MANY ->
                        if (count < 2 || !Cardinals.spellable(count)) return@match null
                }
                if (script != scriptFor(kind, count)) return@match null
                bind("self" to Unit, "kind" to kind, "n" to count)
            }
        }
    }

    /**
     * The counter clause a composite carries, or null when the effect is a bare move.
     *
     * Only the two-element shape [moveRule] builds — anything longer is a clause run and belongs to
     * [Steps], not to this rider.
     */
    private fun counterRider(effect: Effect?): Pair<String, Int>? {
        val composite = effect as? CompositeEffect ?: return null
        if (composite.effects.size != 2) return null
        return Steps.countersAdded(composite.effects[1], EffectTarget.Self)
    }

    /**
     * "Return ~ from your graveyard to the battlefield transformed." — Garland, Knight of Cornelia.
     *
     * A row of its own because the model is a different type: `ReturnSelfFromZoneTransformedEffect`
     * exists because a card in a non-battlefield zone shows only its front face, so "transformed"
     * here is not a [ZonePlacement] that could join [landings] — it is the effect. Its own `tapped`
     * flag is the placement, which is why the pair is spelled here rather than slotted.
     */
    private fun transformedRule(surface: String, tapped: Boolean): Phrase<CardScript> {
        val script = CardScript(spellEffect = Effects.ReturnSelfFromGraveyardTransformed(tapped = tapped))
        return phrase(surface, name = "return the source transformed${if (tapped) " tapped" else ""}") {
            slot("self", Primitives.self)
            build { script }
            match { if (it == script) bind("self" to Unit) else null }
        }
    }

    private val transformed: List<Phrase<CardScript>> = listOf(
        transformedRule("return {self} from your graveyard to the battlefield transformed", tapped = false),
        transformedRule(
            "return {self} from your graveyard to the battlefield tapped and transformed",
            tapped = true,
        ),
        // The same two moves, the other verb — see [Move.canonical].
        alternate(transformedRule("put {self} from your graveyard onto the battlefield transformed", tapped = false)),
    )

    val clauses: List<Phrase<CardScript>> = moves.flatMap { move ->
        val rows = if (move.riders) {
            landings.flatMap { landing -> CounterForm.entries.map { moveRule(move, landing, it) } }
        } else {
            listOf(moveRule(move, landings.first(), CounterForm.NONE))
        }
        if (move.canonical) rows else rows.map(::alternate)
    } + transformed

    // ---------------------------------------------------------------------------------------
    // CR 113.6m — where the ability functions
    // ---------------------------------------------------------------------------------------

    /**
     * The zone an ability functions in, derived from what it moves the source out of — null for the
     * battlefield, which is CR 113.6's default and every ability's until one of these clauses is
     * printed.
     *
     * @param cost the ability's cost, or null for a triggered ability. Read for CR 113.6m's "unless
     *   … a previous part of its cost … specifies that the object is put into that zone": a cost that
     *   sacrifices or exiles the source is what put it where the effect then moves it from, so the
     *   ability still functions on the battlefield.
     * @param putsSourceInto the zone the ability's *trigger condition* puts the source into, or null
     *   when it has none. The other half of the same "unless" — a dies trigger that returns the card
     *   from the graveyard functions on the battlefield.
     */
    fun functionsIn(effect: Effect, cost: AbilityCost? = null, putsSourceInto: Zone? = null): Zone? {
        val moved = selfMoveSource(effect) ?: return null
        if (moved == putsSourceInto) return null
        if (cost != null && costPutsSourceInto(cost) == moved) return null
        return moved
    }

    /**
     * The zone [effect] takes the source out of.
     *
     * The walk descends through the two wrappers a printed clause builds — a `Composite` for a
     * sequence and a `GatedEffect` for "you may", "you may pay {2}. If you do," and an
     * intervening-if — because CR 113.6m asks what the effect *specifies*, and a move the controller
     * may decline is still specified. It goes no further for [Activated.producesMana]'s reason: an
     * iteration or a replacement is not a clause this grammar reads as a self-move.
     */
    private fun selfMoveSource(effect: Effect): Zone? = when (effect) {
        is MoveToZoneEffect -> effect.fromZone?.takeIf { effect.target == EffectTarget.Self }
        is ReturnSelfFromZoneTransformedEffect -> effect.fromZone
        is CompositeEffect -> effect.effects.firstNotNullOfOrNull(::selfMoveSource)
        is GatedEffect -> selfMoveSource(effect.then)
        else -> null
    }

    /** Where a cost that pays with the source itself leaves it. */
    private fun costPutsSourceInto(cost: AbilityCost): Zone? = when (cost) {
        is AbilityCost.SacrificeSelf -> Zone.GRAVEYARD
        is AbilityCost.ExileSelf -> Zone.EXILE
        is AbilityCost.Composite -> cost.costs.firstNotNullOfOrNull(::costPutsSourceInto)
        else -> null
    }
}
