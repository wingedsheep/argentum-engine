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
 * Daydream {W} Sorcery — "Exile target creature you control, then return that card to the
 * battlefield under its owner's control with a +1/+1 counter on it. Flashback {2}{W}."
 *
 * Covers the blink-with-counter resolution and the flashback re-cast from the graveyard.
 */
class DaydreamScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Daydream — flicker a creature and return it with a +1/+1 counter") {

            test("the targeted creature returns with one +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Daydream")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Daydream", targetId = bears).error shouldBe null
                game.resolveStack()

                val returned = game.findPermanent("Grizzly Bears")
                withClue("Grizzly Bears should be back on the battlefield") {
                    (returned != null) shouldBe true
                }
                withClue("It returns with exactly one +1/+1 counter") {
                    plusOneCounters(game, returned!!) shouldBe 1
                }
            }

            test("Daydream can be flashed back from the graveyard for {2}{W}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Daydream")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpellFromGraveyard(1, "Daydream", targetId = bears).error shouldBe null
                game.resolveStack()

                val returned = game.findPermanent("Grizzly Bears")!!
                withClue("Flashback blink also returns the creature with a +1/+1 counter") {
                    plusOneCounters(game, returned) shouldBe 1
                }
                withClue("Flashback exiles Daydream, so it is no longer in the graveyard") {
                    game.isInGraveyard(1, "Daydream") shouldBe false
                }
            }
        }
    }
}
