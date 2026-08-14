package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Wilderland Scrounger (HOB) — {4}{G} Creature — Wolf 3/6.
 * "Ferocious — Whenever this creature attacks while you control a creature with power 4 or
 *  greater, put a +1/+1 counter on each creature you control."
 *
 * "Each creature you control" has to include the Scrounger and the creature that satisfied
 * ferocious, has to include creatures that stayed home, and has to exclude the opponent's board.
 */
class WilderlandScroungerScenarioTest : ScenarioTestBase() {

    private fun counters(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Wilderland Scrounger") {

            test("attacking with ferocious counters up every creature you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wilderland Scrounger")
                    .withCardOnBattlefield(1, "Force of Nature")
                    // Stays home — "each creature you control" still reaches it.
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val scrounger = game.findPermanent("Wilderland Scrounger")!!
                val ally = game.findPermanent("Force of Nature")!!
                val homebody = game.findPermanent("Centaur Courser")!!
                val theirs = game.findPermanent("Grizzly Bears")!!

                game.declareAttackers(mapOf("Wilderland Scrounger" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the Scrounger counters itself") { counters(game, scrounger) shouldBe 1 }
                withClue("the ferocious enabler is counted too") { counters(game, ally) shouldBe 1 }
                withClue("a creature that stayed home still gets one") {
                    counters(game, homebody) shouldBe 1
                }
                withClue("the opponent's creature gets nothing") { counters(game, theirs) shouldBe 0 }
                withClue("the counters show through the projection — 3/6 becomes 4/7") {
                    game.state.projectedState.getPower(scrounger) shouldBe 4
                    game.state.projectedState.getToughness(scrounger) shouldBe 7
                }
            }

            test("without a power-4 creature nothing gets a counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wilderland Scrounger")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val scrounger = game.findPermanent("Wilderland Scrounger")!!
                val courser = game.findPermanent("Centaur Courser")!!

                game.declareAttackers(mapOf("Wilderland Scrounger" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the Scrounger's own power is 3, and the Courser's is 3") {
                    counters(game, scrounger) shouldBe 0
                    counters(game, courser) shouldBe 0
                }
            }
        }
    }
}
