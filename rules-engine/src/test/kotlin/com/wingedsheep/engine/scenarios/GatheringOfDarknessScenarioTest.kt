package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gathering of Darkness (HOB) — {3}{B} Sorcery
 *
 * Return up to one target creature card from your graveyard to your hand.
 * Amass Goblins 3.
 *
 * The "up to one" target is the interesting half: the amass must still happen when no target is
 * chosen, and the returned card must be the targeted one.
 */
class GatheringOfDarknessScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Gathering of Darkness") {

            test("returns the targeted creature card and amasses Goblins 3") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Gathering of Darkness")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()

                game.castSpellTargetingGraveyardCard(1, "Gathering of Darkness", listOf(bears))
                    .error shouldBe null
                game.resolveStack()

                withClue("The targeted creature card is back in hand") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }

                val army = game.findAllPermanents("Army Token").firstOrNull()
                    ?: game.state.getBattlefield(game.player1Id).firstOrNull { id ->
                        projector.project(game.state).hasSubtype(id, "Army")
                    }
                    ?: error("amass should have created an Army token")

                val projected = projector.project(game.state)
                withClue("A 0/0 Army with three +1/+1 counters is a 3/3") {
                    projected.getPower(army) shouldBe 3
                }
                withClue("The Army is also a Goblin") {
                    projected.hasSubtype(army, "Goblin") shouldBe true
                }
            }

            test("with no target chosen the spell still amasses") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Gathering of Darkness")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Gathering of Darkness").error shouldBe null
                game.resolveStack()

                val projected = projector.project(game.state)
                val army = game.state.getBattlefield(game.player1Id).firstOrNull { id ->
                    projected.hasSubtype(id, "Army")
                } ?: error("amass should have created an Army token even with an empty graveyard")

                withClue("The Army still got its three counters") {
                    projected.getPower(army) shouldBe 3
                }
            }
        }
    }
}
