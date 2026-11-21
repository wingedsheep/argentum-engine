package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class BreakingEnteringScenarioTest : ScenarioTestBase() {
    init {
        fun TestGame.cardId() = state.getHand(player1Id).first {
            state.getEntity(it)?.get<CardComponent>()?.name == "Breaking // Entering"
        }

        test("fused, Breaking mills eight and Entering reanimates a milled creature with haste") {
            val builder = scenario()
                .withPlayers("Caster", "Opponent").withCardInHand(1, "Breaking // Entering")
                .withLandsOnBattlefield(1, "Island", 1).withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Grizzly Bears").withCardInLibrary(2, "Centaur Courser")
            repeat(6) { builder.withCardInLibrary(2, "Island") }
            val game = builder.withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.execute(
                CastSpell(
                    game.player1Id, game.cardId(),
                    targets = listOf(ChosenTarget.Player(game.player2Id)), castFused = true
                )
            ).error.shouldBeNull()
            game.resolveStack()

            // Breaking milled all eight of the opponent's cards, so both creatures are eligible.
            game.graveyardSize(2) shouldBe 8
            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            val courser = decision.options.first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Centaur Courser"
            }
            game.selectCards(listOf(courser)).error.shouldBeNull()
            game.resolveStack()

            val reanimated = game.findPermanent("Centaur Courser").shouldNotBeNull()
            game.state.getEntity(reanimated)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
            game.state.projectedState.hasKeyword(reanimated, Keyword.HASTE) shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }

        test("Entering alone takes a creature card from an opponent's graveyard") {
            val game = scenario()
                .withPlayers("Caster", "Opponent").withCardInHand(1, "Breaking // Entering")
                .withLandsOnBattlefield(1, "Swamp", 3).withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInGraveyard(2, "Grizzly Bears")
                .withCardInLibrary(1, "Plains").withCardInLibrary(2, "Plains")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.execute(CastSpell(game.player1Id, game.cardId(), faceIndex = 1)).error.shouldBeNull()
            game.resolveStack()
            if (game.getPendingDecision() is SelectCardsDecision) {
                val decision = game.getPendingDecision() as SelectCardsDecision
                game.selectCards(listOf(decision.options.single())).error.shouldBeNull()
                game.resolveStack()
            }

            val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            game.state.getEntity(bears)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
            game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
            game.graveyardSize(2) shouldBe 0
        }
    }
}
