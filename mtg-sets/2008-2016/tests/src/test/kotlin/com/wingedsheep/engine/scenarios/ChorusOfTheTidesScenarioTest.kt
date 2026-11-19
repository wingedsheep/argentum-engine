package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Chorus of the Tides: "Flying. Heroic — Whenever you cast a spell that targets this creature,
 * scry 1."
 */
class ChorusOfTheTidesScenarioTest : ScenarioTestBase() {

    init {
        test("targeting it with a spell scries 1 before the spell resolves") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Chorus of the Tides")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val chorus = game.findPermanent("Chorus of the Tides")!!
            val island = game.findCardsInLibrary(1, "Island").single()
            val forest = game.findCardsInLibrary(1, "Forest").single()

            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Giant Growth").single(),
                    targets = listOf(ChosenTarget.Permanent(chorus)),
                )
            ).error shouldBe null
            game.resolveStack()

            val scry = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            scry.options shouldBe listOf(island)
            game.selectCards(listOf(island)).error shouldBe null
            game.resolveStack()

            game.state.getLibrary(game.player1Id).first() shouldBe forest
            game.state.getLibrary(game.player1Id).last() shouldBe island
            game.state.projectedState.getPower(chorus) shouldBe 6
        }
    }
}
