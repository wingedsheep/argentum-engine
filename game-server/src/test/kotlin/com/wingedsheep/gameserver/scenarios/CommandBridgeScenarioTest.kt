package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Command Bridge's enter-tapped + pay-or-sacrifice trigger.
 *
 * Card reference:
 * - Command Bridge (Land)
 *   "This land enters tapped.
 *   When this land enters, sacrifice it unless you tap an untapped permanent you control.
 *   {T}: Add one mana of any color."
 *
 * Covers the new [com.wingedsheep.sdk.scripting.costs.PayCost.Tap] payment variant for
 * [com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect].
 */
class CommandBridgeScenarioTest : ScenarioTestBase() {

    init {
        context("Command Bridge ETB trigger") {
            test("survives when controller taps an untapped permanent to pay the cost") {
                // Forest is the untapped permanent we'll tap. Command Bridge itself enters tapped
                // so cannot satisfy the cost.
                val game = scenario()
                    .withPlayers("Bridger", "Opponent")
                    .withCardInHand(1, "Command Bridge")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bridgeInHand = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Command Bridge"
                }

                game.execute(PlayLand(game.player1Id, bridgeInHand))
                game.resolveStack()

                withClue("Bridge should be on the battlefield, tapped from its ETB replacement") {
                    game.isOnBattlefield("Command Bridge") shouldBe true
                    val bridgeId = game.findPermanent("Command Bridge")!!
                    (game.state.getEntity(bridgeId)?.get<TappedComponent>() != null) shouldBe true
                }

                withClue("Trigger should pause for the tap-or-sacrifice decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // The only valid candidate is the Forest (Bridge is tapped and excluded).
                val forestId = game.findPermanent("Forest")!!
                game.selectCards(listOf(forestId))

                withClue("Bridge should still be on the battlefield (cost was paid)") {
                    game.isOnBattlefield("Command Bridge") shouldBe true
                    game.isInGraveyard(1, "Command Bridge") shouldBe false
                }

                withClue("Forest should now be tapped (we paid the cost by tapping it)") {
                    (game.state.getEntity(forestId)?.get<TappedComponent>() != null) shouldBe true
                }
            }

            test("is sacrificed when controller declines to tap a permanent") {
                val game = scenario()
                    .withPlayers("Bridger", "Opponent")
                    .withCardInHand(1, "Command Bridge")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bridgeInHand = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Command Bridge"
                }

                game.execute(PlayLand(game.player1Id, bridgeInHand))
                game.resolveStack()

                withClue("Trigger should pause for the tap-or-sacrifice decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Player declines — pick zero permanents.
                game.skipSelection()

                withClue("Bridge should be sacrificed (suffer effect)") {
                    game.isOnBattlefield("Command Bridge") shouldBe false
                    game.isInGraveyard(1, "Command Bridge") shouldBe true
                }

                val forestId = game.findPermanent("Forest")!!
                withClue("Forest should remain untapped (we did not pay)") {
                    (game.state.getEntity(forestId)?.get<TappedComponent>() != null) shouldBe false
                }
            }

            test("is auto-sacrificed when controller has no other untapped permanents") {
                // No other permanents — Bridge enters tapped, so the only candidate
                // (itself) is filtered out. The PayOrSuffer executor must shortcut
                // directly to the suffer branch without prompting.
                val game = scenario()
                    .withPlayers("Bridger", "Opponent")
                    .withCardInHand(1, "Command Bridge")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bridgeInHand = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Command Bridge"
                }

                game.execute(PlayLand(game.player1Id, bridgeInHand))
                game.resolveStack()

                withClue("There should be no pending decision when no candidates exist") {
                    game.hasPendingDecision() shouldBe false
                }

                withClue("Bridge should be in the graveyard") {
                    game.isOnBattlefield("Command Bridge") shouldBe false
                    game.isInGraveyard(1, "Command Bridge") shouldBe true
                }
            }

            test("only already-untapped permanents are valid candidates") {
                // Both Forests are tapped — only the Mountain is a legal candidate.
                val game = scenario()
                    .withPlayers("Bridger", "Opponent")
                    .withCardInHand(1, "Command Bridge")
                    .withCardOnBattlefield(1, "Forest", tapped = true)
                    .withCardOnBattlefield(1, "Forest", tapped = true)
                    .withCardOnBattlefield(1, "Mountain", tapped = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bridgeInHand = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Command Bridge"
                }

                game.execute(PlayLand(game.player1Id, bridgeInHand))
                game.resolveStack()

                withClue("Trigger should pause — Mountain is a legal candidate") {
                    game.hasPendingDecision() shouldBe true
                }

                val mountainId = game.findPermanent("Mountain")!!
                game.selectCards(listOf(mountainId))

                withClue("Mountain should now be tapped") {
                    (game.state.getEntity(mountainId)?.get<TappedComponent>() != null) shouldBe true
                }

                withClue("Bridge should survive") {
                    game.isOnBattlefield("Command Bridge") shouldBe true
                }
            }
        }
    }
}
