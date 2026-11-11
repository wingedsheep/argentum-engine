package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Skyline Cascade — enters tapped, and its enters trigger keeps an opponent's creature tapped through
 * its controller's next untap step.
 */
class SkylineCascadeScenarioTest : ScenarioTestBase() {
    init {
        test("enters tapped and freezes an opponent's tapped creature for one untap step") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Skyline Cascade")
                .withCardOnBattlefield(2, "Glory Seeker", tapped = true)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val seeker = game.findPermanent("Glory Seeker")!!
            val cascade = game.findCardsInHand(1, "Skyline Cascade").first()

            game.execute(PlayLand(game.player1Id, cascade)).error shouldBe null
            withClue("the land enters tapped") {
                game.state.getEntity(cascade)!!.has<TappedComponent>() shouldBe true
            }
            if (game.state.pendingDecision != null) game.selectTargets(listOf(seeker)).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("opponent's turn: Glory Seeker skipped its untap") {
                game.state.activePlayerId shouldBe game.player2Id
                game.state.getEntity(seeker)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
