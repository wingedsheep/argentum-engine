package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.CombatResolutionResponse
import com.wingedsheep.engine.core.DamageEdgeAmount
import com.wingedsheep.engine.core.DamageEdgeDirection
import com.wingedsheep.engine.core.EngineFeatures
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Phase 2 of the combat resolution board migration
 * (see `docs/plans/combat-resolution-board.md`). Verifies the engine emits the
 * bipartite [CombatResolutionDecision] shape when
 * [EngineFeatures.combatResolutionBoardEnabled] is on.
 */
class CombatResolutionBoardTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver(
            features = EngineFeatures(combatResolutionBoardEnabled = true),
        )
        driver.registerCards(TestCards.all)
        return driver
    }

    /** Advance steps until a pending decision shows up (without auto-resolving). */
    fun advanceUntilDecision(driver: GameTestDriver, maxPasses: Int = 50) {
        var passes = 0
        while (driver.state.pendingDecision == null && passes < maxPasses) {
            val priority = driver.state.priorityPlayerId ?: error("No priority and no pending decision")
            driver.submit(PassPriority(priority))
            passes++
            if (driver.state.gameOver) error("Game ended before a decision was emitted")
        }
        if (passes >= maxPasses) {
            error("No pending decision emitted within $maxPasses passes; current step=${driver.currentStep}")
        }
    }

    test("trample attacker blocked by two creatures emits a CombatResolutionDecision with a drain edge") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val attacker = driver.activePlayer!!
        val defender = if (attacker == driver.player1) driver.player2 else driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val tusker = driver.putCreatureOnBattlefield(attacker, "Trample Beast")          // 5/5 trample
        val lions = driver.putCreatureOnBattlefield(defender, "Savannah Lions")           // 2/1
        val centaur = driver.putCreatureOnBattlefield(defender, "Centaur Courser")        // 3/3
        driver.removeSummoningSickness(tusker)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(tusker), defender)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        // declareBlockers takes a Map<blocker, List<attackers>>. With multiple blockers on
        // the same attacker, the engine immediately pauses on an OrderObjectsDecision so
        // the chooser can pick damage-assignment order; result.isPaused == true.
        driver.declareBlockers(defender, mapOf(lions to listOf(tusker), centaur to listOf(tusker)))

        // The engine emits an OrderObjectsDecision for the blocker-order in declare-blockers.
        // Submit the natural order (lions, centaur) and continue.
        var decision: com.wingedsheep.engine.core.PendingDecision? = driver.state.pendingDecision
        if (decision is com.wingedsheep.engine.core.OrderObjectsDecision) {
            driver.submitDecision(
                decision.playerId,
                com.wingedsheep.engine.core.OrderedResponse(decision.id, listOf(lions, centaur))
            )
        }
        advanceUntilDecision(driver)
        decision = driver.state.pendingDecision

        decision.shouldBeInstanceOf<CombatResolutionDecision>()
        decision.firstStrike shouldBe false
        decision.attackers.map { it.id } shouldBe listOf(tusker)
        decision.attackers.first().hasTrample shouldBe true
        decision.blockers.map { it.id }.toSet() shouldBe setOf(lions, centaur)

        val tuskerEdges = decision.edges.filter { it.sourceId == tusker }
        tuskerEdges.size shouldBe 3
        val drain = tuskerEdges.single { it.isTrampleDrain }
        drain.targetId shouldBe defender
        drain.direction shouldBe DamageEdgeDirection.ATTACKER_TO_PLAYER

        // Submit the engine-supplied defaults and confirm the engine moves on cleanly.
        val response = CombatResolutionResponse(
            decisionId = decision.id,
            edges = decision.edges.map { DamageEdgeAmount(it.id, it.amount) },
        )
        driver.submitDecision(decision.playerId, response).isSuccess shouldBe true
    }

    test("non-trample attacker with single blocker does not emit a resolution decision") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val attacker = driver.activePlayer!!
        val defender = if (attacker == driver.player1) driver.player2 else driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val courser = driver.putCreatureOnBattlefield(attacker, "Centaur Courser")  // 3/3
        val lions = driver.putCreatureOnBattlefield(defender, "Savannah Lions")     // 2/1
        driver.removeSummoningSickness(courser)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(courser), defender).isSuccess shouldBe true
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(defender, mapOf(lions to listOf(courser))).isSuccess shouldBe true

        // No manual assignment needed — engine auto-resolves combat damage internally.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        // Lions (1t) dies from 3 damage; Courser survives at 3t having taken 2.
        driver.state.getBattlefield().contains(lions) shouldBe false
    }
})
