package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.SamuraiOfThePaleCurtain
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Samurai of the Pale Curtain (CHK #43):
 *   "Bushido 1. If a permanent would be put into a graveyard, exile it instead."
 *
 * Proves the replacement is scoped to permanents (battlefield -> graveyard) — a resolved instant
 * still reaches the graveyard — that it covers opposing creatures and the Samurai itself, and that
 * the lowered bushido pump fires when it blocks.
 */
class SamuraiOfThePaleCurtainScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(SamuraiOfThePaleCurtain))
        return driver
    }

    fun startTurn(driver: GameTestDriver): EntityId {
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver.activePlayer!!
    }

    test("an opposing creature that would die is exiled instead") {
        val driver = createDriver()
        val you = startTurn(driver)
        val opp = driver.getOpponent(you)

        driver.putCreatureOnBattlefield(you, "Samurai of the Pale Curtain")
        val victim = driver.putCreatureOnBattlefield(opp, "Savannah Lions")

        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, targets = listOf(victim)).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getGraveyard(opp) shouldNotContain victim
        driver.getExile(opp) shouldContain victim
    }

    test("a resolved instant is not a permanent and still goes to the graveyard") {
        val driver = createDriver()
        val you = startTurn(driver)
        val opp = driver.getOpponent(you)

        driver.putCreatureOnBattlefield(you, "Samurai of the Pale Curtain")
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, targets = listOf(opp)).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getGraveyard(you) shouldContain bolt
        driver.getExile(you) shouldNotContain bolt
    }

    test("the Samurai itself is exiled when it would die") {
        val driver = createDriver()
        val you = startTurn(driver)

        val samurai = driver.putCreatureOnBattlefield(you, "Samurai of the Pale Curtain")
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, targets = listOf(samurai)).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getGraveyard(you) shouldNotContain samurai
        driver.getExile(you) shouldContain samurai
    }

    test("bushido 1 pumps it when it blocks, and the slain attacker is exiled") {
        val driver = createDriver()
        val you = startTurn(driver)
        val opp = driver.getOpponent(you)

        val attacker = driver.putCreatureOnBattlefield(you, "Goblin Guide")
        driver.removeSummoningSickness(attacker)
        val samurai = driver.putCreatureOnBattlefield(opp, "Samurai of the Pale Curtain")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(you, listOf(attacker), opp)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(opp, mapOf(samurai to listOf(attacker)))
        driver.passPriorityUntil(Step.END_COMBAT)

        // Unpumped, the 2/2 Samurai would die to the 2/1; bushido makes it a 3/3 that survives.
        driver.findPermanent(opp, "Samurai of the Pale Curtain") shouldBe samurai
        driver.getGraveyard(you) shouldNotContain attacker
        driver.getExile(you) shouldContain attacker
    }
})
