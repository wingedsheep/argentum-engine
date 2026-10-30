package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Phase Dolphin (IKO #62) — {2}{U} Creature — Elemental Whale 1/4.
 *
 *   "Whenever this creature attacks, another target attacking creature can't be blocked this turn."
 */
class PhaseDolphinScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20,
            skipMulligans = true
        )
        return driver
    }

    test("the other attacker can't be blocked; the Dolphin itself is neither a target nor unblockable") {
        val driver = createDriver()
        val attacker = driver.player1
        val defender = driver.player2

        val dolphin = driver.putCreatureOnBattlefield(attacker, "Phase Dolphin")
        driver.removeSummoningSickness(dolphin)
        val bears = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        driver.removeSummoningSickness(bears)
        val blocker1 = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")
        driver.removeSummoningSickness(blocker1)
        val blocker2 = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")
        driver.removeSummoningSickness(blocker2)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(dolphin, bears), defender)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseTargetsDecision>()
        withClue("\"another target\" excludes the Dolphin") {
            decision.legalTargets.values.flatten().contains(dolphin) shouldBe false
        }
        driver.submitTargetSelection(attacker, listOf(bears))

        driver.bothPass()
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        val blockBears = driver.submitExpectFailure(
            DeclareBlockers(defender, mapOf(blocker1 to listOf(bears)))
        )
        blockBears.outcome shouldNotBe Outcome.Done
        blockBears.error shouldContainIgnoringCase "can't be blocked"

        driver.declareBlockers(defender, mapOf(blocker2 to listOf(dolphin))).outcome shouldBe Outcome.Done
    }
})
