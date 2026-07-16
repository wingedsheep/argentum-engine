package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Apprentice Sharpshooter (VOW #185) — {2}{G} 1/4 Creature — Human Archer, Reach + Training.
 *
 * The plain Training body: no payoff of its own, so this proves the two keywords the card carries
 * are both live —
 *  - Reach is present as a projected keyword (an unrelated evasion-denial static), and
 *  - Training (CR 702.149a) puts exactly one +1/+1 counter when the Sharpshooter (power 1) attacks
 *    alongside a creature of strictly greater power, and none when it attacks alone. The full
 *    projected-power comparison matrix lives in [TrainingTest]; here we pin this specific card.
 */
class ApprenticeSharpshooterScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        return driver
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.drainStack() {
        var guard = 0
        while ((stackSize > 0 || pendingDecision != null) && guard < 20) {
            if (pendingDecision != null) autoResolveDecision() else bothPass()
            guard++
        }
    }

    test("Apprentice Sharpshooter has Reach") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val archer = driver.putCreatureOnBattlefield(me, "Apprentice Sharpshooter")

        driver.state.projectedState.hasKeyword(archer, Keyword.REACH) shouldBe true
    }

    test("training triggers once when attacking alongside a creature of strictly greater power") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val archer = driver.putCreatureOnBattlefield(me, "Apprentice Sharpshooter") // power 1
        val centaur = driver.putCreatureOnBattlefield(me, "Centaur Courser")        // power 3 (greater)
        listOf(archer, centaur).forEach { driver.removeSummoningSickness(it) }

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(archer, centaur), opp)
        driver.drainStack()

        driver.plusOneCounters(archer) shouldBe 1
    }

    test("training does not trigger when the Sharpshooter attacks alone") {
        val driver = newDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val archer = driver.putCreatureOnBattlefield(me, "Apprentice Sharpshooter")
        driver.removeSummoningSickness(archer)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(archer), opp)
        driver.drainStack()

        driver.plusOneCounters(archer) shouldBe 0
    }
})
