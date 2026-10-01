package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.KuldothaCackler
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Kuldotha Cackler (ONE #139) — {2}{R} 2/3 Creature — Phyrexian Hyena.
 *
 * "Trample. Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the
 *  number of permanents you control with oil counters on them."
 */
class KuldothaCacklerScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KuldothaCackler))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun attackAndResolve(driver: GameTestDriver, cackler: EntityId) {
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(cackler), driver.player2)
        var guard = 0
        while ((driver.state.stack.isNotEmpty() || driver.pendingDecision != null) && guard++ < 10) {
            if (driver.pendingDecision != null) driver.autoResolveDecision() else driver.bothPass()
        }
    }

    test("has trample and base 2/3") {
        val driver = newDriver()
        val cackler = driver.putCreatureOnBattlefield(driver.player1, "Kuldotha Cackler")
        driver.state.projectedState.hasKeyword(cackler, Keyword.TRAMPLE) shouldBe true
        driver.state.projectedState.getPower(cackler) shouldBe 2
        driver.state.projectedState.getToughness(cackler) shouldBe 3
    }

    test("attacking gets +X/+0 for each permanent you control with oil counters") {
        val driver = newDriver()
        val p1 = driver.player1
        val cackler = driver.putCreatureOnBattlefield(p1, "Kuldotha Cackler")
        driver.removeSummoningSickness(cackler)
        val oiledA = driver.putCreatureOnBattlefield(p1, "Savannah Lions")
        val oiledB = driver.putPermanentOnBattlefield(p1, "Mountain")
        driver.putCreatureOnBattlefield(p1, "Centaur Courser") // no counters
        val opposingOiled = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        driver.addComponent(oiledA, CountersComponent(mapOf(CounterType.OIL to 3)))
        driver.addComponent(oiledB, CountersComponent(mapOf(CounterType.OIL to 1)))
        driver.addComponent(opposingOiled, CountersComponent(mapOf(CounterType.OIL to 2)))

        attackAndResolve(driver, cackler)

        // Two permanents you control carry oil (counter amounts don't matter; opponent's don't count).
        driver.state.projectedState.getPower(cackler) shouldBe 4
        driver.state.projectedState.getToughness(cackler) shouldBe 3
    }

    test("no oiled permanents means no bonus") {
        val driver = newDriver()
        val cackler = driver.putCreatureOnBattlefield(driver.player1, "Kuldotha Cackler")
        driver.removeSummoningSickness(cackler)
        attackAndResolve(driver, cackler)
        driver.state.projectedState.getPower(cackler) shouldBe 2
    }
})
