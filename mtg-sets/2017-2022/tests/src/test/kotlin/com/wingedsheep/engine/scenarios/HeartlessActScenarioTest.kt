package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Heartless Act (Ikoria: Lair of Behemoths #91).
 *
 * Heartless Act ({1}{B} Instant), Choose one —
 *   • Destroy target creature with no counters on it.
 *   • Remove up to three counters from target creature.
 *
 * Exercises both modes: the "no counters" target restriction on the destroy mode (composed from
 * `StatePredicate.Not(HasAnyCounter)` via `TargetFilter.withoutCounters`), and the
 * total-budget-capped `RemoveCountersUpTo(3)` removal — including the cross-kind budget cap.
 */
class HeartlessActScenarioTest : ScenarioTestBase() {

    private fun addCounters(game: TestGame, id: EntityId, vararg counters: Pair<CounterType, Int>) {
        game.state = game.state.updateEntity(id) { container ->
            var comp = container.get<CountersComponent>() ?: CountersComponent()
            counters.forEach { (type, amount) -> comp = comp.withAdded(type, amount) }
            container.with(comp)
        }
    }

    private fun count(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun totalCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.counters?.values?.sum() ?: 0

    init {
        context("Heartless Act — choose one") {

            test("mode 1: destroys a creature with no counters on it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Heartless Act")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpellWithMode(1, "Heartless Act", modeIndex = 0, targetId = bears).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears (no counters) is destroyed") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("mode 1: a creature with a counter is not a legal target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Heartless Act")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                addCounters(game, bears, CounterType.PLUS_ONE_PLUS_ONE to 1)

                val result = game.castSpellWithMode(1, "Heartless Act", modeIndex = 0, targetId = bears)
                withClue("A creature with a +1/+1 counter cannot be the destroy mode's target") {
                    (result.error != null) shouldBe true
                }
                withClue("It is not destroyed (the illegal cast was rejected)") {
                    (game.findPermanent("Grizzly Bears") != null) shouldBe true
                }
            }

            test("mode 2: removes up to three counters of a single kind (capped at three)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Heartless Act")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                addCounters(game, bears, CounterType.PLUS_ONE_PLUS_ONE to 5)

                game.castSpellWithMode(1, "Heartless Act", modeIndex = 1, targetId = bears).error shouldBe null
                game.resolveStack()

                // One prompt for the +1/+1 kind, capped at the budget of 3.
                val decision = game.getPendingDecision()
                withClue("The first (and only) prompt is a ChooseNumber capped at the budget (3)") {
                    (decision is ChooseNumberDecision) shouldBe true
                    (decision as ChooseNumberDecision).maxValue shouldBe 3
                }
                game.chooseNumber(3).error shouldBe null

                withClue("Three of the five +1/+1 counters were removed; two remain") {
                    count(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                }
            }

            test("mode 2: removes fewer than three when the creature has fewer counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Heartless Act")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                addCounters(game, bears, CounterType.PLUS_ONE_PLUS_ONE to 2)

                game.castSpellWithMode(1, "Heartless Act", modeIndex = 1, targetId = bears).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision() as ChooseNumberDecision
                withClue("The prompt is capped at the two counters present, not the budget of 3") {
                    decision.maxValue shouldBe 2
                }
                game.chooseNumber(2).error shouldBe null

                withClue("Both counters removed") {
                    count(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
                }
            }

            test("mode 2: the three-counter cap is a total across kinds") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Heartless Act")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                // Two kinds, two each = four counters total.
                addCounters(
                    game,
                    bears,
                    CounterType.PLUS_ONE_PLUS_ONE to 2,
                    CounterType.STUN to 2,
                )
                withClue("Setup: four counters total across two kinds") {
                    totalCounters(game, bears) shouldBe 4
                }

                game.castSpellWithMode(1, "Heartless Act", modeIndex = 1, targetId = bears).error shouldBe null
                game.resolveStack()

                // Answer each per-kind prompt by removing as many as the cap allows; the engine
                // caps each prompt at the *remaining* budget, so only three counters can be removed.
                var prompts = 0
                while (game.getPendingDecision() is ChooseNumberDecision) {
                    val d = game.getPendingDecision() as ChooseNumberDecision
                    game.chooseNumber(d.maxValue).error shouldBe null
                    if (prompts++ > 5) error("too many prompts — budget not enforced")
                }

                withClue("Exactly three counters removed in total (budget cap); one remains") {
                    totalCounters(game, bears) shouldBe 1
                }
            }
        }
    }
}
