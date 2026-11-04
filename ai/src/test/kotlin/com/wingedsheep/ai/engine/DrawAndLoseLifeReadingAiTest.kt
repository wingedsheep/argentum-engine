package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Target player draws N cards and loses N life" (Sign in Blood, Vault Plunderer, Blood Pact,
 * Ancestral Craving): the draw helps the slot and the life loss hurts it.
 */
class DrawAndLoseLifeReadingAiTest : ScenarioTestBase() {

    init {
        test("Vault Plunderer's enter trigger draws for its controller when the life loss can't kill") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(1, "Vault Plunderer")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(2, "Craw Wurm")
                .withCardInLibrary(2, "Craw Wurm")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            game.castSpell(1, "Vault Plunderer").error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision
            decision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val ai = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
            val response = ai.respondToDecision(game.state, decision)
            response.shouldBeInstanceOf<TargetsResponse>()
            val picked = response.selectedTargets.values.flatten()
            withClue("a card for 1 life at 20 is a gift, not a ping: picked $picked, AI is ${game.player1Id}") {
                picked shouldBe listOf(game.player1Id)
            }
        }
    }
}
