package com.wingedsheep.gameserver.deck

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.gameserver.protocol.DeckRequestConverter
import com.wingedsheep.sdk.core.DeckFormat
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardEntry
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.CompanionCondition
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * The deck validator's companion rules (CR 702.139, 103.2b, 903.11a), in constructed and in
 * Commander.
 */
class DeckValidatorCompanionTest : FunSpec({

    fun legend(name: String, cost: String, condition: CompanionCondition? = null) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse(cost),
        subtypes = setOf(Subtype("Cat")),
        power = 3,
        toughness = 2,
        supertypes = setOf(Supertype.LEGENDARY),
        oracleText = if (condition != null) "Companion — Each card is odd. (Reminder.)" else "",
    ).copy(keywordAbilities = listOfNotNull(condition?.let { KeywordAbility.companion(it) }))

    fun creature(name: String, cost: String) = CardDefinition.creature(
        name = name, manaCost = ManaCost.parse(cost), subtypes = setOf(Subtype("Bear")), power = 2, toughness = 2,
    )

    val oddOnly = CompanionCondition.EachCard(GameObjectFilter.Land or GameObjectFilter.Any.manaValueIsOdd())
    val registry = CardRegistry().apply {
        register(CardDefinition.basicLand("Forest", Subtype("Forest")))
        register(CardDefinition.basicLand("Mountain", Subtype("Mountain")))
        register(legend("Odd Green Companion", "{2}{G}", oddOnly))
        register(legend("Odd Red Companion", "{2}{R}", oddOnly))
        register(legend("Plain Green Legend", "{1}{G}"))
        register(legend("Odd Green Commander", "{G}"))
        register(creature("Odd Bear", "{G}"))
        register(creature("Even Bear", "{1}{G}"))
    }
    val validator = DeckValidator(registry)

    fun codes(deck: Deck, format: DeckFormat? = null) = validator.validate(deck, format).errors.map { it.code }

    fun constructed(vararg extra: String, companion: String = "Odd Green Companion", sideboard: List<String> = listOf(companion)) =
        Deck(
            cards = List(30) { "Forest" } + List(4) { "Odd Bear" } + List(26) { "Mountain" } + extra,
            sideboard = sideboard.map(::CardEntry),
            companion = companion,
        )

    test("a deck that meets the condition may reveal its sideboard companion") {
        validator.validate(constructed()).errors.shouldBeEmpty()
    }

    test("the condition reads the starting deck: one even card breaks it") {
        val errors = validator.validate(constructed("Even Bear")).errors
        errors.map { it.code } shouldContain "COMPANION_CONDITION_NOT_MET"
        errors.single { it.code == "COMPANION_CONDITION_NOT_MET" }.cardName shouldBe "Odd Green Companion"
    }

    test("a card without a companion ability can't be the companion") {
        codes(constructed(companion = "Plain Green Legend")) shouldContain "INVALID_COMPANION"
    }

    test("the companion must be on the sideboard — the converter puts it there when missing") {
        codes(constructed(sideboard = emptyList())) shouldContain "COMPANION_NOT_IN_SIDEBOARD"
        val converted = DeckRequestConverter.toDeck(
            deckList = mapOf("Forest" to 30, "Odd Bear" to 4, "Mountain" to 26),
            companion = "Odd Green Companion",
        )
        converted.sideboard.map { it.name } shouldBe listOf("Odd Green Companion")
        validator.validate(converted).errors.shouldBeEmpty()
    }

    test("the companion counts toward the copy limit with the deck's copies (CR 100.4a)") {
        val deck = constructed("Odd Green Companion", "Odd Green Companion", "Odd Green Companion", "Odd Green Companion")
        codes(deck) shouldContain "TOO_MANY_COPIES"
        codes(constructed("Odd Green Companion", "Odd Green Companion", "Odd Green Companion")) shouldNotContain "TOO_MANY_COPIES"
    }

    fun commanderDeck(commander: String, companion: String, vararg extra: String) = Deck(
        cards = List(99 - extra.size) { "Forest" } + extra,
        commander = commander,
        sideboard = listOf(CardEntry(companion)),
        companion = companion,
    )

    test("Commander: the starting deck includes the commander (CR 702.139b)") {
        validator.validate(commanderDeck("Odd Green Commander", "Odd Green Companion"), DeckFormat.COMMANDER)
            .errors.shouldBeEmpty()
        // Plain Green Legend costs {1}{G} — even — so as the commander it breaks the condition.
        codes(commanderDeck("Plain Green Legend", "Odd Green Companion"), DeckFormat.COMMANDER) shouldContain
            "COMPANION_CONDITION_NOT_MET"
    }

    test("Commander: the companion fits the commander's colour identity (CR 903.11a)") {
        codes(commanderDeck("Odd Green Commander", "Odd Red Companion"), DeckFormat.COMMANDER) shouldContain
            "COMPANION_COLOR_IDENTITY"
    }

    test("Commander: the companion can't share a name with a card in the starting deck (CR 903.11a)") {
        codes(commanderDeck("Odd Green Commander", "Odd Green Companion", "Odd Green Companion"), DeckFormat.COMMANDER) shouldContain
            "COMPANION_DUPLICATE_NAME"
    }

    test("Commander: the companion isn't one of the 100") {
        codes(commanderDeck("Odd Green Commander", "Odd Green Companion"), DeckFormat.COMMANDER) shouldNotContain "TOO_MANY_CARDS"
    }
})
