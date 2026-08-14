package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Nasty Little Rabbit (HOB) — {G} Creature — Rabbit 1/2.
 * "Ferocious — At the beginning of combat on your turn, if you control a creature with power 4 or
 *  greater, put a +1/+1 counter on this creature."
 *
 * A begin-combat trigger with an intervening-if. Both halves matter: the condition, and "on *your*
 * turn" — the trigger must not fire during the opponent's combat.
 */
class NastyLittleRabbitScenarioTest : ScenarioTestBase() {

    private fun counters(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Nasty Little Rabbit") {

            test("with a power-4 creature it gets a +1/+1 counter at begin combat") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nasty Little Rabbit")
                    .withCardOnBattlefield(1, "Force of Nature")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rabbit = game.findPermanent("Nasty Little Rabbit")!!
                counters(game, rabbit) shouldBe 0

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                withClue("ferocious satisfied — one +1/+1 counter") {
                    counters(game, rabbit) shouldBe 1
                }
                withClue("the counter shows through the projection: a 2/3") {
                    game.state.projectedState.getPower(rabbit) shouldBe 2
                    game.state.projectedState.getToughness(rabbit) shouldBe 3
                }
            }

            test("without a power-4 creature it gets nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nasty Little Rabbit")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rabbit = game.findPermanent("Nasty Little Rabbit")!!
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                withClue("a 3/3 does not meet 'power 4 or greater'") {
                    counters(game, rabbit) shouldBe 0
                    game.state.projectedState.getPower(rabbit) shouldBe 1
                }
            }

            test("it does not trigger during the opponent's combat") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nasty Little Rabbit")
                    .withCardOnBattlefield(1, "Force of Nature")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rabbit = game.findPermanent("Nasty Little Rabbit")!!
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                withClue("'at the beginning of combat on your turn' — not the opponent's") {
                    counters(game, rabbit) shouldBe 0
                }
            }
        }
    }
}
