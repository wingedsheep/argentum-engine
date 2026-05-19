package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.CombatResolutionResponse
import com.wingedsheep.engine.core.DamageEdgeAmount
import com.wingedsheep.engine.core.DamageEdgeDirection
import com.wingedsheep.engine.core.EngineFeatures
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.CanBlockAnyNumber
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
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

    /**
     * Bipartite crossover (Scenario E in `docs/plans/combat-resolution-board.md`)
     * combined with a trample attacker so the board is forced to emit. A pure
     * bipartite case with no trample currently auto-resolves under the new flag
     * because no per-attacker manual-assignment is needed; that is a known engine
     * gap noted in §10 (the blocker-side division should still surface a
     * decision). Once that gap is closed we can drop the trample helper and
     * test pure bipartite directly.
     *
     * Setup: trample attacker double-blocked by an Ironfist-style crusher and a
     * vanilla blocker; the crusher *also* blocks a second non-trample attacker.
     * Verifies (a) the trample drain edge is emitted, (b) the crusher's
     * blocker→attacker edges surface, (c) ownership defaults to the defender.
     */
    test("bipartite crusher between two manually-assigned attackers emits BLOCKER_TO_ATTACKER edges") {
        val driver = createDriver()
        val testCrusher = CardDefinition.creature(
            name = "Test Crusher",
            manaCost = ManaCost.parse("{4}{W}"),
            subtypes = setOf(Subtype("Soldier")),
            power = 2,
            toughness = 4,
            script = CardScript.creature(staticAbilities = listOf(CanBlockAnyNumber())),
        )
        val testBear = CardDefinition.creature(
            name = "Test Bear",
            manaCost = ManaCost.parse("{1}{G}"),
            subtypes = setOf(Subtype("Bear")),
            power = 2,
            toughness = 2,
        )
        driver.registerCard(testCrusher)
        driver.registerCard(testBear)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val attacker = driver.activePlayer!!
        val defender = if (attacker == driver.player1) driver.player2 else driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // Two trample attackers: trample forces manual assignment unconditionally
        // (the player must choose drain), guaranteeing both end up in
        // planCandidates and so in the decision's `attackers` list.
        val tusker1 = driver.putCreatureOnBattlefield(attacker, "Trample Beast")   // 5/5 trample
        val tusker2 = driver.putCreatureOnBattlefield(attacker, "Trample Beast")   // 5/5 trample
        val crusher = driver.putCreatureOnBattlefield(defender, "Test Crusher")    // 2/4
        val lions = driver.putCreatureOnBattlefield(defender, "Savannah Lions")    // 2/1
        val bear = driver.putCreatureOnBattlefield(defender, "Test Bear")          // 2/2
        driver.removeSummoningSickness(tusker1)
        driver.removeSummoningSickness(tusker2)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(tusker1, tusker2), defender)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        // Crusher blocks both attackers; Lions on tusker1, Bear on tusker2.
        driver.declareBlockers(
            defender,
            mapOf(
                crusher to listOf(tusker1, tusker2),
                lions to listOf(tusker1),
                bear to listOf(tusker2),
            ),
        )

        // Drain any OrderObjectsDecision pre-steps (CR 510.1d): one per attacker
        // with >1 blocker, one per blocker with >1 attacker.
        advanceUntilDecision(driver)
        var decision: com.wingedsheep.engine.core.PendingDecision? = driver.state.pendingDecision
        while (decision is com.wingedsheep.engine.core.OrderObjectsDecision) {
            driver.submitDecision(
                decision.playerId,
                com.wingedsheep.engine.core.OrderedResponse(decision.id, decision.objects),
            )
            advanceUntilDecision(driver)
            decision = driver.state.pendingDecision
        }

        decision.shouldBeInstanceOf<CombatResolutionDecision>()
        decision.attackers.map { it.id }.toSet() shouldBe setOf(tusker1, tusker2)
        decision.blockers.map { it.id }.toSet() shouldBe setOf(crusher, lions, bear)

        // Trample drain edges present on each tusker.
        val drains = decision.edges.filter { it.isTrampleDrain }
        drains.map { it.sourceId }.toSet() shouldBe setOf(tusker1, tusker2)
        drains.forEach {
            it.direction shouldBe DamageEdgeDirection.ATTACKER_TO_PLAYER
            it.targetId shouldBe defender
        }

        // Bipartite half: crusher (blocking both attackers) emits BLOCKER_TO_ATTACKER
        // edges to each. Single-attacker blockers (lions, bear) do not.
        val fromCrusher = decision.edges.filter { it.sourceId == crusher }
        fromCrusher.size shouldBe 2
        fromCrusher.map { it.direction }.toSet() shouldContainExactly setOf(DamageEdgeDirection.BLOCKER_TO_ATTACKER)
        fromCrusher.map { it.targetId }.toSet() shouldBe setOf(tusker1, tusker2)

        decision.edges.filter { it.sourceId == lions }.size shouldBe 0
        decision.edges.filter { it.sourceId == bear }.size shouldBe 0

        // Defender owns the blocker-side edges by default (CR 510.1d).
        fromCrusher.forEach { it.editableBy shouldBe defender }
    }

    /**
     * Pure bipartite — one Ironfist-style crusher blocks both attackers, neither
     * attacker has trample/banding, and neither requires manual assignment on
     * its own. The blocker still needs to divide its 2 damage between the two
     * attackers, so the board must emit a decision (engine gap fixed in
     * `CombatDamageManager.attackersWithBipartiteBlocker`).
     */
    test("pure bipartite (no trample, no banding) still emits a CombatResolutionDecision") {
        val driver = createDriver()
        val testCrusher = CardDefinition.creature(
            name = "Test Crusher",
            manaCost = ManaCost.parse("{4}{W}"),
            subtypes = setOf(Subtype("Soldier")),
            power = 2,
            toughness = 4,
            script = CardScript.creature(staticAbilities = listOf(CanBlockAnyNumber())),
        )
        driver.registerCard(testCrusher)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val attacker = driver.activePlayer!!
        val defender = if (attacker == driver.player1) driver.player2 else driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val courser1 = driver.putCreatureOnBattlefield(attacker, "Centaur Courser") // 3/3
        val courser2 = driver.putCreatureOnBattlefield(attacker, "Centaur Courser") // 3/3
        val crusher = driver.putCreatureOnBattlefield(defender, "Test Crusher")     // 2/4
        driver.removeSummoningSickness(courser1)
        driver.removeSummoningSickness(courser2)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(courser1, courser2), defender)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(defender, mapOf(crusher to listOf(courser1, courser2)))

        advanceUntilDecision(driver)
        var decision: com.wingedsheep.engine.core.PendingDecision? = driver.state.pendingDecision
        while (decision is com.wingedsheep.engine.core.OrderObjectsDecision) {
            driver.submitDecision(
                decision.playerId,
                com.wingedsheep.engine.core.OrderedResponse(decision.id, decision.objects),
            )
            advanceUntilDecision(driver)
            decision = driver.state.pendingDecision
        }

        decision.shouldBeInstanceOf<CombatResolutionDecision>()
        // Both attackers must be in the decision so the crusher's blocker→attacker
        // edges have valid target nodes.
        decision.attackers.map { it.id }.toSet() shouldBe setOf(courser1, courser2)
        decision.blockers.map { it.id } shouldBe listOf(crusher)

        val fromCrusher = decision.edges.filter { it.sourceId == crusher }
        fromCrusher.size shouldBe 2
        fromCrusher.map { it.direction }.toSet() shouldContainExactly setOf(DamageEdgeDirection.BLOCKER_TO_ATTACKER)
        fromCrusher.map { it.targetId }.toSet() shouldBe setOf(courser1, courser2)
        fromCrusher.forEach { it.editableBy shouldBe defender }
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
