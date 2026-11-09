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
 * Scenario test for Grave Strength (FRF #71) — {1}{B} Sorcery.
 *
 *   Choose target creature. Mill three cards, then put a +1/+1 counter on that creature for each
 *   creature card in your graveyard.
 *
 * Pins that the counters land on the chosen target (not the caster's side by default), that the
 * count is taken after the mill, and that it reads only the caster's graveyard.
 */
class GraveStrengthScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Grave Strength — mill three, then counters on the target per creature card in your graveyard") {

            test("counts freshly milled and pre-existing creature cards in the caster's graveyard only") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Grave Strength")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Swamp")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Grave Strength", giant).error shouldBe null
                game.resolveStack()

                withClue("all three library cards were milled") {
                    game.librarySize(1) shouldBe 0
                    game.graveyardSize(1) shouldBe 5 // 1 Bears + 3 milled + Grave Strength itself
                }
                withClue("one pre-existing + two milled creature cards; the opponent's Bears doesn't count") {
                    plusOneCounters(game, giant) shouldBe 3
                }
            }
        }
    }
}
