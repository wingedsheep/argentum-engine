package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Quick Draw (OTJ #138) — {R} Instant.
 *
 *   "Target creature you control gets +1/+1 and gains first strike until end of turn.
 *    Creatures target opponent controls lose first strike and double strike until end of turn."
 *
 * Verifies that the controlled creature is pumped and gains first strike, while every creature
 * the targeted opponent controls loses first strike (White Knight) and double strike (Fencing
 * Ace) until end of turn.
 */
class QuickDrawScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Quick Draw") {

            test("pumps your creature and strips opponent's first/double strike") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Quick Draw")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2, no first strike
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "White Knight") // 2/2 first strike
                    .withCardOnBattlefield(2, "Fencing Ace") // 1/1 double strike
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val whiteKnight = game.findPermanent("White Knight")!!
                val fencingAce = game.findPermanent("Fencing Ace")!!

                var projected = stateProjector.project(game.state)
                projected.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe false
                projected.hasKeyword(whiteKnight, Keyword.FIRST_STRIKE) shouldBe true
                projected.hasKeyword(fencingAce, Keyword.DOUBLE_STRIKE) shouldBe true

                // Find Quick Draw in hand and cast it: target 0 = our creature, target 1 = opponent.
                val handCard = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Quick Draw"
                }
                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = handCard,
                        targets = listOf(
                            ChosenTarget.Permanent(bears),
                            ChosenTarget.Player(game.player2Id),
                        )
                    )
                )
                withClue("Casting Quick Draw should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                projected = stateProjector.project(game.state)
                withClue("Your creature gains +1/+1 and first strike") {
                    projected.getPower(bears) shouldBe 3
                    projected.getToughness(bears) shouldBe 3
                    projected.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
                }
                withClue("Opponent's White Knight loses first strike") {
                    projected.hasKeyword(whiteKnight, Keyword.FIRST_STRIKE) shouldBe false
                }
                withClue("Opponent's Fencing Ace loses double strike") {
                    projected.hasKeyword(fencingAce, Keyword.DOUBLE_STRIKE) shouldBe false
                }
            }
        }
    }
}
