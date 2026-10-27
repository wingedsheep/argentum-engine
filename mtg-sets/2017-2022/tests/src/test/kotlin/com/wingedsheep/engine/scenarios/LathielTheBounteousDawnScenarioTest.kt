package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.cmr.cards.LathielTheBounteousDawn
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Lathiel, the Bounteous Dawn — "At the beginning of each end step, if you gained life this turn,
 * distribute up to that many +1/+1 counters among any number of other target creatures."
 *
 * The "up to" division is announced as the trigger goes on the stack and may total less than the
 * life gained — even with a single target.
 */
class LathielTheBounteousDawnScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LathielTheBounteousDawn))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, driver.player1)
        return driver
    }

    fun GameTestDriver.gainLife(amount: Int) {
        setLifeTotal(player1, getLifeTotal(player1) + amount)
        replaceState(DamageUtils.markLifeGainedThisTurn(state, player1, amount))
    }

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Pass to this turn's end step, where Lathiel's trigger asks for its targets. */
    fun GameTestDriver.toEndStepTargets(): ChooseTargetsDecision {
        passPriorityUntil(Step.END)
        return pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
    }

    test("distributes fewer counters than the life gained among two targets") {
        val driver = newDriver()
        driver.putCreatureOnBattlefield(driver.player1, "Lathiel, the Bounteous Dawn")
        val bearA = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val bearB = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.gainLife(4)

        driver.toEndStepTargets()
        driver.submitMultiTargetSelection(driver.player1, mapOf(0 to listOf(bearA, bearB)))

        val division = driver.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        division.totalAmount shouldBe 4
        division.allowPartial shouldBe true
        division.prompt shouldContain "up to 4"
        driver.submitDecision(driver.player1, DistributionResponse(division.id, mapOf(bearA to 1, bearB to 2)))

        driver.bothPass()
        driver.plusOnes(bearA) shouldBe 1
        driver.plusOnes(bearB) shouldBe 2
    }

    test("a single target still chooses how many it gets") {
        val driver = newDriver()
        driver.putCreatureOnBattlefield(driver.player1, "Lathiel, the Bounteous Dawn")
        val opposingBear = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.gainLife(3)

        driver.toEndStepTargets()
        driver.submitMultiTargetSelection(driver.player1, mapOf(0 to listOf(opposingBear)))

        val division = driver.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        division.allowPartial shouldBe true
        driver.submitDecision(driver.player1, DistributionResponse(division.id, mapOf(opposingBear to 1)))

        driver.bothPass()
        driver.plusOnes(opposingBear) shouldBe 1
    }

    test("a division over the life gained is rejected") {
        val driver = newDriver()
        driver.putCreatureOnBattlefield(driver.player1, "Lathiel, the Bounteous Dawn")
        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.gainLife(2)

        driver.toEndStepTargets()
        driver.submitMultiTargetSelection(driver.player1, mapOf(0 to listOf(bear)))

        val division = driver.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        val result = driver.submitDecision(driver.player1, DistributionResponse(division.id, mapOf(bear to 3)))
        result.error shouldNotBe null
    }

    test("triggers on an opponent's end step too") {
        val driver = newDriver()
        driver.putCreatureOnBattlefield(driver.player1, "Lathiel, the Bounteous Dawn")
        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")

        driver.passPriorityUntil(Step.UPKEEP, driver.player2)
        driver.gainLife(2)
        driver.passPriorityUntil(Step.END)
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitMultiTargetSelection(driver.player1, mapOf(0 to listOf(bear)))
        val division = driver.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        division.totalAmount shouldBe 2
        driver.submitDecision(driver.player1, DistributionResponse(division.id, mapOf(bear to 2)))

        driver.bothPass()
        driver.plusOnes(bear) shouldBe 2
    }

    test("gaining more life in response changes neither the pool nor the division") {
        val driver = newDriver()
        driver.putCreatureOnBattlefield(driver.player1, "Lathiel, the Bounteous Dawn")
        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        driver.gainLife(2)

        driver.toEndStepTargets()
        driver.submitMultiTargetSelection(driver.player1, mapOf(0 to listOf(bear)))
        val division = driver.pendingDecision.shouldBeInstanceOf<DistributeDecision>()
        driver.submitDecision(driver.player1, DistributionResponse(division.id, mapOf(bear to 2)))

        driver.gainLife(5)
        driver.bothPass()
        driver.plusOnes(bear) shouldBe 2
    }

    test("doesn't trigger when you gained no life this turn") {
        val driver = newDriver()
        driver.putCreatureOnBattlefield(driver.player1, "Lathiel, the Bounteous Dawn")
        val bear = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")

        driver.passPriorityUntil(Step.END)
        driver.pendingDecision shouldBe null
        driver.state.stack.isEmpty() shouldBe true
        driver.plusOnes(bear) shouldBe 0
    }
})
