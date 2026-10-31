package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.iko.cards.CunningNightbonder
import com.wingedsheep.mtg.sets.definitions.isd.cards.AmbushViper
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Cunning Nightbonder — "Spells with flash you cast cost {1} less to cast and can't be countered."
 *
 * Pins the flash-keyword filter on both halves: Ambush Viper ({1}{G}, flash) is castable for {G}
 * and shrugs off a Counterspell, while a spell without flash stays counterable.
 */
class CunningNightbonderScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CunningNightbonder, AmbushViper))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("a spell with flash costs {1} less and can't be countered") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.putPermanentOnBattlefield(you, "Cunning Nightbonder")

        val viper = driver.putCardInHand(you, "Ambush Viper")
        driver.giveMana(you, Color.GREEN, 1)
        driver.castSpell(you, viper).error shouldBe null
        driver.passPriority(you)

        val counterspell = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(opponent, Color.BLUE, 2)
        driver.castSpellWithTargets(opponent, counterspell, listOf(ChosenTarget.Spell(viper))).error shouldBe null
        while (driver.stackSize > 0) driver.bothPass()

        driver.findPermanent(you, "Ambush Viper") shouldNotBe null
        driver.getGraveyardCardNames(opponent) shouldBe listOf("Counterspell")
    }

    test("a spell without flash is still counterable") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.putPermanentOnBattlefield(you, "Cunning Nightbonder")

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
