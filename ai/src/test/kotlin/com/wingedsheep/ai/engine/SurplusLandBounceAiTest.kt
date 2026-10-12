package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [AiProfile.surplusAwareLands]: an optional "return up to one target permanent you control" does
 * not send the AI's own land back to a hand that already holds the next land drops.
 *
 * 2026-10-12 AI-vs-AI logs (`live`), game 2 turn 9: Mirkwood Nurturer returned its controller's
 * tapped Plains for the +1/+1 counter with Hobbit Hole and Forest in hand — one land fewer for the
 * rest of the game. The evaluator alone put that pick about 2.9 above skipping.
 */
class SurplusLandBounceAiTest : ScenarioTestBase() {

    private val withoutFlag = AiProfile.LIVE.copy(id = "live-without-surpluslands", surplusAwareLands = false)

    /** Pass priority until Nurturer's enters trigger asks for its target. */
    private fun TestGame.advanceToTargets(): ChooseTargetsDecision {
        var guard = 0
        while (state.pendingDecision == null && guard++ < 20) {
            execute(PassPriority(state.priorityPlayerId!!))
        }
        return state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
    }

    private fun TestGame.answer(profile: AiProfile, decision: ChooseTargetsDecision): List<EntityId> =
        AIPlayer.create(cardRegistry, player1Id, profile).respondToDecision(state, decision)
            .shouldBeInstanceOf<TargetsResponse>().selectedTargets[0].orEmpty()

    private fun TestGame.isLand(id: EntityId): Boolean =
        state.getEntity(id)?.get<CardComponent>()?.isLand == true

    /** The game 2 turn 9 board, with [landsInHand] beside Nurturer in player 1's hand. */
    private fun nurturerDecision(vararg landsInHand: String): Pair<TestGame, ChooseTargetsDecision> {
        var builder = scenario().withPlayers()
            .withLandsOnBattlefield(1, "Plains", 2)
            .withCardOnBattlefield(1, "Elvenking's Halls")
            .withCardOnBattlefield(1, "Forest")
            .withCardOnBattlefield(1, "Little Bear")
            .withCardOnBattlefield(1, "Serra Angel")
            .withCardInHand(1, "Mirkwood Nurturer")
            .withCardInHand(1, "Wilderland Scrounger")
            .withCardInHand(1, "Nori, Teller of Tales")
            .withLandsOnBattlefield(2, "Forest", 2)
            .withLandsOnBattlefield(2, "Mountain", 2)
            .withCardOnBattlefield(2, "Trained Jackal")
            .withCardOnBattlefield(2, "Zodiac Monkey")
            .withCardOnBattlefield(2, "Yellow Scarves Troops")
            .withCardInHand(2, "Forest")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withCardInLibrary(2, "Forest")
            .withLifeTotal(1, 17)
            .withLifeTotal(2, 18)
            .withTurnNumber(9)
        for (land in landsInHand) builder = builder.withCardInHand(1, land)
        val game = builder.build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
        game.castSpell(1, "Mirkwood Nurturer").error shouldBe null
        return game to game.advanceToTargets()
    }

    init {
        test("g2 T9: Nurturer does not return its own land with two more lands already in hand") {
            val (game, decision) = nurturerDecision("Hobbit Hole", "Forest")

            withClue("the position must still reproduce the misplay without the flag") {
                val old = game.answer(withoutFlag, decision)
                old.size shouldBe 1
                game.isLand(old.single()) shouldBe true
            }
            game.answer(AiProfile.LIVE, decision).filter { game.isLand(it) }.shouldBeEmpty()
        }

        test("a bounce that costs no land drop is still taken: nothing else in hand to play") {
            // No land in hand and the drop unused: the returned land goes straight back down, so the
            // counter is free and the evaluator, not the rule, decides.
            val (game, decision) = nurturerDecision()
            val picked = game.answer(AiProfile.LIVE, decision)
            picked.size shouldBe 1
            game.isLand(picked.single()) shouldBe true
        }
    }
}
