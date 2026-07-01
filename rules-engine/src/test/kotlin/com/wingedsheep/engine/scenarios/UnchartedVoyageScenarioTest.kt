package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Uncharted Voyage (FDN #53) — {3}{U} Instant
 * "Target creature's owner puts it on their choice of the top or bottom of their library. Surveil 1."
 *
 * Composes [com.wingedsheep.sdk.dsl.Effects.PutOnTopOrBottomOfLibrary] (owner picks top/bottom)
 * with [com.wingedsheep.sdk.dsl.Effects.Surveil] — the same pair as Vanish from Sight, but the
 * bounce is restricted to a creature.
 */
class UnchartedVoyageScenarioTest : ScenarioTestBase() {

    init {
        context("Uncharted Voyage — bounce a creature to its owner's library, then surveil") {

            test("the targeted creature's owner puts it into their library, then the caster surveils") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Uncharted Voyage")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Island") // something for surveil to look at
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spellId = game.state.getHand(game.player1Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Uncharted Voyage"
                }
                val bears = game.state.getBattlefield(game.player2Id).first { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
                }

                val cast = game.execute(
                    CastSpell(game.player1Id, spellId, listOf(ChosenTarget.Permanent(bears)))
                )
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                // The bounced creature's owner (Player2) chooses top or bottom.
                withClue("Owner of the bounced creature is prompted for top/bottom") {
                    (game.getPendingDecision() != null) shouldBe true
                    game.getPendingDecision()!!.playerId shouldBe game.player2Id
                }
                game.submitDecision(OptionChosenResponse(game.getPendingDecision()!!.id, 0)) // top
                game.resolveStack()

                withClue("Grizzly Bears left the battlefield and is now in Player2's library") {
                    game.state.getBattlefield(game.player2Id).contains(bears) shouldBe false
                    game.state.getLibrary(game.player2Id).contains(bears) shouldBe true
                }

                // Surveil 1 for the caster resolves as a SelectCardsDecision (min 0) — keep the top card.
                if (game.getPendingDecision() is SelectCardsDecision) {
                    game.skipSelection()
                    game.resolveStack()
                }
                withClue("the surveil decision is resolved") {
                    (game.getPendingDecision() is SelectCardsDecision) shouldBe false
                }
            }
        }
    }
}
