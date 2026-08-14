package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.PopularEgotist
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/** Scenario tests for Silent Hallcreeper. */
class SilentHallcreeperScenarioTest : FunSpec({

    val projector = StateProjector()

    fun GameTestDriver.advanceToPlayer1DeclareAttackers() {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.DECLARE_ATTACKERS)
            safety++
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Silent Hallcreeper — combat damage offers the modal choice; +1/+1 mode adds two counters") {
        val driver = createDriver()
        driver.registerCard(PopularEgotist) // unused here; keeps registration symmetric
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)

        val me = driver.player1
        val opp = driver.player2
        val creeper = driver.putCreatureOnBattlefield(me, "Silent Hallcreeper")
        driver.removeSummoningSickness(creeper)

        driver.advanceToPlayer1DeclareAttackers()
        driver.declareAttackers(me, listOf(creeper), opp)
        driver.bothPass() // to declare blockers
        driver.declareNoBlockers(opp)
        driver.bothPass() // into combat damage; trigger goes on the stack

        // Resolve into the modal ChooseOptionDecision.
        var guard = 0
        while (driver.pendingDecision !is ChooseOptionDecision && guard < 20) {
            driver.bothPass(); guard++
        }
        val choice = driver.pendingDecision as? ChooseOptionDecision
            ?: error("expected ChooseOptionDecision for the modal trigger; got ${driver.pendingDecision}")
        // Only two of the three modes are offered: the Hallcreeper is the sole creature its
        // controller has, so "another target creature you control" has no legal target and CR 603.3c
        // forbids choosing that mode as the ability goes on the stack.
        choice.options.size shouldBe 2

        val counterMode = "Put two +1/+1 counters on this creature"
        choice.options shouldContain counterMode
        driver.submitDecision(me, OptionChosenResponse(choice.id, choice.options.indexOf(counterMode)))
        driver.bothPass()

        val counters = driver.state.getEntity(creeper)?.get<CountersComponent>()?.counters ?: emptyMap()
        counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 2
        // 1/1 base + two +1/+1 = 3/3
        projector.getProjectedPower(driver.state, creeper) shouldBe 3
        projector.getProjectedToughness(driver.state, creeper) shouldBe 3
    }
})
