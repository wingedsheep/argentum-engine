package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Moldgraf Millipede (VOW #209) — {4}{G} Creature — Insect Horror, 2/2.
 *
 *   When this creature enters, mill three cards, then put a +1/+1 counter on this creature for
 *   each creature card in your graveyard.
 *
 * Exercises the ETB mill-then-count: the library is seeded with exactly three cards so the mill
 * consumes the whole seeded set deterministically (no ambiguity about "top of library" order),
 * and the resulting +1/+1 counter total is checked against how many of those milled cards (plus
 * any creature cards already in the graveyard) are creatures.
 */
class MoldgrafMillipedeScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Moldgraf Millipede — mill three, then a +1/+1 counter per creature card in graveyard") {

            test("milling two creatures and a land nets two +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Moldgraf Millipede")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("the library starts with exactly the three seeded cards") {
                    game.librarySize(1) shouldBe 3
                }

                game.castSpell(1, "Moldgraf Millipede").error shouldBe null
                game.resolveStack()

                val millipede = game.findPermanent("Moldgraf Millipede")!!

                withClue("all three seeded cards were milled to the graveyard") {
                    game.librarySize(1) shouldBe 0
                    game.graveyardSize(1) shouldBe 3
                }
                withClue("two of the three milled cards are creatures -> two +1/+1 counters") {
                    plusOneCounters(game, millipede) shouldBe 2
                }
            }

            test("a creature already in the graveyard also counts toward the total") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Moldgraf Millipede")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Moldgraf Millipede").error shouldBe null
                game.resolveStack()

                val millipede = game.findPermanent("Moldgraf Millipede")!!

                withClue("no new creature cards were milled, but the pre-existing one still counts") {
                    plusOneCounters(game, millipede) shouldBe 1
                }
            }
        }
    }
}
