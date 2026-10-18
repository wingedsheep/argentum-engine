package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Revelation (LEG) — {G} World Enchantment: "Players play with their hands revealed."
 *
 * Symmetric, unlike Seer's Vision: the controller's own hand is opened to the opponent too.
 */
class RevelationScenarioTest : ScenarioTestBase() {

    private fun transformer() =
        ClientStateTransformer(cardRegistry, predicateEvaluator = services.predicateEvaluator)

    init {
        context("Revelation") {
            test("every player sees every hand, the controller's own included") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Revelation")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val controllerCard = game.state.getHand(game.player1Id).single()
                val opponentCard = game.state.getHand(game.player2Id).single()

                transformer().transform(game.state, game.player1Id).cards.keys shouldContain opponentCard
                transformer().transform(game.state, game.player2Id).cards.keys shouldContain controllerCard
            }

            test("hands are hidden again once a newer world enchantment replaces it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Revelation")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(2, "Gravity Sphere")
                    .withLandsOnBattlefield(2, "Mountain", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val controllerCard = game.state.getHand(game.player1Id).single()
                transformer().transform(game.state, game.player2Id).cards.keys shouldContain controllerCard

                game.castSpell(2, "Gravity Sphere")
                game.resolveStack()

                game.isInGraveyard(1, "Revelation") shouldBe true
                transformer().transform(game.state, game.player2Id).cards.keys shouldNotContain controllerCard
            }
        }
    }
}
