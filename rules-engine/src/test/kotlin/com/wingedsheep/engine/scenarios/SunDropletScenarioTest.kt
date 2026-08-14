package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.SunDroplet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Sun Droplet (MRD #249).
 *
 * "Whenever you're dealt damage, put that many charge counters on this artifact. At the beginning
 * of each upkeep, you may remove a charge counter from this artifact. If you do, you gain 1 life."
 *
 * Covers the source-blind damage-to-you trigger ([com.wingedsheep.sdk.dsl.Triggers.YouAreDealtDamage]):
 * a burn spell is a noncreature source, which the old damage-to-you detector silently ignored.
 */
class SunDropletScenarioTest : FunSpec({

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SunDroplet))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        return Triple(driver, you, driver.getOpponent(you))
    }

    fun GameTestDriver.chargeCountersOn(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0

    /** Drain the stack: the spell resolves, then the damage trigger it caused resolves too. */
    fun GameTestDriver.resolveAll(max: Int = 10) {
        var i = 0
        while (state.stack.isNotEmpty() && pendingDecision == null && i++ < max) bothPass()
    }

    test("a burn spell — a noncreature source — banks one charge counter per damage") {
        val (driver, you) = newGame()
        val droplet = driver.putPermanentOnBattlefield(you, "Sun Droplet")

        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(you)))
        driver.resolveAll()

        driver.getLifeTotal(you) shouldBe 17
        driver.chargeCountersOn(droplet) shouldBe 3
    }

    test("damage dealt to an opponent leaves your Droplet empty") {
        val (driver, you, opponent) = newGame()
        val droplet = driver.putPermanentOnBattlefield(you, "Sun Droplet")

        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(opponent)))
        driver.resolveAll()

        driver.getLifeTotal(opponent) shouldBe 17
        driver.chargeCountersOn(droplet) shouldBe 0
    }

    test("upkeep: accepting the may removes one counter and gains 1 life") {
        val (driver, you) = newGame()
        val droplet = driver.putPermanentOnBattlefield(you, "Sun Droplet")

        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(you)))
        driver.resolveAll()
        driver.chargeCountersOn(droplet) shouldBe 3
        val lifeAfterBolt = driver.getLifeTotal(you)

        // "Each upkeep" — the very next upkeep is the opponent's, and it is still *our* choice.
        driver.passPriorityUntil(Step.UPKEEP)
        driver.resolveAll()
        driver.submitYesNo(you, true)

        driver.chargeCountersOn(droplet) shouldBe 2
        driver.getLifeTotal(you) shouldBe lifeAfterBolt + 1
    }

    test("upkeep: declining the may keeps the counter and gains nothing") {
        val (driver, you) = newGame()
        val droplet = driver.putPermanentOnBattlefield(you, "Sun Droplet")

        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(you)))
        driver.resolveAll()
        val lifeAfterBolt = driver.getLifeTotal(you)

        driver.passPriorityUntil(Step.UPKEEP)
        driver.resolveAll()
        driver.submitYesNo(you, false)

        driver.chargeCountersOn(droplet) shouldBe 3
        driver.getLifeTotal(you) shouldBe lifeAfterBolt
    }

    test("an empty Droplet raises no upkeep decision at all") {
        val (driver, you) = newGame()
        val droplet = driver.putPermanentOnBattlefield(you, "Sun Droplet")
        driver.chargeCountersOn(droplet) shouldBe 0

        driver.passPriorityUntil(Step.UPKEEP)

        driver.pendingDecision shouldBe null
        driver.getLifeTotal(you) shouldBe 20
    }
})
