package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Recoil ({1}{U}{B} instant): "Return target permanent to its owner's hand. Then that player
 * discards a card."
 *
 * Regression: the discard step must be made by the permanent's owner (the discarding player),
 * choosing from their own hand — not by the spell's controller. The owner is referenced via
 * [com.wingedsheep.sdk.scripting.references.Player.OwnerOf], which previously fell through to the
 * controller in `effectTargetToChooser`.
 */
class RecoilScenarioTest : ScenarioTestBase() {

    init {
        context("Recoil") {

            test("the bounced permanent's owner chooses which card to discard") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Recoil")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    // Player 2 owns the targeted permanent and holds cards to choose from.
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(2, "Forest")
                    .withCardInHand(2, "Mountain")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!

                val castResult = game.castSpell(1, "Recoil", targetId = bearsId)
                withClue("Casting Recoil should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // The bounce returns Grizzly Bears to player 2's hand before the discard.
                withClue("Grizzly Bears should be back in player 2's hand") {
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                }

                // The discard must be a choice made BY the owner (player 2), not the caster.
                val decision = game.getPendingDecision()
                withClue("Resolving Recoil should pause for the owner to choose a card to discard") {
                    decision.shouldNotBeNull()
                }
                val selectDecision = decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Player 2 (the permanent's owner) must be the one choosing the discard") {
                    selectDecision.playerId shouldBe game.player2Id
                }

                // Player 2 chooses Mountain; the caster does not get to pick.
                val mountainId = game.findCardsInHand(2, "Mountain").single()
                val discardResult = game.selectCards(listOf(mountainId))
                withClue("Submitting the discard should succeed: ${discardResult.error}") {
                    discardResult.error shouldBe null
                }

                withClue("The card player 2 chose should be in their graveyard") {
                    game.isInGraveyard(2, "Mountain") shouldBe true
                }
                withClue("The card player 2 kept should remain in hand") {
                    game.isInHand(2, "Forest") shouldBe true
                }
            }
        }
    }
}
