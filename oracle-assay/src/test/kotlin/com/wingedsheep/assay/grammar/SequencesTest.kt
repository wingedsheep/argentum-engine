package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A line with more than one clause in it: the full stop, the two joins, the anaphors that make a
 * later clause mean anything, and the one fold the whole thing rests on — that a target is declared
 * at its first mention.
 */
class SequencesTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    /** The slot one clause's effect reads, found the way a reader would: by looking at its target. */
    fun referencedSlot(effect: Effect): String? = when (effect) {
        is MoveToZoneEffect -> (effect.target as? EffectTarget.BoundVariable)?.name
        is DrawCardsEffect -> (effect.target as? EffectTarget.BoundVariable)?.name
        else -> null
    }

    "two sentences on one line are one composite" {
        fragment("Draw a card. You gain 2 life.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = Effects.Composite(listOf(Effects.DrawCards(1), Effects.GainLife(2)))
            )
        )
        roundTrips("Draw a card. You gain 2 life.")
        roundTrips("Scry 2. Draw a card. You gain 2 life.")
    }

    // The requirement belongs to the clause that introduces the referent, and the later clause reads
    // the slot without declaring one.
    "a target is declared at its first mention and referred to afterwards" {
        fragment("Target creature gets +1/+3 until end of turn. Untap that creature.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = Effects.Composite(
                    listOf(
                        Effects.ModifyStats(1, 3, Targets.bound()),
                        Effects.Untap(Targets.bound()),
                    )
                ),
                targetRequirements = listOf(Targets.permanent(GameObjectFilter.Creature)),
            )
        )
        roundTrips("Target creature gets +1/+3 until end of turn. Untap that creature.")
    }

    // The bug the differential found: the same four words mean the source in one position and the
    // target in the other, and reading the wrong one round-trips perfectly.
    "\"it\" is the source in a first clause and the target in a later one" {
        fragment("It gets +2/+0 until end of turn.") shouldBe CardFragment(
            script = CardScript(spellEffect = Effects.ModifyStats(2, 0, EffectTarget.Self))
        )
        fragment("Untap target creature. It gets +2/+4 until end of turn.") shouldBe CardFragment(
            script = CardScript(
                spellEffect = Effects.Composite(
                    listOf(
                        Effects.Untap(Targets.bound()),
                        Effects.ModifyStats(2, 4, Targets.bound()),
                    )
                ),
                targetRequirements = listOf(Targets.permanent(GameObjectFilter.Creature)),
            )
        )
        // The source form has two printed spellings — the card's own noun and the pronoun — and the
        // noun is canonical, so the pronoun comes back normalized. The *target* form has only the
        // pronoun, and round-trips.
        // `printLine` prints the abstracted token; restoring the card's own noun is
        // `NormalizedFace.restore`'s job, one layer out.
        Grammar.abilityLine.printLine(fragment("It gets +2/+0 until end of turn.")) shouldBe
            "~ gets +2/+0 until end of turn."
        roundTrips("Untap target creature. It gets +2/+4 until end of turn.")
    }

    // The joins denote the same composite the full stop does, so they parse and never print: the
    // model has no room for the conjunction and something has to be canonical.
    "the joins are alternate spellings of the full stop" {
        fragment("Draw a card and you gain 2 life.") shouldBe fragment("Draw a card. You gain 2 life.")
        fragment("Scry 2, then draw a card.") shouldBe fragment("Scry 2. Draw a card.")

        Grammar.abilityLine.printLine(fragment("Draw a card and you gain 2 life.")) shouldBe
            "Draw a card. You gain 2 life."
        Grammar.abilityLine.printLine(fragment("Scry 2, then draw a card.")) shouldBe
            "Scry 2. Draw a card."
    }

    // Mixing the separators has to fold to the same flat composite; a run with one separator could
    // not read the line, and a join rule that was itself a clause would have nested it.
    "one line can mix the joins" {
        fragment("Scry 2, then draw two cards. You lose 2 life.") shouldBe
            fragment("Scry 2. Draw two cards. You lose 2 life.")
    }

    // Two declared targets are numbered by the position their clause introduces them in, and the
    // first one keeps the bare name so a single-target line folds through unchanged.
    "two clauses that each declare a target are numbered by position" {
        val line = fragment("Destroy target land. Draw a card. Target player draws a card.").script
        line.targetRequirements.map { it.id } shouldBe listOf(Targets.slot(0), Targets.slot(1))
        val effects = (line.spellEffect as CompositeEffect).effects
        effects.map { referencedSlot(it) } shouldBe listOf(Targets.slot(0), null, Targets.slot(1))
        roundTrips("Destroy target land. Draw a card. Target player draws a card.")
        roundTrips("Destroy target land. ~ deals 13 damage to target creature.")
        roundTrips("Counter target spell. Return target permanent to its owner's hand.")
    }

    // Fail-closed, and now the only case that is: a pronoun clause beside a second declared target
    // would mean the most recent mention in English and the first slot in this grammar, and nothing
    // in the printed line chooses between them.
    "a pronoun clause beside a second target declines" {
        Grammar.abilityLine
            .parseLine("Destroy target land. ~ deals 13 damage to target creature. Untap that creature.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // A continuation cannot start a line: the thing it names has not been introduced.
    "a dangling anaphor is not a line" {
        Grammar.abilityLine.parseLine("Untap that creature.").shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // The wrappers are clauses, so a trigger and an activated ability get sequences for free.
    "a sequence is the same clause wherever it lands" {
        roundTrips("When ~ enters, draw a card. You gain 2 life.")
        roundTrips("{T}: Draw a card. You gain 2 life.")
        roundTrips("You may draw a card.")
    }
})
