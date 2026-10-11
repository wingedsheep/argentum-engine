package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Octomancer (BLC #37) — {3}{G}{U} Creature — Frog Druid 3/3
 *
 *   Gift an Octopus
 *   At the beginning of each end step, create a token that's a copy of target creature token that
 *   entered the battlefield this turn.
 *
 * The end-step trigger may target any player's creature token — here the Octopus the opponent was
 * just gifted — and the copy is created under Octomancer's controller.
 */
class OctomancerScenarioTest : ScenarioTestBase() {

    private fun TestGame.tokensControlledBy(playerId: EntityId): List<EntityId> =
        state.getBattlefield(playerId).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    init {
        context("Octomancer") {

            test("gifted Octopus is copied for Octomancer's controller at the end step") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Octomancer")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val octomancer = game.state.getZone(game.player1Id, Zone.HAND).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Octomancer"
                }
                val cast = game.execute(
                    CastSpell(playerId = game.player1Id, cardId = octomancer, giftRecipient = game.player2Id)
                )
                withClue("casting with the gift promised should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val gifted = game.tokensControlledBy(game.player2Id)
                withClue("the promised opponent gets an 8/8 Octopus") {
                    gifted shouldHaveSize 1
                    game.state.projectedState.getPower(gifted.single()) shouldBe 8
                    game.state.projectedState.getToughness(gifted.single()) shouldBe 8
                }
                game.tokensControlledBy(game.player1Id) shouldHaveSize 0

                game.passUntilPhase(Phase.ENDING, Step.END)
                withClue("the Octopus entered this turn, so it is offered as a target") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                }
                game.selectTargets(gifted)
                game.resolveStack()

                val copies = game.tokensControlledBy(game.player1Id)
                withClue("Octomancer's controller gets a token copy of the Octopus") {
                    copies shouldHaveSize 1
                    game.state.projectedState.getPower(copies.single()) shouldBe 8
                    game.state.projectedState.getToughness(copies.single()) shouldBe 8
                    game.state.projectedState.hasSubtype(copies.single(), "Octopus") shouldBe true
                }
                withClue("the opponent's Octopus is untouched") {
                    game.tokensControlledBy(game.player2Id) shouldHaveSize 1
                }
            }

            test("with no creature token that entered this turn the trigger has no target and creates nothing") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardOnBattlefield(1, "Octomancer")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears", isToken = true)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                withClue("neither a nontoken creature nor a token that entered on an earlier turn is a legal target") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe false
                }
                game.resolveStack()

                game.tokensControlledBy(game.player1Id) shouldHaveSize 0
                game.tokensControlledBy(game.player2Id) shouldHaveSize 1
            }
        }
    }
}
