package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.SplitSecond
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Siege Smash (MH3 #136) — split second, choose one: destroy target artifact, or target creature
 * gets +3/+2 and gains trample until end of turn.
 */
class SiegeSmashScenarioTest : FunSpec({

    val TestRelic = card("Test Relic") {
        manaCost = "{2}"
        typeLine = "Artifact"
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + TestRelic)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castSmash(player: EntityId, modeIndex: Int, target: EntityId) = run {
        val card = putCardInHand(player, "Siege Smash")
        giveMana(player, Color.RED, 2)
        val targets = listOf(ChosenTarget.Permanent(target))
        submit(
            CastSpell(
                playerId = player,
                cardId = card,
                targets = targets,
                chosenModes = listOf(modeIndex),
                modeTargetsOrdered = listOf(targets),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
    }

    test("mode 1 destroys target artifact") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val relic = driver.putPermanentOnBattlefield(opponent, "Test Relic")

        driver.castSmash(me, 0, relic).error shouldBe null
        driver.bothPass()

        driver.findPermanent(opponent, "Test Relic") shouldBe null
    }

    test("mode 2 gives +3/+2 and trample until end of turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.castSmash(me, 1, bears).error shouldBe null
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.getPower(bears) shouldBe 5
        projected.getToughness(bears) shouldBe 4
        projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true

        driver.passPriorityUntil(Step.UPKEEP)
        val after = driver.state.projectedState
        after.getPower(bears) shouldBe 2
        after.getToughness(bears) shouldBe 2
        after.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
    }

    test("split second: the opponent can't respond with a spell") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)

        driver.castSmash(me, 1, bears).error shouldBe null
        SplitSecond.isLocked(driver.state, driver.cardRegistry) shouldBe true
        driver.passPriority(me)
        driver.assertPriority(opponent)

        driver.legalActions(opponent).filter { it.action is CastSpell }.shouldBeEmpty()
        driver.submitExpectFailure(CastSpell(opponent, bolt, listOf(ChosenTarget.Permanent(bears))))
        driver.assertStackSize(1)
    }
})
