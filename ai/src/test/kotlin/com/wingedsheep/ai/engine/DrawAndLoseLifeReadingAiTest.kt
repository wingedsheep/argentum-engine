package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleRunner
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
 * Ancestral Craving): the life is the price of the cards, so the slot reads as a gift to our own
 * seat. Unread, a triggered draw was scored with the ability still on the stack, both seats alike,
 * and the removal tie-break handed the opponent the card. A cast spell still simulates both seats
 * for its mandatory slot, so the life loss stays a finisher when it is lethal.
 */
class DrawAndLoseLifeReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

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

        test("Sign in Blood still goes to the opponent's face when the life loss is lethal") {
            val result = runner.run(
                AiPuzzle(
                    id = "sign-in-blood-finisher",
                    category = PuzzleCategory.LETHAL_DETECTION,
                    expectation = "",
                    aiSeat = 1,
                    position = { s ->
                        s.withPlayers()
                            .withActivePlayer(1)
                            .withLifeTotal(2, 2)
                            .withLandsOnBattlefield(1, "Swamp", 2)
                            .withCardInHand(1, "Sign in Blood")
                            .apply { repeat(4) { withCardInLibrary(1, "Swamp"); withCardInLibrary(2, "Swamp") } }
                            .build()
                            .advanceToPriority(1, Step.PRECOMBAT_MAIN)
                    },
                    check = { shouldCast("Sign in Blood"); shouldTargetOpponentFace() },
                ),
                AiProfile.LIVE,
            )
            withClue("reading the slot as a gift must not prune the lethal line: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }
    }
}
