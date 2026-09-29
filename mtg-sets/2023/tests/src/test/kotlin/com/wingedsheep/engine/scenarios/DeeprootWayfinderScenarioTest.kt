package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Deeproot Wayfinder (MOM #184) — "Whenever this creature deals combat damage to a player or
 * battle, surveil 1, then you may return a land card from your graveyard to the battlefield tapped."
 */
class DeeprootWayfinderScenarioTest : ScenarioTestBase() {

    private fun board(vararg graveyard: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Deeproot Wayfinder")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Island")
        .apply { graveyard.forEach { withCardInGraveyard(1, it) } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.attackAndAwaitSurveil(): SelectCardsDecision {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Deeproot Wayfinder" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        var guard = 0
        while (state.pendingDecision !is SelectCardsDecision && guard++ < 20) resolveStack()
        return state.pendingDecision as? SelectCardsDecision
            ?: error("expected the surveil prompt; got ${state.pendingDecision}")
    }

    init {
        test("a land surveiled into the graveyard can be returned to the battlefield tapped") {
            val game = board()
            val surveil = game.attackAndAwaitSurveil()
            game.getLifeTotal(2) shouldBe 18

            game.selectCards(surveil.options).error shouldBe null
            val choose = game.state.pendingDecision as? SelectCardsDecision
                ?: error("expected the return-a-land prompt; got ${game.state.pendingDecision}")
            val forest = game.findCardsInGraveyard(1, "Forest").single()
            game.selectCards(listOf(forest)).error shouldBe null
            game.resolveStack()

            withClue("the surveiled Forest came back tapped") {
                game.isOnBattlefield("Forest") shouldBe true
                game.state.getEntity(forest)?.has<TappedComponent>() shouldBe true
                game.findCardsInGraveyard(1, "Forest").size shouldBe 0
            }
            game.librarySize(1) shouldBe 1
        }

        test("declining both leaves the library top and the graveyard land alone") {
            val game = board("Mountain")
            game.attackAndAwaitSurveil()

            game.skipSelection().error shouldBe null
            (game.state.pendingDecision is ReorderLibraryDecision) shouldBe true
            game.keepLibraryOrder().error shouldBe null
            (game.state.pendingDecision is SelectCardsDecision) shouldBe true
            game.skipSelection().error shouldBe null
            game.resolveStack()

            game.librarySize(1) shouldBe 2
            game.isInGraveyard(1, "Mountain") shouldBe true
            game.isOnBattlefield("Mountain") shouldBe false
        }
    }
}
