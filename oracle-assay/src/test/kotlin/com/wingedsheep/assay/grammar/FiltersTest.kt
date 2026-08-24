package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.parseText
import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The noun-phrase cascade: type nouns, the colour and quality layers around them, the controller
 * suffix, and the two grammatical numbers the whole thing is instantiated in.
 *
 * The properties worth holding are that each layer owns exactly one field, that the layers compose
 * in the order English writes them, and that printing is decided by the model rather than by the
 * alternation's order — which is what the round trips below check one layer at a time.
 */
class FiltersTest : StringSpec({

    fun read(phrase: com.wingedsheep.assay.syntax.Phrase<GameObjectFilter>, text: String): GameObjectFilter =
        phrase.parseText(text).shouldBeInstanceOf<ParseOutcome.Accepted<GameObjectFilter>>().value

    fun roundTrips(phrase: com.wingedsheep.assay.syntax.Phrase<GameObjectFilter>, text: String) {
        phrase.unparse(read(phrase, text)) shouldBe text
    }

    "a bare type noun is the whole filter" {
        read(Filters.filter, "creature") shouldBe GameObjectFilter.Creature
        roundTrips(Filters.filter, "creature")
        roundTrips(Filters.filter, "nonbasic land")
        roundTrips(Filters.filter, "artifact or enchantment")
    }

    // The colour layer owns the top of the predicate stack and delegates the rest inward, which is
    // what lets it sit in front of a type phrase that already carries a state predicate.
    "the colour layer wraps any type noun" {
        read(Filters.filter, "white creature") shouldBe GameObjectFilter.Creature.withColor(Color.WHITE)
        read(Filters.filter, "nonblack attacking creature") shouldBe
            GameObjectFilter.Creature.notColor(Color.BLACK).attacking()
        read(Filters.plural, "black and/or red creatures") shouldBe
            GameObjectFilter.Creature.withAnyColor(Color.BLACK, Color.RED)

        roundTrips(Filters.filter, "white creature")
        roundTrips(Filters.filter, "nonblack attacking creature")
        roundTrips(Filters.plural, "black and/or red creatures")
    }

    "the quality suffix owns the keyword and power predicates" {
        read(Filters.plural, "creatures with flying") shouldBe
            GameObjectFilter.Creature.withKeyword(Keyword.FLYING)
        read(Filters.plural, "creatures without flying") shouldBe
            GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING)
        read(Filters.plural, "creatures with power 2 or greater") shouldBe
            GameObjectFilter.Creature.powerAtLeast(2)

        roundTrips(Filters.plural, "creatures with flying")
        roundTrips(Filters.plural, "creatures without flying")
        roundTrips(Filters.plural, "creatures with power 2 or greater")
    }

    // Colour then controller then quality, which is both the printed order and — for the colour
    // half — the order the SDK's fluent builders append in, the reason "strip the top of the stack"
    // is well defined. The controller clause is not in that stack at all: it is its own field, so
    // the two clause layers commute in the model and only English decides which comes first.
    "the layers compose in the order English writes them" {
        read(Filters.plural, "black creatures you control") shouldBe
            GameObjectFilter.Creature.withColor(Color.BLACK).youControl()
        roundTrips(Filters.plural, "black creatures you control")
        roundTrips(Filters.filter, "creature an opponent controls")
    }

    // Oracle prints the controller clause in front of the quality clause 158 times to 5, and this
    // cascade used to have it the other way round — so it read the five and declined the rest.
    "the controller clause comes before the quality clause" {
        read(Filters.plural, "creatures you control with power 4 or greater") shouldBe
            GameObjectFilter.Creature.youControl().powerAtLeast(4)
        read(Filters.filter, "creature an opponent controls with mana value 3 or less") shouldBe
            GameObjectFilter.Creature.opponentControls().manaValueAtMost(3)

        roundTrips(Filters.plural, "creatures you control with power 4 or greater")
        roundTrips(Filters.filter, "creature an opponent controls with mana value 3 or less")
        roundTrips(Filters.plural, "creatures you control with flying")
    }

    // The fourteen cards that print the other order still read, and print back canonically — a
    // VARIANT, which is what an `alternate` is for. The alternate's inner phrase excludes the bare
    // noun, so "creature you control" keeps exactly one reading and the redundancy count stays 0.
    "the reversed order parses and prints back canonically" {
        read(Filters.filter, "creature with mana value 3 or less an opponent controls") shouldBe
            GameObjectFilter.Creature.opponentControls().manaValueAtMost(3)
        Filters.filter.unparse(read(Filters.filter, "creature with mana value 3 or less an opponent controls")) shouldBe
            "creature an opponent controls with mana value 3 or less"
        read(Filters.plural, "creatures with flying an opponent controls") shouldBe
            GameObjectFilter.Creature.opponentControls().withKeyword(Keyword.FLYING)
    }

    // "your opponents control" is a third `ControllerPredicate` this cascade has never spelled, in
    // either word order — a row nobody has written, not a casualty of the order above.
    "the plural-opponents controller clause is still unspelled" {
        Filters.plural.parseText("creatures your opponents control")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // Number is an axis rather than a second vocabulary, so every layer exists in both.
    "the plural cascade is the same cascade" {
        read(Filters.plural, "creatures") shouldBe GameObjectFilter.Creature
        roundTrips(Filters.plural, "creatures")
        roundTrips(Filters.plural, "artifact creatures")
        // The conjunction changes with the number, which is why the plural is a column in the table
        // rather than a rule derived from the singular.
        read(Filters.plural, "creatures and lands") shouldBe GameObjectFilter.CreatureOrLand
        roundTrips(Filters.plural, "creatures and lands")
    }

    // "Plains" is its own plural — the same trap Primitives.pluralSubtype exists for.
    "a basic land type is a type noun, and Plains is invariant" {
        read(Filters.filter, "Mountain") shouldBe GameObjectFilter.Land.withSubtype(Subtype("Mountain"))
        roundTrips(Filters.filter, "Mountain")
        roundTrips(Filters.plural, "Mountains")
        roundTrips(Filters.plural, "Plains")
    }

    // The article is a function of the noun's spelling, so both halves of both rules derive it and
    // the disagreeing one refuses in both directions.
    "the indefinite article agrees with the noun in both directions" {
        roundTrips(Filters.indefinite, "a creature")
        roundTrips(Filters.indefinite, "an artifact")
        roundTrips(Filters.indefinite, "an Island")
        roundTrips(Filters.indefinite, "a Forest")

        Filters.indefinite.parseText("an Forest").shouldBeInstanceOf<ParseOutcome.Declined>()
        Filters.indefinite.parseText("a artifact").shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // The head noun is a layer, and its place in the cascade is the property worth holding: the
    // modifiers English writes in front of a noun stay inside it and the clauses English writes
    // behind one stay outside it. Getting that backwards prints "creature with flying card".
    "the card noun splits the cascade where English splits it" {
        read(Filters.cardNoun, "creature card") shouldBe GameObjectFilter.Creature
        read(Filters.cardNoun, "black creature card") shouldBe
            GameObjectFilter.Creature.withColor(Color.BLACK)
        read(Filters.cardNoun, "creature card with flying") shouldBe
            GameObjectFilter.Creature.withKeyword(Keyword.FLYING)

        roundTrips(Filters.cardNoun, "creature card")
        roundTrips(Filters.cardNoun, "black creature card")
        roundTrips(Filters.cardNoun, "creature card with flying")
        roundTrips(Filters.cardNoun, "creature card with power 2 or greater")
    }

    // Oracle inflects only the head noun, so the type phrase in front of it stays singular in the
    // plural. "creatures cards" is what the noun-in-the-template shape used to be able to print.
    "only the head noun inflects in card position" {
        read(Filters.pluralCards, "creature cards") shouldBe GameObjectFilter.Creature
        roundTrips(Filters.pluralCards, "creature cards")
        roundTrips(Filters.pluralCards, "artifact creature cards")
        Filters.pluralCards.parseText("creatures cards").shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // The zone clause owns the controller field in card position — "from your graveyard" is what
    // says whose — so the layer that owns `controllerPredicate` is absent from this cascade.
    "card position carries no controller clause" {
        Filters.cardNoun.parseText("creature card you control").shouldBeInstanceOf<ParseOutcome.Declined>()
        Filters.cardNoun.unparse(GameObjectFilter.Creature.youControl()) shouldBe null
    }

    // A form nobody wrote down declines rather than being approximated — the point of enumerating
    // the type list at all.
    "a phrase the list does not spell declines" {
        Filters.filter.parseText("legendary creature").shouldBeInstanceOf<ParseOutcome.Declined>()
        Filters.plural.parseText("artifact or enchantments").shouldBeInstanceOf<ParseOutcome.Declined>()
    }
})
