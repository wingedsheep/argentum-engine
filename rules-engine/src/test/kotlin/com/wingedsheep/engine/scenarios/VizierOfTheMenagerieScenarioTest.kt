package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.akh.cards.VizierOfTheMenagerie
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Vizier of the Menagerie (canonical AKH #192, reprinted FDN #649).
 *
 * You may look at the top card of your library any time.
 * You may cast creature spells from the top of your library.
 * You can spend mana of any type to cast creature spells.
 *
 * These cover the new [com.wingedsheep.sdk.scripting.SpendAnyManaTypeForSpells] static. Its
 * load-bearing property is that it is **zone-agnostic** and **type-filtered**: per the rulings it
 * relaxes the colored pips of *any* creature spell you cast — not just one cast off the top of your
 * library — and leaves noncreature spells alone. Casting is exercised with a pool that has *no*
 * green mana at all, so a successful cast can only mean the relaxation applied.
 */
class VizierOfTheMenagerieScenarioTest : FunSpec({

    // A green creature that plainly cannot be cast off-color without the Vizier.
    val greenBear = CardDefinition.creature(
        name = "Test Green Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = emptySet(),
        power = 2,
        toughness = 2,
    )

    val greenSorcery = CardDefinition(
        name = "Test Green Sorcery",
        manaCost = ManaCost.parse("{1}{G}"),
        typeLine = com.wingedsheep.sdk.core.TypeLine.parse("Sorcery"),
        oracleText = "Do nothing.",
        script = com.wingedsheep.sdk.model.CardScript.spell(
            effect = com.wingedsheep.sdk.dsl.Effects.GainLife(1)
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(VizierOfTheMenagerie, greenBear, greenSorcery))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        return driver
    }

    test("a green creature spell in hand is castable with only blue mana") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(you, "Vizier of the Menagerie")
        val bear = driver.putCardInHand(you, "Test Green Bear")
        driver.giveMana(you, Color.BLUE, 2)

        driver.legalActions(you).any {
            it.actionType == "CastSpell" && it.description.contains("Test Green Bear")
        } shouldBe true

        driver.castSpell(you, bear).error shouldBe null
        driver.bothPass()

        driver.findPermanent(you, "Test Green Bear") shouldBe
            driver.getCreatures(you).single { driver.getCardName(it) == "Test Green Bear" }
    }

    test("without the Vizier the same off-color cast is not offered") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardInHand(you, "Test Green Bear")
        driver.giveMana(you, Color.BLUE, 2)

        driver.legalActions(you).any {
            it.actionType == "CastSpell" && it.description.contains("Test Green Bear")
        } shouldBe false
    }

    test("the relaxation is creature-only — a green sorcery stays uncastable off-color") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(you, "Vizier of the Menagerie")
        driver.putCardInHand(you, "Test Green Sorcery")
        driver.giveMana(you, Color.BLUE, 2)

        driver.legalActions(you).any {
            it.actionType == "CastSpell" && it.description.contains("Test Green Sorcery")
        } shouldBe false
    }

    test("the displayed mana cost stays the printed cost, not the relaxed one") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(you, "Vizier of the Menagerie")
        driver.putCardInHand(you, "Test Green Bear")
        driver.giveMana(you, Color.BLUE, 2)

        val cast = driver.legalActions(you).single {
            it.actionType == "CastSpell" && it.description.contains("Test Green Bear")
        }
        cast.manaCostString shouldBe "{1}{G}"
    }

    test("a green creature on top of the library is castable with only blue mana") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(you, "Vizier of the Menagerie")
        val bear = driver.putCardOnTopOfLibrary(you, "Test Green Bear")
        driver.giveMana(you, Color.BLUE, 2)

        val fromLibrary = driver.legalActions(you).singleOrNull {
            it.actionType == "CastSpell" &&
                it.description.contains("Test Green Bear") &&
                it.sourceZone == "LIBRARY"
        }
        require(fromLibrary != null) { "Expected a library cast for the top creature card" }

        driver.castSpell(you, bear).error shouldBe null
        driver.bothPass()

        driver.getCreatures(you).any { driver.getCardName(it) == "Test Green Bear" } shouldBe true
    }

    test("a noncreature card on top of the library is not castable from there") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(you, "Vizier of the Menagerie")
        driver.putCardOnTopOfLibrary(you, "Test Green Sorcery")
        driver.giveMana(you, Color.BLUE, 2)

        driver.legalActions(you).any {
            it.actionType == "CastSpell" && it.sourceZone == "LIBRARY"
        } shouldBe false
    }
})
