package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Wild Hypothesis {X}{G} Sorcery — "Create a 0/0 green and blue Fractal creature token. Put X
 * +1/+1 counters on it. Surveil 2."
 *
 * Verifies the Fractal token enters with X +1/+1 counters and that the trailing Surveil 2
 * pauses for the look-at-top-two choice.
 */
class WildHypothesisScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Wild Hypothesis — Fractal token with X +1/+1 counters, then Surveil 2") {

            test("X=3 makes a 0/0 Fractal with three +1/+1 counters, then surveils") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wild Hypothesis")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Wild Hypothesis", xValue = 3).error shouldBe null
                game.resolveStack()

                val fractal = game.findPermanent("Fractal Token")
                withClue("A Fractal token should have been created") {
                    (fractal != null) shouldBe true
                }
                withClue("It enters with X = 3 +1/+1 counters (0/0 base)") {
                    plusOneCounters(game, fractal!!) shouldBe 3
                }

                // Surveil 2 pauses for the put-in-graveyard / keep-on-top choice; keep both on top.
                withClue("Surveil 2 should raise a card-selection decision") {
                    (game.state.pendingDecision != null) shouldBe true
                }
                game.skipSelection().error shouldBe null
            }

            test("X=0 makes a 0/0 Fractal with no counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wild Hypothesis")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Wild Hypothesis", xValue = 0).error shouldBe null
                game.resolveStack()

                val fractal = game.findPermanent("Fractal Token")!!
                withClue("X = 0 → no +1/+1 counters on the 0/0 Fractal") {
                    plusOneCounters(game, fractal) shouldBe 0
                }
                game.skipSelection().error shouldBe null
            }
        }
    }
}
