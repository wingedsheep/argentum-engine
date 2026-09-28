package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Fairgrounds Trumpeter (KLD #155, reprinted MOM #335) — {2}{G} Creature —
 * Elephant, 2/2.
 *
 *   At the beginning of each end step, if a +1/+1 counter was put on a permanent under your control
 *   this turn, put a +1/+1 counter on this creature.
 *
 * The intervening-if is keyed on the controller of the permanent that received the counter, so a
 * +1/+1 counter you put on an opponent's creature does not satisfy it, and one on any of your
 * permanents does.
 */
class FairgroundsTrumpeterScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.battlegrowth(target: EntityId) {
        val spell = findCardsInHand(1, "Battlegrowth").first()
        execute(CastSpell(player1Id, spell, listOf(entityIdToChosenTarget(state, target)))).error shouldBe null
        resolveStack()
    }

    private fun TestGame.throughEndStep() {
        passUntilPhase(Phase.ENDING, Step.END)
        resolveStack()
    }

    init {
        context("Fairgrounds Trumpeter's end-step counter") {

            test("does nothing when no +1/+1 counter was put on your permanents this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Fairgrounds Trumpeter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val trumpeter = game.findPermanent("Fairgrounds Trumpeter")!!

                game.throughEndStep()

                game.plusOneCounters(trumpeter) shouldBe 0
            }

            test("gets a counter after a +1/+1 counter went on another permanent you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Fairgrounds Trumpeter")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Battlegrowth")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val trumpeter = game.findPermanent("Fairgrounds Trumpeter")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.battlegrowth(bears)
                game.throughEndStep()

                withClue("the counter went on the Bears; the Trumpeter still gets one at end of turn") {
                    game.plusOneCounters(trumpeter) shouldBe 1
                }
            }

            test("a +1/+1 counter you put on an opponent's creature does not count") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Fairgrounds Trumpeter")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Battlegrowth")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val trumpeter = game.findPermanent("Fairgrounds Trumpeter")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.battlegrowth(bears)
                game.throughEndStep()

                withClue("the Bears are the opponent's, so the condition is false") {
                    game.plusOneCounters(bears) shouldBe 1
                    game.plusOneCounters(trumpeter) shouldBe 0
                }
            }

            test("a counter on the Trumpeter itself counts, and the trigger adds another") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Fairgrounds Trumpeter")
                    .withCardInHand(1, "Battlegrowth")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val trumpeter = game.findPermanent("Fairgrounds Trumpeter")!!

                game.battlegrowth(trumpeter)
                game.throughEndStep()

                game.plusOneCounters(trumpeter) shouldBe 2
            }
        }
    }
}
