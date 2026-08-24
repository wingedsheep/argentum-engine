package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The count a **cost** announces — "sacrifice any number of creatures", "one or more".
 *
 * The properties worth holding onto are the ones that decide whether this stays a family or decays
 * into a rule per printed sentence. The verb picks the frame (a sacrifice names no controller, a tap
 * says "untapped"); the count is one slot the frames share; the measure clause is a layer whose
 * *absent* row is a real row; and the exclusion is spelled in front of the noun or behind the
 * controller clause depending on the verb, which is why it belongs to the frame.
 *
 * The last two tests are the boundary: three variable-count costs the corpus prints that
 * `CostAtom.VariablePermanents` cannot hold, each of which must decline rather than land in the
 * nearest field. See [VariableCosts] for what each one would need.
 */
class VariableCostsTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    fun declines(line: String) {
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    "the two printed counts are one slot, and the noun beside them does not move" {
        roundTrips("Sacrifice any number of creatures: Draw a card.")
        roundTrips("Sacrifice one or more creatures: Draw a card.")
    }

    // The frame is the verb's business: a sacrifice can only ever be of what you control
    // (CR 701.17a), a tap can only be of an untapped permanent (CR 701.26a), and an exile says so.
    "each verb spells its own frame" {
        roundTrips("Sacrifice any number of artifacts: Draw a card.")
        roundTrips("Exile any number of artifacts you control: Draw a card.")
        roundTrips("Tap any number of untapped creatures you control: Draw a card.")
    }

    // Same field, two positions. A shared "other" layer would have printed one of these wrong.
    "the exclusion goes in front of the noun for an exile and behind the clause for a tap" {
        roundTrips("Exile one or more other artifacts you control: Draw a card.")
        roundTrips("Tap any number of untapped creatures you control other than ~: Draw a card.")
    }

    "a measure clause carries its own floor, and the bare row is the one with none" {
        roundTrips("Sacrifice any number of creatures: Draw a card.")
        roundTrips("Tap any number of untapped creatures you control with total power 10 or greater: Draw a card.")
        roundTrips("Exile one or more other artifacts you control with total mana value X: Draw a card.")
    }

    // Mossbridge Troll, the card that prints every axis at once.
    "the whole product reads in one sentence" {
        roundTrips(
            "Tap any number of untapped creatures you control other than ~ with total power 10 or greater: " +
                "~ gets +20/+20 until end of turn."
        )
    }

    // The vocabulary is `CostAtom`'s, so a row written here has to reach all three of its contexts.
    // This is the additional-cost one; the payable one is below.
    "the family reaches a spell's additional cost, in the lower case that position prints" {
        roundTrips("As an additional cost to cast this spell, sacrifice any number of creatures.")
        roundTrips("As an additional cost to cast this spell, tap any number of untapped creatures you control.")
    }

    // Phyrexian Dreadnought — `PayCost` is the third context, and it takes the family whole.
    "the family reaches a payable cost" {
        roundTrips("When ~ enters, sacrifice it unless you sacrifice any number of creatures with total power 12 or greater.")
    }

    // "Sacrifice any number of permanents you control" is printed once against the bare form's many,
    // and CR 701.17a makes the clause redundant — so it is the same rule with a second surface,
    // parsed and never printed.
    "the redundant controller clause parses and prints back as the bare form" {
        Grammar.abilityLine.printLine(
            fragment("Sacrifice any number of permanents you control: Draw a card.")
        ) shouldBe "Sacrifice any number of permanents: Draw a card."
    }

    // An effect is not a cost: `SacrificeEffect.any` happens on resolution and announces no X, which
    // is why the sentence after it reads a collection rather than "X". Same English, two models.
    "the effect position has its own type, and reads the same words" {
        roundTrips("Sacrifice any number of creatures.")
        roundTrips("Sacrifice any number of other creatures.")
    }

    // `CostAtom.RemoveCounters(count = XValue, self = true)` is the *same* value "Remove X …"
    // spells, so the batteries' wording is an alternate surface of that one rule rather than a
    // second rule that could print it.
    "a chosen number of counters is the X form's second spelling, and prints back as X" {
        roundTrips("{T}, Remove X charge counters from ~: Draw a card.")
        Grammar.abilityLine.printLine(
            fragment("{T}, Remove any number of charge counters from ~: Draw a card.")
        ) shouldBe "{T}, Remove X charge counters from ~: Draw a card."
    }

    // `CostAtom.ExileFrom.count` is an Int and `CollectEvidence` carries no filter, so a variable
    // count from a graveyard has nowhere to land. Reading it as the battlefield type would be a
    // different zone with the same words.
    "a variable-count exile from a graveyard declines" {
        declines("{T}, Exile any number of cards from your graveyard: Draw a card.")
    }

    // `CostAtom.Discard.count` is an Int. Same shape, no field.
    "a variable-count discard declines" {
        declines("Discard any number of cards: Draw a card.")
    }

    "every variable-cost rule prints what it parses" {
        listOf(
            "Sacrifice any number of creatures: Draw a card.",
            "Sacrifice one or more creatures: Draw a card.",
            "Sacrifice any number of other creatures: Draw a card.",
            "Sacrifice one or more other artifacts: Draw a card.",
            "Sacrifice any number of creatures with total power 12 or greater: Draw a card.",
            "Exile any number of artifacts you control: Draw a card.",
            "Exile one or more other artifacts you control: Draw a card.",
            "Exile one or more other artifacts you control with total mana value X: Draw a card.",
            "Tap any number of untapped creatures you control: Draw a card.",
            "Tap one or more untapped creatures you control: Draw a card.",
            "Tap any number of untapped creatures you control other than ~: Draw a card.",
            "Tap any number of untapped creatures you control with total power 10 or greater: Draw a card.",
        ).forEach { line -> Grammar.abilityLine.printLine(fragment(line)) shouldBe line }
    }
})
