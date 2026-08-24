package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.parseText
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The mana-value qualifier — three comparisons over three value shapes, and the word order that
 * follows from which shape a line prints.
 *
 * What is worth asserting here is not that the sentences parse but that the **table's cells stay
 * disjoint**: the postfix spellings belong to a numeral and to the letter `X`, the prefix spellings
 * belong to a clause, and neither can reach the other's model. A slot that could print both orders
 * would round-trip byte-perfectly on whichever it chose and leave the other unprintable, which is
 * the failure the [ManaValues] KDoc is written against.
 */
class ManaValuesTest : StringSpec({

    fun read(phrase: Phrase<GameObjectFilter>, text: String): GameObjectFilter =
        phrase.parseText(text).shouldBeInstanceOf<ParseOutcome.Accepted<GameObjectFilter>>().value

    fun roundTrips(phrase: Phrase<GameObjectFilter>, text: String) {
        phrase.unparse(read(phrase, text)) shouldBe text
    }

    fun cannotPrint(phrase: Phrase<GameObjectFilter>, filter: GameObjectFilter) {
        phrase.unparse(filter) shouldBe null
    }

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    // -----------------------------------------------------------------------------------------
    // A numeral: the comparison goes behind it, and equality is the empty comparison
    // -----------------------------------------------------------------------------------------

    "a numeral takes the comparison behind it" {
        read(Filters.filter, "creature with mana value 3") shouldBe
            GameObjectFilter.Creature.manaValue(3)
        read(Filters.filter, "creature with mana value 3 or less") shouldBe
            GameObjectFilter.Creature.manaValueAtMost(3)
        read(Filters.filter, "creature with mana value 3 or greater") shouldBe
            GameObjectFilter.Creature.manaValueAtLeast(3)

        roundTrips(Filters.filter, "creature with mana value 3")
        roundTrips(Filters.filter, "creature with mana value 3 or less")
        roundTrips(Filters.filter, "creature with mana value 3 or greater")
    }

    // The equality row is the bare numeral, so a *prefixed* "equal to 3" must not also be a spelling
    // of it — nothing would decide which of the two printed.
    "equality with a numeral has only the bare spelling" {
        Filters.filter.parseText("creature with mana value equal to 3")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // -----------------------------------------------------------------------------------------
    // The announced X: a row, because the SDK carries no number for it
    // -----------------------------------------------------------------------------------------

    "the letter X is its own row in each comparison the SDK has" {
        read(Filters.filter, "creature with mana value X") shouldBe
            GameObjectFilter.Creature.manaValueEqualsX()
        read(Filters.filter, "creature with mana value X or less") shouldBe
            GameObjectFilter.Creature.manaValueAtMostX()

        roundTrips(Filters.filter, "creature with mana value X")
        roundTrips(Filters.filter, "creature with mana value X or less")
    }

    // The one card that prints "mana value X or greater" has no `ManaValueAtLeastX` to be, so the
    // cell is empty in both directions rather than approximated by the numeral row.
    "X has no at-least row, because the SDK has no predicate for it" {
        Filters.filter.parseText("creature with mana value X or greater")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // -----------------------------------------------------------------------------------------
    // A clause: the comparison goes in front of it
    // -----------------------------------------------------------------------------------------

    "a counted clause takes the comparison in front of it" {
        val lands = DynamicAmount.AggregateBattlefield(
            player = Player.You,
            filter = GameObjectFilter.Land,
        )
        read(Filters.cardNoun, "card with mana value less than or equal to the number of lands you control")
            .shouldBe(GameObjectFilter.Any.manaValueAtMostDynamic(lands))
        read(Filters.cardNoun, "card with mana value equal to the number of lands you control")
            .shouldBe(GameObjectFilter.Any.manaValueEqualsDynamic(lands))

        roundTrips(Filters.cardNoun, "card with mana value less than or equal to the number of lands you control")
        roundTrips(Filters.cardNoun, "card with mana value equal to the number of lands you control")
    }

    // `Amounts.count` cannot print a `Fixed` amount or `XValue`, which is what keeps the clause rows
    // from being a second printed form of the two rows above. Asserted here rather than trusted,
    // because it is the property the whole table's determinism rests on.
    "the clause rows refuse the two amount shapes the other rows own" {
        cannotPrint(
            Filters.filter,
            GameObjectFilter.Creature.manaValueAtMostDynamic(DynamicAmount.Fixed(3)),
        )
        cannotPrint(
            Filters.filter,
            GameObjectFilter.Creature.manaValueAtMostDynamic(DynamicAmount.XValue),
        )
    }

    // -----------------------------------------------------------------------------------------
    // The layer's place in the cascade — behind the head noun, in both positions
    // -----------------------------------------------------------------------------------------

    "the qualifier attaches behind whichever head noun the position prints" {
        read(Filters.filter, "creature with mana value 3 or less") shouldBe
            GameObjectFilter.Creature.manaValueAtMost(3)
        read(Filters.cardNoun, "creature card with mana value 3 or less") shouldBe
            GameObjectFilter.Creature.manaValueAtMost(3)
        read(Filters.pluralCards, "creature cards with mana value 3 or less") shouldBe
            GameObjectFilter.Creature.manaValueAtMost(3)

        roundTrips(Filters.cardNoun, "creature card with mana value 3 or less")
        roundTrips(Filters.pluralCards, "creature cards with mana value 3 or less")
    }

    // The layers in front of the head noun still compose with it, which is the property that makes
    // the insertion point rather than the layer the interesting part of the change.
    "the modifiers in front of the noun still layer under the qualifier" {
        read(Filters.cardNoun, "green creature card with mana value 3 or less") shouldBe
            GameObjectFilter.Creature.withColor(com.wingedsheep.sdk.core.Color.GREEN).manaValueAtMost(3)
        read(Filters.cardNoun, "Rebel permanent card with mana value 3 or less") shouldBe
            GameObjectFilter.Permanent.withSubtype(com.wingedsheep.sdk.core.Subtype("Rebel")).manaValueAtMost(3)

        roundTrips(Filters.cardNoun, "green creature card with mana value 3 or less")
        roundTrips(Filters.cardNoun, "Rebel permanent card with mana value 3 or less")
    }

    // -----------------------------------------------------------------------------------------
    // The whole sentences the band was picked for
    // -----------------------------------------------------------------------------------------

    "the searches and returns that named this band read as whole lines" {
        listOf(
            "Search your library for a creature card with mana value 3 or less, put that card onto the battlefield, then shuffle.",
            "Search your library for an artifact card with mana value 6 or greater, reveal it, put it into your hand, then shuffle.",
            "Search your library for a Rebel permanent card with mana value 3 or less, put that card onto the battlefield, then shuffle.",
            "Search your library for a card with mana value less than or equal to the number of lands you control, reveal it, put it into your hand, then shuffle.",
            "Return target creature card with mana value 3 or less from your graveyard to the battlefield.",
            "Return target creature card with mana value 2 or less from your graveyard to your hand.",
            "Destroy target creature with mana value X.",
        ).forEach { line -> Grammar.abilityLine.printLine(fragment(line)) shouldBe line }
    }

    // The pronoun spelling of the search's move is [Library]'s pre-existing `alternate`, and this
    // band inherits it: the cards that print "put it onto the battlefield" read to the same script
    // and come back as a VARIANT. Asserted so a later change to the canonical is a visible one.
    "the pronoun form of the search is a variant, not a second reading" {
        val pronoun = "Search your library for a creature card with mana value 3 or less, " +
            "put it onto the battlefield, then shuffle."
        val canonical = "Search your library for a creature card with mana value 3 or less, " +
            "put that card onto the battlefield, then shuffle."
        fragment(pronoun) shouldBe fragment(canonical)
        Grammar.abilityLine.printLine(fragment(pronoun)) shouldBe canonical
    }

    // The bare noun now comes out of the same vocabulary as the qualified one, which is what let two
    // rules for "target card" / "discard a card" be deleted rather than guarded.
    "the unqualified card noun is a row of the same vocabulary" {
        read(Filters.cardNoun, "card") shouldBe GameObjectFilter.Any
        read(Filters.pluralCards, "cards") shouldBe GameObjectFilter.Any
        roundTrips(Filters.cardNoun, "card")
        roundTrips(Filters.pluralCards, "cards")

        Grammar.abilityLine.printLine(fragment("Return target card from your graveyard to your hand.")) shouldBe
            "Return target card from your graveyard to your hand."
    }

    // The article agrees with the first word of the whole noun phrase, which in card position is the
    // type phrase — and is "card" itself when there is no type phrase.
    "the article agrees with the card noun's own first word" {
        Filters.indefiniteCard.unparse(GameObjectFilter.Creature) shouldBe "a creature card"
        Filters.indefiniteCard.unparse(GameObjectFilter.Artifact.manaValueAtLeast(6)) shouldBe
            "an artifact card with mana value 6 or greater"
        Filters.indefiniteCard.unparse(GameObjectFilter.Any) shouldBe "a card"
    }

    "every mana-value rule prints what it parses" {
        listOf(
            "creature with mana value 3",
            "creature with mana value 3 or less",
            "creature with mana value 3 or greater",
            "creature with mana value X",
            "creature with mana value X or less",
            "creature card with mana value 1 or less",
            "artifact card with mana value 6 or greater",
            "permanent card with mana value 3 or less",
            "card with mana value equal to the number of cards in your graveyard",
            "card with mana value less than or equal to the number of creature cards in your graveyard",
        ).forEach { text ->
            val vocabulary = if (" card" in text) Filters.cardNoun else Filters.filter
            roundTrips(vocabulary, text)
        }
    }

    // A `deferred` slot resolves on first use, so a cycle between two grammar objects cannot leave
    // one of them reading a null out of the other's half-built state. Touching the count vocabulary
    // *first* is the order that would have failed without the indirection.
    "the count vocabulary and the noun phrase can be reached in either order" {
        Amounts.count.unparse(
            DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature)
        ) shouldBe "the number of creature cards in your graveyard"
        read(Filters.cardNoun, "creature card with mana value 3 or less")
            .cardPredicates
            .last() shouldBe CardPredicate.ManaValueAtMost(3)
    }
})
