package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.legalactions.support.EnumerationFixtures
import com.wingedsheep.sdk.core.Step
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Tests for [enumerators.PassPriorityEnumerator].
 *
 * The enumerator is unconditional — it always emits exactly one PassPriority
 * action for the requested player. These tests pin that contract.
 */
class PassPriorityEnumeratorTest : FunSpec({

    test("emits exactly one PassPriority on the active player's main phase") {
        val driver = EnumerationFixtures.mainPhaseOfActivePlayer()

        val passes = driver.enumerateFor(driver.player1).filter { it.action is PassPriority }

        passes shouldHaveSize 1
        (passes.single().action as PassPriority).playerId shouldBe driver.player1
    }

    test("emits a PassPriority for the non-priority player too") {
        // The enumerator does not gate on priority — game-server filters that.
        // A non-active player can still ask "what would I be allowed to do?"
        // and must get a PassPriority back.
        val driver = EnumerationFixtures.mainPhaseOfActivePlayer()

        val passes = driver.enumerateFor(driver.player2).filter { it.action is PassPriority }

        passes shouldHaveSize 1
        (passes.single().action as PassPriority).playerId shouldBe driver.player2
    }

    test("emits PassPriority on the upkeep step") {
        val driver = EnumerationFixtures.forestAndBearsMirror()
        driver.game.passPriorityUntil(Step.UPKEEP)

        val passes = driver.enumerateFor(driver.player1).filter { it.action is PassPriority }

        passes shouldHaveSize 1
    }

    test("PassPriority is the description and actionType is well-formed") {
        val driver = EnumerationFixtures.mainPhaseOfActivePlayer()

        val pass = driver.enumerateFor(driver.player1).single { it.action is PassPriority }

        pass.actionType shouldBe "PassPriority"
        pass.description shouldBe "Pass priority"
        pass.affordable shouldBe true
    }
})
