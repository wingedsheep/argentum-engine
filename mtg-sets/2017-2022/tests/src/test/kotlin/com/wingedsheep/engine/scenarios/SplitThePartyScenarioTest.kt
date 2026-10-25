package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Split the Party (AFR) — "Choose target player. Return half the creatures they control to their
 * owner's hand, rounded up."
 *
 * Only the player is targeted; on resolution the caster picks exactly ceil(n / 2) of that player's
 * creatures, and each goes to its owner's hand.
 */
class SplitThePartyScenarioTest : ScenarioTestBase() {

    init {
        test("three creatures: the caster chooses exactly two of them to return") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Split the Party")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val opponentBears = game.state.getBattlefield().filter {
                game.state.getEntity(it)?.get<CardComponent>()
                    ?.name == "Grizzly Bears" && game.state.projectedState.getController(it) == game.player2Id
            }
            opponentBears.size shouldBe 3

            game.castSpellTargetingPlayer(1, "Split the Party", 2).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("half of three, rounded up, chosen from the target player's creatures only") {
                decision.playerId shouldBe game.player1Id
                decision.minSelections shouldBe 2
                decision.maxSelections shouldBe 2
                decision.options shouldContainExactlyInAnyOrder opponentBears
            }
            game.selectCards(opponentBears.take(2)).error shouldBe null

            withClue("two bears returned to their owner's hand, one stays, the caster's own bear untouched") {
                game.state.getHand(game.player2Id).count {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                } shouldBe 2
                game.findPermanents("Grizzly Bears").size shouldBe 2
                game.state.getBattlefield().contains(opponentBears[2]) shouldBe true
            }
        }

        test("a single creature is returned (half of one rounds up to one)") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Split the Party")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Split the Party", 2).error shouldBe null
            game.resolveStack()
            (game.state.pendingDecision as? SelectCardsDecision)?.let { game.selectCards(it.options) }

            game.findPermanents("Grizzly Bears").size shouldBe 0
            game.isInHand(2, "Grizzly Bears") shouldBe true
        }
    }
}
