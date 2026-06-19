package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.ClammyProwler
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Clammy Prowler — {3}{U} Enchantment Creature — Horror 2/5
 * Whenever this creature attacks, another target attacking creature can't be blocked this turn.
 */
class ClammyProwlerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ClammyProwler)
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20,
            skipMulligans = true
        )
        return driver
    }

    test("the chosen other attacking creature can't be blocked, but Clammy Prowler can") {
        val driver = createDriver()
        val attacker = driver.player1
        val defender = driver.player2

        val prowler = driver.putCreatureOnBattlefield(attacker, "Clammy Prowler")
        driver.removeSummoningSickness(prowler)
        val bears = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        driver.removeSummoningSickness(bears)

        // Opponent has two blockers.
        val wall = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")
        driver.removeSummoningSickness(wall)
        val wall2 = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")
        driver.removeSummoningSickness(wall2)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Both creatures attack. Clammy Prowler's attack trigger fires.
        driver.declareAttackers(attacker, listOf(prowler, bears), defender)

        // The trigger asks for "another target attacking creature" — only the Bears is legal
        // (Clammy Prowler itself is excluded by TargetOther).
        val chooseTargets = driver.pendingDecision
        chooseTargets.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(attacker, listOf(bears))

        // Resolve the trigger, then advance into the declare-blockers step.
        driver.bothPass()
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        // Blocking the Bears (now unblockable) must fail.
        val blockBears = driver.submitExpectFailure(
            DeclareBlockers(defender, mapOf(wall to listOf(bears)))
        )
        blockBears.isSuccess shouldBe false
        blockBears.error shouldContainIgnoringCase "can't be blocked"

        // Blocking Clammy Prowler (which is NOT unblockable) succeeds.
        val blockProwler = driver.declareBlockers(defender, mapOf(wall2 to listOf(prowler)))
        blockProwler.isSuccess shouldBe true
    }

    test("Clammy Prowler attacking with no other attacker yields no legal target") {
        val driver = createDriver()
        val attacker = driver.player1
        val defender = driver.player2

        val prowler = driver.putCreatureOnBattlefield(attacker, "Clammy Prowler")
        driver.removeSummoningSickness(prowler)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(prowler), defender)

        // No other attacking creature exists, so the trigger has no legal target and
        // is removed from the stack (no targeting decision is pending).
        driver.pendingDecision.let { it !is ChooseTargetsDecision } shouldBe true
    }
})
