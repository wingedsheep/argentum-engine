package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ogw.cards.OathOfNissa
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Oath of Nissa (OGW #140).
 *
 * When Oath of Nissa enters, look at the top three cards of your library. You may reveal a
 * creature, land, or planeswalker card from among them and put it into your hand. Put the rest on
 * the bottom of your library in any order.
 * You may spend mana as though it were mana of any color to cast planeswalker spells.
 *
 * The static is `SpendAnyManaTypeForSpells(Planeswalker, asThoughAnyColor = true)`. Its contract:
 * any mana pays a planeswalker spell's colored pips, a `{C}` pip still needs colorless mana (that's
 * the difference from "mana of any type"), and nonplaneswalker spells are untouched.
 */
class OathOfNissaScenarioTest : FunSpec({

    val bluePlaneswalker = CardDefinition.planeswalker(
        name = "Test Blue Walker",
        manaCost = ManaCost.parse("{1}{U}{U}"),
        subtypes = setOf(Subtype("Jace")),
        startingLoyalty = 3,
    )

    val colorlessPipWalker = CardDefinition.planeswalker(
        name = "Test Colorless Walker",
        manaCost = ManaCost.parse("{1}{C}"),
        subtypes = setOf(Subtype("Ugin")),
        startingLoyalty = 3,
    )

    val blueBear = CardDefinition.creature(
        name = "Test Blue Bear",
        manaCost = ManaCost.parse("{1}{U}"),
        subtypes = emptySet(),
        power = 2,
        toughness = 2,
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(OathOfNissa, bluePlaneswalker, colorlessPipWalker, blueBear))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.canCast(name: String): Boolean =
        legalActions(activePlayer!!).any { it.actionType == "CastSpell" && it.description.contains(name) }

    test("a blue planeswalker spell is castable with only green mana") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putPermanentOnBattlefield(you, "Oath of Nissa")
        val walker = driver.putCardInHand(you, "Test Blue Walker")
        driver.giveMana(you, Color.GREEN, 3)

        driver.canCast("Test Blue Walker") shouldBe true
        driver.castSpell(you, walker).error shouldBe null
        driver.bothPass()

        (driver.findPermanent(you, "Test Blue Walker") != null) shouldBe true
    }

    test("the displayed mana cost stays the printed cost") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putPermanentOnBattlefield(you, "Oath of Nissa")
        driver.putCardInHand(you, "Test Blue Walker")
        driver.giveMana(you, Color.GREEN, 3)

        driver.legalActions(you).single {
            it.actionType == "CastSpell" && it.description.contains("Test Blue Walker")
        }.manaCostString shouldBe "{1}{U}{U}"
    }

    test("without the Oath the off-color planeswalker is not castable") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putCardInHand(you, "Test Blue Walker")
        driver.giveMana(you, Color.GREEN, 3)

        driver.canCast("Test Blue Walker") shouldBe false
    }

    test("nonplaneswalker spells are not relaxed") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putPermanentOnBattlefield(you, "Oath of Nissa")
        driver.putCardInHand(you, "Test Blue Bear")
        driver.giveMana(you, Color.GREEN, 2)

        driver.canCast("Test Blue Bear") shouldBe false
    }

    test("a {C} pip still needs colorless mana — colorless is a type, not a color") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putPermanentOnBattlefield(you, "Oath of Nissa")
        driver.putCardInHand(you, "Test Colorless Walker")
        driver.giveMana(you, Color.GREEN, 2)

        driver.canCast("Test Colorless Walker") shouldBe false

        driver.giveColorlessMana(you, 1)
        driver.canCast("Test Colorless Walker") shouldBe true
    }

    test("an opponent's Oath doesn't relax your planeswalker spells") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.putPermanentOnBattlefield(opponent, "Oath of Nissa")
        driver.putCardInHand(you, "Test Blue Walker")
        driver.giveMana(you, Color.GREEN, 3)

        driver.canCast("Test Blue Walker") shouldBe false
    }

    test("enters: reveal a planeswalker from the top three, the rest go to the bottom") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.putCardOnTopOfLibrary(you, "Test Blue Bear")
        val walker = driver.putCardOnTopOfLibrary(you, "Test Blue Walker")
        driver.putCardOnTopOfLibrary(you, "Forest")
        val oath = driver.putCardInHand(you, "Oath of Nissa")
        driver.giveMana(you, Color.GREEN, 1)

        driver.castSpell(you, oath).error shouldBe null
        driver.bothPass() // Oath resolves, trigger goes on the stack
        driver.bothPass() // trigger resolves

        val choice = driver.pendingDecision as SelectCardsDecision
        choice.options.size shouldBe 3
        choice.maxSelections shouldBe 1
        driver.submitCardSelection(you, listOf(walker)).error shouldBe null
        when (val order = driver.pendingDecision) {
            is OrderObjectsDecision -> driver.submitOrderedResponse(you, order.objects)
            is ReorderLibraryDecision -> driver.submitOrderedResponse(you, order.cards)
            else -> Unit
        }

        (walker in driver.getHand(you)) shouldBe true
        val library = driver.state.getLibrary(you)
        library.takeLast(2).map { driver.getCardName(it) }.toSet() shouldBe setOf("Forest", "Test Blue Bear")
    }
})
