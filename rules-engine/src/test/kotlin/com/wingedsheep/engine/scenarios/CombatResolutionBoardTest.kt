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

    /**
     * CR 702.22k: when an attacking creature has banding, the active player
     * chooses the damage division for the blockers blocking it AND can ignore
     * the damage-assignment order. So the BLOCKER_TO_ATTACKER edges should
     * have `minimum = 0` (not the lethal threshold) so the active player can
     * dump all of a blocker's damage on a single band member.
     *
     * Before the fix the validator rejected such submissions with
     * "amount X below minimum Y".
     */
    test("banding attacker: BLOCKER_TO_ATTACKER edges have minimum=0 (lethal-first bypassed)") {
        val driver = createDriver()
        val bandingKnight = CardDefinition.creature(
            name = "Test Banding Knight",
            manaCost = ManaCost.parse("{2}{W}"),
            subtypes = setOf(Subtype("Knight")),
            power = 2,
            toughness = 2,
            keywords = setOf(com.wingedsheep.sdk.core.Keyword.BANDING),
        )
        driver.registerCard(bandingKnight)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val attacker = driver.activePlayer!!
        val defender = if (attacker == driver.player1) driver.player2 else driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // Two banded attackers (one banding, one not) — CR 702.22c allows a
        // banding creature to band with up to one non-banding attacker.
        val knight1 = driver.putCreatureOnBattlefield(attacker, "Test Banding Knight")
        val knight2 = driver.putCreatureOnBattlefield(attacker, "Test Banding Knight")
        val giant1 = driver.putCreatureOnBattlefield(defender, "Trample Beast")  // 5/5 trample, used as 5-power blocker
        val giant2 = driver.putCreatureOnBattlefield(defender, "Trample Beast")
        driver.removeSummoningSickness(knight1)
        driver.removeSummoningSickness(knight2)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(
            com.wingedsheep.engine.core.DeclareAttackers(
                attacker,
                mapOf(knight1 to defender, knight2 to defender),
                bands = listOf(setOf(knight1, knight2)),
            ),
        )
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        // Per CR 702.22h: blocking any band member blocks the entire band.
        // Both blockers naturally block the band.
        driver.declareBlockers(defender, mapOf(giant1 to listOf(knight1), giant2 to listOf(knight2)))

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
        val blockerEdges = decision.edges.filter {
            it.direction == DamageEdgeDirection.BLOCKER_TO_ATTACKER
        }
        // With banding active on at least one blocked attacker, every blocker
        // edge should have minimum=0 — the active player can ignore the
        // damage-assignment order (CR 702.22k).
        blockerEdges.forEach { edge ->
            edge.minimum shouldBe 0
        }
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

    /**
     * CR 510.1d edge case: a non-trample attacker whose total power is less
     * than its first blocker's lethal threshold. The attacker must be allowed
     * to assign its full power to that blocker (with 0 on later blockers).
     *
     * Pre-fix, the emitter set `minimum = lethal` and `maximum = power` on
     * the blocker edges, producing an unsatisfiable constraint that rejected
     * even the engine's own default. The fix: blocker edges have
     * `minimum = 0`; CR 510.1d is enforced relationally in the validator via
     * `lethalThreshold`.
     */
    test("non-trample attacker with power < blocker lethal: defaults validate cleanly") {
        val driver = createDriver()
        val testCrusher = CardDefinition.creature(
            name = "Test Crusher",
            manaCost = ManaCost.parse("{4}{W}"),
            subtypes = setOf(Subtype("Soldier")),
            power = 2,
            toughness = 4,
            script = CardScript.creature(staticAbilities = listOf(CanBlockAnyNumber())),
        )
        val small = CardDefinition.creature(
            name = "Test Small Attacker",
            manaCost = ManaCost.parse("{2}{R}"),
            subtypes = setOf(Subtype("Cat")),
            power = 3,
            toughness = 2,
        )
        driver.registerCard(testCrusher)
        driver.registerCard(small)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val attacker = driver.activePlayer!!
        val defender = if (attacker == driver.player1) driver.player2 else driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // A trample attacker forces the resolution board to emit (so we can
        // inspect edges). The second attacker has 3 power and faces two
        // 4-toughness blockers — the case this test guards.
        val tusker = driver.putCreatureOnBattlefield(attacker, "Trample Beast")
        val smallId = driver.putCreatureOnBattlefield(attacker, "Test Small Attacker")
        val crusher1 = driver.putCreatureOnBattlefield(defender, "Test Crusher")
        val crusher2 = driver.putCreatureOnBattlefield(defender, "Test Crusher")
        driver.removeSummoningSickness(tusker)
        driver.removeSummoningSickness(smallId)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(tusker, smallId), defender)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(
            defender,
            mapOf(
                crusher1 to listOf(tusker, smallId),
                crusher2 to listOf(tusker, smallId),
            ),
        )

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

        // The small attacker's blocker edges: minimum=0, maximum=3 (its power),
        // lethalThreshold=4 (Crusher's toughness). The constraint is satisfiable.
        val smallEdges = decision.edges.filter {
            it.sourceId == smallId && it.direction == DamageEdgeDirection.ATTACKER_TO_BLOCKER
        }
        smallEdges.size shouldBe 2
        smallEdges.forEach { edge ->
            edge.minimum shouldBe 0
            edge.maximum shouldBe 3
            edge.lethalThreshold shouldBe 4
        }

        // The engine's defaults validate cleanly (this is what the frontend
        // submits when the player doesn't edit).
        val response = CombatResolutionResponse(
            decisionId = decision.id,
            edges = decision.edges.map { DamageEdgeAmount(it.id, it.amount) },
        )
        driver.submitDecision(decision.playerId, response).isSuccess shouldBe true
    }

    /**
     * Banding cross-band damage cooperation (CR 702.22j/k).
     *
     * The active player divides each banded attacker's outgoing damage
     * among the band's blockers ignoring damage-assignment order
     * (CR 702.22j, attacker-side bypass — symmetric to the long-standing
     * CR 702.22k bypass on blocker-to-attacker damage). The chooser stays
     * the active player; only the order constraint is lifted. The
     * non-banding member of the band (CR 702.22c) inherits the bypass
     * because the band as a whole contains a banding creature.
     *
     * Scenario:
     *
     *   Attackers (banded): NE1 2/2 banding, NE2 2/2 banding, Bear 2/2
     *   Blockers          : Giant1 3/3, Giant2 3/3, defBear 2/2
     *
     * Per CR 702.21g, blocking any band member blocks the entire band.
     *
     * Damage plan:
     *   NE1     → 2 to Giant1               (non-lethal on its own)
     *   NE2     → 1 to Giant1, 1 to Giant2  (cooperates: G1 hits 3 = lethal,
     *                                        G2 carries 1 toward lethal)
     *   atkBear → 2 to Giant2               (G2 hits 3 = lethal)
     *   blockers (3+3+2 = 8 incoming damage) all → atkBear (CR 702.22k
     *                                        overassignment onto one band
     *                                        member).
     *
     * Outcome: both giants die, atkBear dies (sponge), both NEs survive.
     */
    test("banding overassignment: 3-attacker band cooperates to kill two 3/3 blockers") {
        val driver = createDriver()
        val bandingElephant = CardDefinition.creature(
            name = "Test Banding Elephant",
            manaCost = ManaCost.parse("{2}{W}"),
            subtypes = setOf(Subtype("Elephant")),
            power = 2,
            toughness = 2,
            keywords = setOf(com.wingedsheep.sdk.core.Keyword.BANDING),
        )
        val testBear = CardDefinition.creature(
            name = "Test Bear",
            manaCost = ManaCost.parse("{1}{G}"),
            subtypes = setOf(Subtype("Bear")),
            power = 2,
            toughness = 2,
        )
        val testGiant = CardDefinition.creature(
            name = "Test Giant",
            manaCost = ManaCost.parse("{3}{R}"),
            subtypes = setOf(Subtype("Giant")),
            power = 3,
            toughness = 3,
        )
        driver.registerCard(bandingElephant)
        driver.registerCard(testBear)
        driver.registerCard(testGiant)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val attacker = driver.activePlayer!!
        val defender = if (attacker == driver.player1) driver.player2 else driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val ne1 = driver.putCreatureOnBattlefield(attacker, "Test Banding Elephant")
        val ne2 = driver.putCreatureOnBattlefield(attacker, "Test Banding Elephant")
        // CR 702.22c: one non-banding creature may band with banding attackers.
        val atkBear = driver.putCreatureOnBattlefield(attacker, "Test Bear")
        val giant1 = driver.putCreatureOnBattlefield(defender, "Test Giant")
        val giant2 = driver.putCreatureOnBattlefield(defender, "Test Giant")
        val defBear = driver.putCreatureOnBattlefield(defender, "Test Bear")
        driver.removeSummoningSickness(ne1)
        driver.removeSummoningSickness(ne2)
        driver.removeSummoningSickness(atkBear)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(
            com.wingedsheep.engine.core.DeclareAttackers(
                attacker,
                mapOf(ne1 to defender, ne2 to defender, atkBear to defender),
                bands = listOf(setOf(ne1, ne2, atkBear)),
            ),
        )
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        // Defender assigns one blocker per attacker; per CR 702.21g this
        // expands so each blocker blocks the whole band.
        driver.declareBlockers(
            defender,
            mapOf(
                giant1 to listOf(ne1),
                giant2 to listOf(ne2),
                defBear to listOf(atkBear),
            ),
        )

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
        decision.attackers.map { it.id }.toSet() shouldBe setOf(ne1, ne2, atkBear)
        decision.blockers.map { it.id }.toSet() shouldBe setOf(giant1, giant2, defBear)

        // The user-described cooperative distribution (see test doc).
        val plan: Map<Pair<com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId>, Int> =
            mapOf(
                (ne1 to giant1) to 2,
                (ne2 to giant1) to 1,
                (ne2 to giant2) to 1,
                (atkBear to giant2) to 2,
                // CR 702.22k overassignment: all blocker damage onto atkBear.
                (giant1 to atkBear) to 3,
                (giant2 to atkBear) to 3,
                (defBear to atkBear) to 2,
            )
        val customEdges = decision.edges.map { edge ->
            DamageEdgeAmount(edge.id, plan[edge.sourceId to edge.targetId] ?: 0)
        }
        val response = CombatResolutionResponse(decisionId = decision.id, edges = customEdges)
        driver.submitDecision(decision.playerId, response).isSuccess shouldBe true

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        val battlefield = driver.state.getBattlefield()
        battlefield.contains(giant1) shouldBe false
        battlefield.contains(giant2) shouldBe false
        battlefield.contains(atkBear) shouldBe false
        battlefield.contains(ne1) shouldBe true
        battlefield.contains(ne2) shouldBe true
        battlefield.contains(defBear) shouldBe true
    }
})
