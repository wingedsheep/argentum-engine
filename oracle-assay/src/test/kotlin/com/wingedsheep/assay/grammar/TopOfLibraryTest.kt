package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The top-of-library band — one gather/select/move vocabulary in three layers, and the sentences
 * that lift it.
 *
 * The assertions that earn their place here are the ones about a *field the printer could get wrong*
 * rather than about a sentence parsing. Three of them are the band's whole argument:
 *
 * - the remainder's **order** is carried, so "in a random order" and "in any order" are different
 *   values rather than two spellings of one — the field five hand-written cards were dropping;
 * - "the other" and "the rest" take **disjoint** halves of one value space, so neither the
 *   alternation's order nor its membership can decide which prints;
 * - the impulse anaphor **agrees with the count**, in both directions, and refuses when it does not.
 */
class TopOfLibraryTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun effect(line: String) = fragment(line).script.spellEffect

    fun steps(line: String): List<*> = (effect(line) as CompositeEffect).effects

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    /** An alternate spelling: it parses to the same model and prints as [canonical]. */
    fun variantOf(line: String, canonical: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe canonical
        effect(line) shouldBe effect(canonical)
    }

    fun declines(line: String) {
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // ---------------------------------------------------------------------------------------
    // Layer 1 — the count, and the noun that agrees with it
    // ---------------------------------------------------------------------------------------

    "the count layer carries the noun, so one and many are one slot" {
        (steps("Exile the top card of your library.")[0] as GatherCardsEffect).source shouldBe
            CardSource.TopOfLibrary(DynamicAmount.Fixed(1))
        (steps("Exile the top three cards of your library.")[0] as GatherCardsEffect).source shouldBe
            CardSource.TopOfLibrary(DynamicAmount.Fixed(3))
        (steps("Exile the top X cards of your library.")[0] as GatherCardsEffect).source shouldBe
            CardSource.TopOfLibrary(DynamicAmount.XValue)
        roundTrips("Exile the top card of your library.")
        roundTrips("Exile the top three cards of your library.")
        roundTrips("Exile the top X cards of your library.")
    }

    // The number word and the noun cannot disagree, because they are the same phrase.
    "a count of one is never spelled as a word" {
        declines("Exile the top one cards of your library.")
        declines("Exile the top one card of your library.")
    }

    // ---------------------------------------------------------------------------------------
    // Layer 2/3 — the destination, and the order layer over it
    // ---------------------------------------------------------------------------------------

    "the remainder's order is a value the sentence carries, not a flourish" {
        fun restMove(line: String) =
            steps(line).filterIsInstance<MoveCollectionEffect>().single { it.from == "rest" }

        val random = "Look at the top four cards of your library. Put one of them into your hand and " +
            "the rest on the bottom of your library in a random order."
        val any = "Look at the top four cards of your library. Put one of them into your hand and " +
            "the rest on the bottom of your library in any order."
        val bare = "Look at the top four cards of your library. Put one of them into your hand and " +
            "the rest on the bottom of your library."

        restMove(random).order shouldBe CardOrder.Random
        restMove(any).order shouldBe CardOrder.ControllerChooses
        restMove(bare).order shouldBe CardOrder.Preserve
        // …and the three are genuinely different cards, which is the point: five goldens had lost
        // exactly this field.
        effect(random) shouldNotBe effect(any)
        listOf(random, any, bare).forEach(::roundTrips)
    }

    "the destination layer spells whole prepositional phrases" {
        fun keepDestination(place: String) =
            steps("Look at the top four cards of your library. Put one of them $place and the rest into your graveyard.")
                .filterIsInstance<MoveCollectionEffect>().single { it.from == "kept" }.destination

        keepDestination("into your hand") shouldBe CardDestination.ToZone(Zone.HAND)
        keepDestination("onto the battlefield") shouldBe CardDestination.ToZone(Zone.BATTLEFIELD)
        keepDestination("on the bottom of your library") shouldBe
            CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom)
        keepDestination("on top of your library") shouldBe
            CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top)
    }

    "the library is elided once the sentence has already named it" {
        variantOf(
            "Look at the top four cards of your library. Put one of them into your hand and the rest " +
                "on the bottom in a random order.",
            "Look at the top four cards of your library. Put one of them into your hand and the rest " +
                "on the bottom of your library in a random order.",
        )
    }

    // Witness the Future writes the recipe as one sentence rather than two. The gather and the
    // selection that consumes it cannot be split into two clauses, so the join is a spelling of
    // this rule and not a member of [Steps]' clause run.
    "the two halves can be joined with a comma instead of a full stop" {
        variantOf(
            "You look at the top four cards of your library, then put one of those cards into your " +
                "hand and the rest on the bottom of your library in a random order.",
            "Look at the top four cards of your library. Put one of them into your hand and the rest " +
                "on the bottom of your library in a random order.",
        )
    }

    "the pile can be named as them or as those cards" {
        variantOf(
            "Look at the top five cards of your library. Put one of those cards into your hand and " +
                "the rest into your graveyard.",
            "Look at the top five cards of your library. Put one of them into your hand and " +
                "the rest into your graveyard.",
        )
    }

    // ---------------------------------------------------------------------------------------
    // "the rest" against "the other" — disjoint on the model
    // ---------------------------------------------------------------------------------------

    // Tower Geist. English writes "the other" when the remainder is exactly one card, and the
    // corpus does it 16 times to 0 — so this is a fact about the numbers, not a spelling to allow.
    "a remainder of exactly one is the other, and nothing else can print it" {
        roundTrips(
            "Look at the top two cards of your library. Put one of them into your hand and the other " +
                "into your graveyard.",
        )
        declines(
            "Look at the top two cards of your library. Put one of them into your hand and the rest " +
                "into your graveyard.",
        )
        // …and the general rule owns every other remainder, including the one it cannot compute.
        roundTrips(
            "Look at the top four cards of your library. Put one of them into your hand and the rest " +
                "into your graveyard.",
        )
        roundTrips(
            "Look at the top X cards of your library. Put two of them into your hand and the rest " +
                "into your graveyard.",
        )
    }

    // ---------------------------------------------------------------------------------------
    // The filtered dig
    // ---------------------------------------------------------------------------------------

    "the filtered dig keeps up to one matching card, and reveals it" {
        val line = "Look at the top three cards of your library. You may reveal a creature or land " +
            "card from among them and put it into your hand. Put the rest on the bottom of your " +
            "library in any order."
        val select = steps(line).filterIsInstance<SelectFromCollectionEffect>().single()
        select.selection shouldBe SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1))
        select.showAllCards shouldBe true
        steps(line).filterIsInstance<MoveCollectionEffect>().single { it.from == "kept" }
            .revealed shouldBe true
        roundTrips(line)
    }

    // ---------------------------------------------------------------------------------------
    // The take sentence — the verb, the quantifier, and where the taken cards go
    // ---------------------------------------------------------------------------------------

    "the quantifier and the option word are one field, and SelectionMode is it" {
        fun selection(take: String) = steps(
            "Look at the top five cards of your library. $take from among them into your hand. " +
                "Put the rest into your graveyard.",
        ).filterIsInstance<SelectFromCollectionEffect>().single().selection

        selection("You may put a creature card") shouldBe SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1))
        selection("You may put any number of creature cards") shouldBe SelectionMode.ChooseAnyNumber
        selection("Put up to two creature cards") shouldBe SelectionMode.ChooseUpTo(DynamicAmount.Fixed(2))
        selection("Put a creature card") shouldBe SelectionMode.ChooseExactly(DynamicAmount.Fixed(1))
    }

    // "You may put a…" is written as two sentences 38 times and joined 0 times; a bare "Put a…" is
    // joined 11 times to 2. So the join is not free — it is decided by the SelectionMode, the same
    // field the two rules split on, exactly as the impulse durations decide their word order.
    "which join is canonical is decided by the selection mode" {
        roundTrips(
            "Look at the top five cards of your library. You may put a creature card from among them " +
                "into your hand. Put the rest into your graveyard.",
        )
        roundTrips(
            "Look at the top five cards of your library. Put a creature card from among them into " +
                "your hand and the rest into your graveyard.",
        )
        // …and each rule still *parses* the other's order, so no printing of the sentence declines.
        variantOf(
            "Look at the top five cards of your library. You may put a creature card from among them " +
                "into your hand and the rest into your graveyard.",
            "Look at the top five cards of your library. You may put a creature card from among them " +
                "into your hand. Put the rest into your graveyard.",
        )
        variantOf(
            "Look at the top five cards of your library. Put a creature card from among them into " +
                "your hand. Put the rest into your graveyard.",
            "Look at the top five cards of your library. Put a creature card from among them into " +
                "your hand and the rest into your graveyard.",
        )
    }

    "the verb says whether everyone sees the pile, and it is one flag on the gather" {
        fun gather(verb: String) = steps(
            "$verb the top five cards of your library. You may put a creature card from among them " +
                "into your hand. Put the rest into your graveyard.",
        ).filterIsInstance<GatherCardsEffect>().single()

        gather("Look at").revealed shouldBe false
        gather("Reveal").revealed shouldBe true
        roundTrips(
            "Reveal the top five cards of your library. You may put a creature card from among them " +
                "into your hand. Put the rest into your graveyard.",
        )
    }

    // The verb is a layer rather than a copy of every sentence, so the counted keep gets it too —
    // Memories Returning and Chrome Courier print "Reveal the top…" over the same recipe.
    "the verb layer reaches the counted keep as well" {
        steps(
            "Reveal the top four cards of your library. Put one of them into your hand and the rest " +
                "into your graveyard.",
        ).filterIsInstance<GatherCardsEffect>().single().revealed shouldBe true
        roundTrips(
            "Reveal the top four cards of your library. Put one of them into your hand and the rest " +
                "into your graveyard.",
        )
        roundTrips(
            "Reveal the top two cards of your library. Put one of them into your hand and the other " +
                "into your graveyard.",
        )
    }

    "the taken cards can enter tapped, and tapped and attacking" {
        fun keepDestination(place: String) = steps(
            "Look at the top five cards of your library. You may put a land card from among them " +
                "$place. Put the rest into your graveyard.",
        ).filterIsInstance<MoveCollectionEffect>().single { it.from == "kept" }.destination

        keepDestination("onto the battlefield tapped") shouldBe
            CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped)
        keepDestination("onto the battlefield tapped and attacking") shouldBe
            CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.TappedAndAttacking)
        roundTrips(
            "Look at the top five cards of your library. You may put a land card from among them " +
                "onto the battlefield tapped. Put the rest on the bottom of your library in a random order.",
        )
    }

    // "You may **reveal** a creature card … and put **it** into your hand" turns the kept card face
    // up as it moves; "You may **put** a creature card … into your hand" does not. One boolean, and
    // it is what keeps the two sentences from being able to print each other.
    "taking a card is not revealing it, and the two sentences stay apart on that one field" {
        fun keptMove(line: String) =
            steps(line).filterIsInstance<MoveCollectionEffect>().single { it.from == "kept" }

        val taken = "Look at the top three cards of your library. You may put a creature card from " +
            "among them into your hand. Put the rest into your graveyard."
        val revealed = "Look at the top three cards of your library. You may reveal a creature card " +
            "from among them and put it into your hand. Put the rest into your graveyard."

        keptMove(taken).revealed shouldBe false
        keptMove(revealed).revealed shouldBe true
        effect(taken) shouldNotBe effect(revealed)
        roundTrips(taken)
        roundTrips(revealed)
    }

    // The facade needs a decision label and no printed word supplies one, so both directions
    // rebuild it out of the layers the rule just read. A prompt invented anywhere else would make
    // two parses of one line unequal.
    "the prompt is the printed sentence, derived from the same layers" {
        fun prompt(line: String) =
            steps(line).filterIsInstance<SelectFromCollectionEffect>().single().prompt

        prompt(
            "Look at the top five cards of your library. You may put a land card from among them " +
                "onto the battlefield tapped. Put the rest into your graveyard.",
        ) shouldBe "You may put a land card from among them onto the battlefield tapped"
        prompt(
            "Reveal the top four cards of your library. Put up to two creature cards from among them " +
                "into your hand and the rest into your graveyard.",
        ) shouldBe "Put up to two creature cards from among them into your hand"
    }

    // "Put a card from among them into your hand" and "Put one of them into your hand" are two
    // different scripts — the first names a filter and a decision, the second is the counted keep —
    // so neither rule can print the other's model and the gate's redundancy count stays at zero.
    "the unfiltered keep is still the counted sentence, not a take of a bare card" {
        val counted = "Look at the top four cards of your library. Put one of them into your hand " +
            "and the rest into your graveyard."
        val taken = "Look at the top four cards of your library. Put a card from among them into " +
            "your hand and the rest into your graveyard."
        effect(counted) shouldNotBe effect(taken)
        roundTrips(counted)
        roundTrips(taken)
    }

    // ---------------------------------------------------------------------------------------
    // Impulse — the anaphor agrees with the count
    // ---------------------------------------------------------------------------------------

    "impulse grants permission until the duration the sentence names" {
        fun expiry(line: String) =
            steps(line).filterIsInstance<GrantMayPlayFromExileEffect>().single().expiry

        expiry("Exile the top card of your library. You may play that card this turn.") shouldBe
            MayPlayExpiry.EndOfTurn
        expiry(
            "Exile the top card of your library. Until the end of your next turn, you may play that card.",
        ) shouldBe MayPlayExpiry.UntilEndOfNextTurn
        expiry(
            "Exile the top two cards of your library. Until your next end step, you may play those cards.",
        ) shouldBe MayPlayExpiry.UntilNextEndStep
        expiry(
            "Exile the top card of your library. You may play that card for as long as it remains exiled.",
        ) shouldBe MayPlayExpiry.Permanent
    }

    // The count already decides the number. A sentence that disagrees with itself denotes nothing.
    "the anaphor must agree with the count it refers back to" {
        roundTrips("Exile the top card of your library. You may play that card this turn.")
        roundTrips("Exile the top two cards of your library. You may play those cards this turn.")
        declines("Exile the top card of your library. You may play those cards this turn.")
        declines("Exile the top two cards of your library. You may play that card this turn.")
    }

    "the pronoun spellings parse and the noun ones print" {
        variantOf(
            "Exile the top card of your library. You may play it this turn.",
            "Exile the top card of your library. You may play that card this turn.",
        )
        variantOf(
            "Exile the top two cards of your library. You may play them this turn.",
            "Exile the top two cards of your library. You may play those cards this turn.",
        )
    }

    // Which order is canonical flips with the duration, because the corpus flips: "this turn"
    // trails 115 lines to 40, and the two cross-turn durations front 59:16 and 8:5.
    "each duration prints in the order the corpus prints it, and parses in both" {
        variantOf(
            "Exile the top card of your library. Until end of turn, you may play that card.",
            "Exile the top card of your library. You may play that card this turn.",
        )
        variantOf(
            "Exile the top card of your library. You may play that card until the end of your next turn.",
            "Exile the top card of your library. Until the end of your next turn, you may play that card.",
        )
        variantOf(
            "Exile the top card of your library. You may play that card until your next end step.",
            "Exile the top card of your library. Until your next end step, you may play that card.",
        )
    }

    // ---------------------------------------------------------------------------------------
    // The one gather this family must not claim
    // ---------------------------------------------------------------------------------------

    // A mill is the same printed shape and a different value: `TopOfLibrary.isMill` makes CR
    // 701.17's "mill that many plus four instead" apply at the count site. Reading the flag off
    // rather than ignoring it is what stops a mill printing as "exile the top two cards".
    "a mill is not an exile from the top, even where the pipeline shape agrees" {
        val milled = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(2), isMill = true),
                    storeAs = "exiled_top",
                ),
                MoveCollectionEffect(from = "exiled_top", destination = CardDestination.ToZone(Zone.EXILE)),
            ),
        )
        effect("Exile the top two cards of your library.") shouldNotBe milled
    }
})
