package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.DarkAscensionSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class FlayerOfTheHateboundScenarioTest : FunSpec({

    fun newDriver() = GameTestDriver().apply {
        registerCards(TestCards.all + DarkAscensionSet.cards)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolveAll(you: EntityId, target: EntityId) {
        var guard = 0
        while (guard++ < 30) {
            when (pendingDecision) {
                is ChooseTargetsDecision -> submitTargetSelection(you, listOf(target))
                else -> if (state.stack.isNotEmpty()) bothPass() else break
            }
        }
    }

    fun GameTestDriver.bolt(you: EntityId, victim: EntityId) {
        val bolt = putCardInHand(you, "Lightning Bolt")
        giveMana(you, Color.RED, 1)
        castSpell(you, bolt, listOf(victim)).outcome shouldBe Outcome.Done
    }

    test("its own undying return makes it deal damage equal to its new power") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val flayer = driver.putCreatureOnBattlefield(you, "Flayer of the Hatebound")

        driver.bolt(you, flayer)
        driver.resolveAll(you, opponent)

        driver.findPermanent(you, "Flayer of the Hatebound") shouldNotBe null
        driver.getLifeTotal(opponent) shouldBe 15
    }

    test("another creature of yours returning from your graveyard deals its power") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.putCreatureOnBattlefield(you, "Flayer of the Hatebound")
        val vampire = driver.putCreatureOnBattlefield(you, "Vengeful Vampire")

        driver.bolt(you, vampire)
        driver.resolveAll(you, opponent)

        driver.findPermanent(you, "Vengeful Vampire") shouldNotBe null
        driver.getLifeTotal(opponent) shouldBe 16
    }

    test("a creature returning from an opponent's graveyard does not trigger it") {
        val driver = newDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.putCreatureOnBattlefield(you, "Flayer of the Hatebound")
        val theirVampire = driver.putCreatureOnBattlefield(opponent, "Vengeful Vampire")

        driver.bolt(you, theirVampire)
        driver.resolveAll(you, opponent)

        driver.findPermanent(opponent, "Vengeful Vampire") shouldNotBe null
        driver.getLifeTotal(opponent) shouldBe 20
        driver.getLifeTotal(you) shouldBe 20
    }
})
