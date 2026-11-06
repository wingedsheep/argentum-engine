package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Counters moved or removed whatever their kind — `MoveAllCountersEffect` (The Ozolith),
 * `MoveAllLastKnownCountersEffect` (Essence Channeler) and `RemoveAnyNumberOfCountersEffect`
 * (Etched Slith, Price of Betrayal). The counters carry the meaning, so the slot reads FLEXIBLE and
 * the simulation weighs the counters each candidate holds: taking -1/-1 counters off our creature
 * is a gift, taking +1/+1 counters off theirs a loss for them.
 *
 * Target choice already simulated both sides while the slot was unread; the two Price of Betrayal
 * puzzles pin that it still does now that it is read — a fixed HARMFUL or BENEFICIAL reading would
 * fail one of them. What the reading newly changes is the audit and the counter carriers' synergy.
 */
class CounterManipulationReadingAiTest : ScenarioTestBase() {
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun TestGame.withCounters(cardName: String, controller: Int, type: CounterType, count: Int) {
        val id = state.getBattlefield().first { entity ->
            state.getEntity(entity)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == cardName &&
                state.getEntity(entity)?.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId ==
                (if (controller == 1) player1Id else player2Id)
        }
        state = state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withCounters(type, count))
        }
    }

    private fun priceOfBetrayal(id: String, ours: CounterType?, theirs: CounterType?, expectTarget: String, avoid: String) =
        AiPuzzle(
            id = id, category = PuzzleCategory.LAST_CHANCE,
            expectation = "Remove the counters that hurt us or help them", aiSeat = 1,
            position = { s ->
                s.withPlayers().withActivePlayer(1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInHand(1, "Price of Betrayal")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .build().also { game ->
                        ours?.let { game.withCounters("Hill Giant", 1, it, 2) }
                        theirs?.let { game.withCounters("Grizzly Bears", 2, it, 2) }
                        game.advanceToPriority(1, Step.PRECOMBAT_MAIN)
                    }
            },
            check = {
                shouldCast("Price of Betrayal")
                shouldTarget(expectTarget)
                shouldNotTarget(avoid)
            },
        )

    init {
        test("counter moves and removals read both sides open, and the cards built on them read fully") {
            val ozolith = cardRegistry.requireCard("The Ozolith")
            val combat = ozolith.script.triggeredAbilities[1]
            TargetPolarityAnalyzer.forRequirements(combat.effect, combat.allTargetRequirements) shouldBe
                listOf(TargetPolarity.FLEXIBLE)
            TargetPolarityAnalyzer.forSpell(cardRegistry.requireCard("Price of Betrayal")) shouldBe
                listOf(TargetPolarity.FLEXIBLE)
            for (name in listOf("The Ozolith", "Etched Slith", "Essence Channeler", "Price of Betrayal")) {
                val reading = AiReadingAudit.read(cardRegistry.requireCard(name))
                withClue("$name: ${reading}") { reading.readFully shouldBe true }
            }
        }

        test("adding or removing one counter of each kind on a target reads both sides open") {
            // Dramatist's Puppet, Quarry Hauler: the kinds are whatever the target carries, so the
            // slot is FLEXIBLE like the moves above, and the per-kind steps need no intent of their own.
            for (name in listOf("Dramatist's Puppet", "Quarry Hauler")) {
                val card = cardRegistry.requireCard(name)
                val etb = card.script.triggeredAbilities.single()
                withClue(name) {
                    TargetPolarityAnalyzer.forRequirements(etb.effect, etb.allTargetRequirements) shouldBe
                        listOf(TargetPolarity.FLEXIBLE)
                }
                val reading = AiReadingAudit.read(card)
                withClue("$name: $reading") { reading.readFully shouldBe true }
            }
        }

        test("a card that carries counters on rewards a deck that puts +1/+1 counters down") {
            for (name in listOf("The Ozolith", "Essence Channeler")) {
                withClue(name) { SynergyAxes.of(cardRegistry.requireCard(name)).rewards shouldContain SynergyAxis.PLUS_COUNTERS }
            }
        }

        test("Price of Betrayal takes -1/-1 counters off our own creature") {
            val result = runner.run(
                priceOfBetrayal("remove-own-minus-counters", CounterType.MINUS_ONE_MINUS_ONE, null, "Hill Giant", "Grizzly Bears"),
                AiProfile.LIVE,
            )
            withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
        }

        test("Price of Betrayal takes +1/+1 counters off their creature, not ours") {
            val result = runner.run(
                priceOfBetrayal("remove-their-plus-counters", CounterType.PLUS_ONE_PLUS_ONE, CounterType.PLUS_ONE_PLUS_ONE, "Grizzly Bears", "Hill Giant"),
                AiProfile.LIVE,
            )
            withClue("chose ${result.move}: ${result.failure}") { result.passed shouldBe true }
        }
    }
}
