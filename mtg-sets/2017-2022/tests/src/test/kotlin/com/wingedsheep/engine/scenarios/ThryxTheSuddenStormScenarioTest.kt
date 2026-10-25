package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.thb.cards.ThryxTheSuddenStorm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Thryx, the Sudden Storm — "Spells you cast with mana value 5 or greater cost {1} less to cast and
 * can't be countered."
 *
 * Pins both halves: a five-drop is castable for four, and a Counterspell aimed at it does nothing,
 * while a cheaper spell stays counterable.
 */
class ThryxTheSuddenStormScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ThryxTheSuddenStorm))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("a mana value 5 spell costs {1} less and can't be countered") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.putPermanentOnBattlefield(you, "Thryx, the Sudden Storm")

        val force = driver.putCardInHand(you, "Force of Nature")
        driver.giveMana(you, Color.GREEN, 2)
        driver.giveColorlessMana(you, 2)
        driver.castSpell(you, force).error shouldBe null
        driver.passPriority(you)

        val counterspell = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(opponent, Color.BLUE, 2)
        driver.castSpellWithTargets(opponent, counterspell, listOf(ChosenTarget.Spell(force))).error shouldBe null
        while (driver.stackSize > 0) driver.bothPass()

        driver.findPermanent(you, "Force of Nature") shouldNotBe null
        driver.getGraveyardCardNames(opponent) shouldBe listOf("Counterspell")
    }

    test("a spell with lower mana value is still counterable") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.putPermanentOnBattlefield(you, "Thryx, the Sudden Storm")

        val courser = driver.putCardInHand(you, "Centaur Courser")
        driver.giveMana(you, Color.GREEN, 3)
        driver.castSpell(you, courser).error shouldBe null
        driver.passPriority(you)

        val counterspell = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(opponent, Color.BLUE, 2)
        driver.castSpellWithTargets(opponent, counterspell, listOf(ChosenTarget.Spell(courser))).error shouldBe null
        while (driver.stackSize > 0) driver.bothPass()

        driver.findPermanent(you, "Centaur Courser") shouldBe null
        driver.getGraveyardCardNames(you) shouldBe listOf("Centaur Courser")
    }
})
