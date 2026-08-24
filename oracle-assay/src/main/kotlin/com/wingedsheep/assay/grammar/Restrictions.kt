package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.constant
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.assay.syntax.separated
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.CastRestriction

/**
 * When a spell may be cast and when an ability may be activated — the two restriction vocabularies,
 * plus the additional cost a spell's own line declares.
 *
 * All three are content a card states in a *sentence of its own* rather than as part of an effect,
 * and each lands in a different slot: `CardScript.castRestrictions`, `CardScript.additionalCosts`,
 * and `ActivatedAbility.restrictions`. They share a file because they share a shape — a printed
 * clause that constrains rather than does — and because the first two are the only lines in the
 * grammar that produce no effect at all.
 *
 * ### Restrictions are a run, not a rule per combination
 *
 * "Cast this spell only during the declare attackers step **and** only if you've been attacked this
 * step." is two restrictions joined by "and", and the model is a list of two. So the rule is a run
 * over a one-restriction vocabulary rather than a rule per printed combination, which is what keeps
 * a third restriction one row instead of doubling the table.
 */
object Restrictions {

    // ---------------------------------------------------------------------------------------
    // Casting
    // ---------------------------------------------------------------------------------------

    /**
     * One cast restriction.
     *
     * The step forms are enumerated rather than slotted over a step vocabulary because Oracle names
     * a step with an article and a noun phrase ("the declare attackers step") whose spelling is not
     * derivable from the SDK's `Step` constant, and a rule that guessed it would be reading an enum
     * name rather than a card.
     */
    private val one: Phrase<CastRestriction> = oneOf(
        "a casting restriction",
        constant(
            "only during the declare attackers step",
            CastRestriction.OnlyDuringStep(Step.DECLARE_ATTACKERS),
        ),
        constant(
            "only during the declare blockers step",
            CastRestriction.OnlyDuringStep(Step.DECLARE_BLOCKERS),
        ),
        phrase("only if {cond}", name = "only if a condition holds") {
            slot("cond", Conditions.condition)
            build { CastRestriction.OnlyIfCondition(it.value("cond")) }
            match { (it as? CastRestriction.OnlyIfCondition)?.let { r -> bind("cond" to r.condition) } }
        },
    )

    /** "Cast this spell only during …, and only if …" — the whole line. */
    val castLine: Phrase<List<CastRestriction>> =
        phrase("cast this spell {restrictions}.", name = "a casting restriction line") {
            slot("restrictions", separated("casting restrictions", one, " and "))
            build { it.value("restrictions") }
            match { restrictions -> restrictions.takeIf { it.isNotEmpty() }?.let { bind("restrictions" to it) } }
        }

    // ---------------------------------------------------------------------------------------
    // Additional costs
    // ---------------------------------------------------------------------------------------

    /**
     * "As an additional cost to cast this spell, sacrifice a creature." — the line that adds to what
     * a spell costs rather than to what it does.
     *
     * ### The clause after the comma is [Costs.additional], not a second cost vocabulary
     *
     * It used to be one rule that read "sacrifice {filter}" and nothing else, while the activation
     * side of the grammar read a longer list of the *same English* — two vocabularies for the one
     * thing `CostAtom`'s own KDoc calls "the one cost language". So this slots the shared atom
     * vocabulary, lifted into `AdditionalCost`, and every row [Costs] gains reaches this line for
     * free: "discard a card", "exile two creature cards from your graveyard", "pay 3 life",
     * "return a permanent you control to its owner's hand".
     *
     * A *list* of one, because `CardScript.additionalCosts` is a list and the sentence spells one
     * cost. The joined forms Oracle also prints — "sacrifice a creature **or pay** {3}{B}",
     * "you may exile any number of …" — are `AdditionalCost.Choice` and the optional rail rather
     * than a run over this, and they decline until they have rules of their own.
     */
    val additionalCostLine: Phrase<List<AdditionalCost>> =
        phrase("as an additional cost to cast this spell, {cost}.", name = "an additional cost line") {
            slot("cost", Costs.additional)
            build { listOf(it.value<AdditionalCost>("cost")) }
            match { costs -> costs.singleOrNull()?.let { bind("cost" to it) } }
        }

    // ---------------------------------------------------------------------------------------
    // Activation
    // ---------------------------------------------------------------------------------------

    /**
     * One activation restriction, and the run of them an ability's last sentence spells.
     *
     * "Activate only during your turn, before attackers are declared." joins its two with a comma
     * rather than with "and", which is a printed-shape difference from the casting line and the
     * reason each has its own separator rather than a shared one.
     */
    private val oneActivation: Phrase<ActivationRestriction> = oneOf(
        "an activation restriction",
        constant("during your turn", ActivationRestriction.OnlyDuringYourTurn),
        constant("before attackers are declared", ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)),
        constant("before blockers are declared", ActivationRestriction.BeforeStep(Step.DECLARE_BLOCKERS)),
        constant("once each turn", ActivationRestriction.OncePerTurn),
        // "Activate only during your upkeep." — Eternal Dragon, Undead Gladiator, Nim Devourer.
        //
        // **One printed phrase, two SDK restrictions, and therefore an `All` rather than two rows.**
        // "Your upkeep" names a step *and* whose turn it is, which the model has no single case for;
        // spelling it as two rows of this vocabulary would put it in the run [activationSentence]
        // joins with ", " and print "Activate only during your turn, during your upkeep." The corpus
        // agrees six cards to two, and the two are fixed in this change.
        constant(
            "during your upkeep",
            ActivationRestriction.All(
                ActivationRestriction.OnlyDuringYourTurn,
                ActivationRestriction.DuringStep(Step.UPKEEP),
            ),
        ),
        // "Activate only if you control a legendary creature.", "Activate only if you attacked this
        // turn." — the whole [Conditions] vocabulary, reached the same way [one]'s casting twin
        // reaches it. This row is what the recursion band ([Recursion]) needed: eight of its lines
        // print a bare condition after the move and nothing else.
        phrase("if {cond}", name = "only if a condition holds") {
            slot("cond", Conditions.condition)
            build { ActivationRestriction.OnlyIfCondition(it.value("cond")) }
            match { (it as? ActivationRestriction.OnlyIfCondition)?.let { r -> bind("cond" to r.condition) } }
        },
    )

    /** "Activate only during your turn, before attackers are declared." — the trailing sentence. */
    val activationSentence: Phrase<List<ActivationRestriction>> =
        phrase("activate only {restrictions}.", name = "an activation restriction sentence") {
            slot("restrictions", separated("activation restrictions", oneActivation, ", "))
            build { it.value("restrictions") }
            match { restrictions -> restrictions.takeIf { it.isNotEmpty() }?.let { bind("restrictions" to it) } }
        }
}
