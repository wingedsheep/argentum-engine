package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.CombatResolutionResponse
import com.wingedsheep.engine.core.DamageEdgeAmount
import com.wingedsheep.engine.core.DamageEdgeDirection
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mirrors manual-scenarios/mechanics/combat-banding-attacking-band.json:
 *
 *   Attacking band : Noble Elephant 2/2 (trample + banding)
 *                    Spined Wurm    5/4 (vanilla)
 *   Blocker        : Panther Warriors 6/3   (lethal = 3)
 *
 * Question: may the attacker assign Noble Elephant's full 2 trample damage to the
 * defending player? Spined Wurm has no trample, so all 5 of its power must go to the
 * blocker (its only legal target) — covering the 3 lethal. That satisfies the CR 702.19b
 * trample lethal-first gate (counting cross-band damage), so the trampler may drain its 2.
 */
class BandingTrampleDrainScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun advanceUntilDecision(driver: GameTestDriver, maxPasses: Int = 50) {
        var passes = 0
        while (driver.state.pendingDecision == null && passes < maxPasses) {
            val priority = driver.state.priorityPlayerId ?: error("No priority and no pending decision")
            driver.submit(PassPriority(priority))
            passes++
            if (driver.state.gameOver) error("Game ended before a decision was emitted")
        }
    }

    test("banded trampler may drain its full 2 to the player when a non-trample band-mate covers lethal") {
        val driver = createDriver()
        val nobleElephant = CardDefinition.creature(
            name = "Test Noble Elephant",
            manaCost = ManaCost.parse("{3}{W}"),
            subtypes = setOf(Subtype("Elephant")),
            power = 2,
            toughness = 2,
            keywords = setOf(Keyword.TRAMPLE, Keyword.BANDING),
        )
        val spinedWurm = CardDefinition.creature(
            name = "Test Spined Wurm",
            manaCost = ManaCost.parse("{4}{G}"),
            subtypes = setOf(Subtype("Wurm")),
            power = 5,
            toughness = 4,
        )
        val panther = CardDefinition.creature(
            name = "Test Panther Warriors",
            manaCost = ManaCost.parse("{3}{G}"),
            subtypes = setOf(Subtype("Cat")),
            power = 6,
            toughness = 3,
        )
        driver.registerCard(nobleElephant)
        driver.registerCard(spinedWurm)
        driver.registerCard(panther)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val attacker = driver.activePlayer!!
        val defender = if (attacker == driver.player1) driver.player2 else driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val ne = driver.putCreatureOnBattlefield(attacker, "Test Noble Elephant")
        val wurm = driver.putCreatureOnBattlefield(attacker, "Test Spined Wurm")
        val pantherId = driver.putCreatureOnBattlefield(defender, "Test Panther Warriors")
        driver.removeSummoningSickness(ne)
        driver.removeSummoningSickness(wurm)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submit(
            DeclareAttackers(
                attacker,
                mapOf(ne to defender, wurm to defender),
                bands = listOf(setOf(ne, wurm)),
            ),
        )
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        // CR 702.22h: blocking one band member blocks the whole band.
        driver.declareBlockers(defender, mapOf(pantherId to listOf(ne)))

        advanceUntilDecision(driver)
        var decision: PendingDecision? = driver.state.pendingDecision
        while (decision is OrderObjectsDecision) {
            driver.submitDecision(decision.playerId, OrderedResponse(decision.id, decision.objects))
            advanceUntilDecision(driver)
            decision = driver.state.pendingDecision
        }

        decision.shouldBeInstanceOf<CombatResolutionDecision>()

        val neDrain = decision.edges.single { it.sourceId == ne && it.isTrampleDrain }
        neDrain.targetId shouldBe defender
        val neToPanther = decision.edges.single {
            it.sourceId == ne && it.direction == DamageEdgeDirection.ATTACKER_TO_BLOCKER
        }
        val wurmToPanther = decision.edges.single {
            it.sourceId == wurm && it.direction == DamageEdgeDirection.ATTACKER_TO_BLOCKER
        }

        fun plan(neToBlk: Int, wurmToBlk: Int, drain: Int): List<DamageEdgeAmount> =
            decision.edges.map { edge ->
                val amount = when (edge.id) {
                    neToPanther.id -> neToBlk
                    wurmToPanther.id -> wurmToBlk
                    neDrain.id -> drain
                    else -> 0
                }
                DamageEdgeAmount(edge.id, amount)
            }

        // Wurm dumps all 5 on Panther (≥ lethal 3); Noble Elephant drains its full 2.
        val response = CombatResolutionResponse(decision.id, plan(neToBlk = 0, wurmToBlk = 5, drain = 2))
        driver.submitDecision(decision.playerId, response).error shouldBe null

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        // Panther (3 toughness) took 5 → dead; defender took 2 trample → 18.
        driver.findPermanent(defender, "Test Panther Warriors") shouldBe null
        driver.assertLifeTotal(defender, 18)
    }
})
