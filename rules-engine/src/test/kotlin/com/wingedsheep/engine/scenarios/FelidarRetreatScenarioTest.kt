package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Felidar Retreat (ZNR #16).
 *
 * {3}{W} Enchantment
 *  Landfall — Whenever a land you control enters, choose one —
 *   • Create a 2/2 white Cat Beast creature token.
 *   • Put a +1/+1 counter on each creature you control. Those creatures gain vigilance until
 *     end of turn.
 *
 * Exercises the modal triggered ability from both sides, and pins the ZNR ruling that mode two
 * affects only the creatures present when the ability resolves.
 */
class FelidarRetreatScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /**
     * Play a land from hand and pick [modeIndex] on the landfall trigger.
     *
     * The mode is announced as the trigger is *put onto the stack* (CR 603.3c), so the decision is
     * already pending when `playLand` returns — before either player gets priority. Only once it is
     * answered does passing priority resolve the ability.
     */
    fun triggerLandfall(driver: GameTestDriver, player: EntityId, modeIndex: Int) {
        val land = driver.putCardInHand(player, "Forest")
        val play = driver.playLand(player, land)
        withClue("playing the land should succeed: ${play.error}") { play.error shouldBe null }

        val decision = driver.pendingDecision as? ChooseOptionDecision
            ?: error("expected a mode choice for the landfall trigger; got ${driver.pendingDecision}")
        driver.submitDecision(player, OptionChosenResponse(decision.id, optionIndex = modeIndex))

        driver.bothPass()
    }

    fun plusOneCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("mode one creates a single 2/2 white Cat Beast token") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Felidar Retreat")

        triggerLandfall(driver, player, modeIndex = 0)

        val tokens = driver.getPermanents(player).filter { driver.getCardName(it) == "Cat Beast Token" }
        withClue("exactly one Cat Beast token") { tokens.size shouldBe 1 }

        val projected = projector.project(driver.state)
        projected.getPower(tokens.single()) shouldBe 2
        projected.getToughness(tokens.single()) shouldBe 2
    }

    test("mode two counters up every creature you control and grants them vigilance") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Felidar Retreat")

        val bearsA = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val bearsB = driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        triggerLandfall(driver, player, modeIndex = 1)

        val projected = projector.project(driver.state)
        listOf(bearsA, bearsB).forEach { bear ->
            withClue("each creature gets a +1/+1 counter") { plusOneCounters(driver, bear) shouldBe 1 }
            withClue("2/2 plus a counter = 3/3") {
                projected.getPower(bear) shouldBe 3
                projected.getToughness(bear) shouldBe 3
            }
            withClue("and gains vigilance until end of turn") {
                projected.hasKeyword(bear, Keyword.VIGILANCE) shouldBe true
            }
        }
    }

    test("a creature that enters after the ability resolved gets neither the counter nor vigilance") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Felidar Retreat")

        val earlyBears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        triggerLandfall(driver, player, modeIndex = 1)

        val lateBears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        val projected = projector.project(driver.state)
        withClue("the creature present at resolution got a counter") {
            plusOneCounters(driver, earlyBears) shouldBe 1
            projected.hasKeyword(earlyBears, Keyword.VIGILANCE) shouldBe true
        }
        withClue("the latecomer gets nothing") {
            plusOneCounters(driver, lateBears) shouldBe 0
            projected.hasKeyword(lateBears, Keyword.VIGILANCE) shouldBe false
            projected.getPower(lateBears) shouldBe 2
        }
    }
})
