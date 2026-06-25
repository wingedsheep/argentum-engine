package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Reluctant Role Model (DSK #26) — {1}{W} 2/2 Creature — Human Survivor.
 *
 * "Survival — At the beginning of your second main phase, if this creature is tapped, put a
 *  flying, lifelink, or +1/+1 counter on it.
 *  Whenever this creature or another creature you control dies, if it had counters on it, put
 *  those counters on up to one target creature."
 *
 * (a) The Survival trigger is an intervening-if postcombat-main trigger gated on the source being
 *     tapped; the payoff is a `ChooseAction` over a flying / lifelink / +1/+1 counter.
 * (b) The dies trigger uses `Conditions.TriggeringEntityHadCounters` + `MoveAllLastKnownCounters`
 *     to relocate the dying creature's counters onto up to one target creature.
 */
class ReluctantRoleModelScenarioTest : ScenarioTestBase() {

    init {
        fun counter(game: TestGame, id: com.wingedsheep.sdk.model.EntityId, type: CounterType): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

        context("Reluctant Role Model — Survival counter choice") {

            test("tapped at second main phase lets you put a chosen +1/+1 counter on it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Reluctant Role Model", tapped = true, summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rrm = game.findPermanent("Reluctant Role Model")!!

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                var guard = 0
                while (game.getPendingDecision() !is ChooseOptionDecision && guard++ < 20) {
                    game.resolveStack()
                }

                val decision = game.getPendingDecision() as? ChooseOptionDecision
                    ?: error("expected a ChooseOptionDecision for the counter choice; got ${game.getPendingDecision()}")
                // Options: [0]=flying, [1]=lifelink, [2]=+1/+1. Pick +1/+1.
                game.submitDecision(OptionChosenResponse(decision.id, 2))
                game.resolveStack()

                withClue("A +1/+1 counter was placed on Reluctant Role Model") {
                    counter(game, rrm, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
            }

            test("untapped at second main phase does not trigger Survival") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Reluctant Role Model", tapped = false, summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rrm = game.findPermanent("Reluctant Role Model")!!

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Intervening-if (this creature is tapped) is false — no counter and no decision") {
                    game.hasPendingDecision() shouldBe false
                    counter(game, rrm, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
                    counter(game, rrm, CounterType.FLYING) shouldBe 0
                    counter(game, rrm, CounterType.LIFELINK) shouldBe 0
                }
            }
        }

        context("Reluctant Role Model — dies, move counters") {

            test("when it dies with counters, they move to up to one target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Reluctant Role Model", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rrm = game.findPermanent("Reluctant Role Model")!!
                val bear = game.findPermanent("Grizzly Bears")!!

                // Stamp two +1/+1 counters (RRM becomes 4/4) so a single 3-damage bolt won't kill
                // it — use lethal another way: give it just one counter so a 3-damage bolt kills the
                // resulting 3/3.
                game.state = game.state.updateEntity(rrm) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }

                game.castSpell(1, "Lightning Bolt", rrm)
                game.resolveStack()

                withClue("Reluctant Role Model is dead") {
                    game.findPermanent("Reluctant Role Model") shouldBe null
                }

                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(bear))
                    game.resolveStack()
                }

                withClue("Its +1/+1 counter moved onto the Bear") {
                    counter(game, bear, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }
            }

            test("a creature that died with no counters does not trigger the move") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Reluctant Role Model", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Grizzly Bears")!!

                // Kill the counterless Bear (a creature you control) — intervening-if is false.
                game.castSpell(1, "Lightning Bolt", bear)
                game.resolveStack()

                withClue("Bear is dead") { game.findPermanent("Grizzly Bears") shouldBe null }
                withClue("No pending target decision — 'if it had counters' is false") {
                    game.hasPendingDecision() shouldBe false
                }
            }
        }
    }
}
