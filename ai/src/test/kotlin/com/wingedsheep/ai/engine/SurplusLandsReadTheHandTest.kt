package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * [AiProfile.surplusLandsReadTheHand], as [AiProfile.LIVE] ships it — a land in hand that no
 * coming land drop needs is not a card's worth.
 *
 * Bushwhack with five lands in play, two Forests and nothing else in hand: one Forest is this
 * turn's drop, the other next turn's, and no spell is waiting on more mana. Fetching a third basic
 * buys a land with nothing to cast off it; the fight mode has Centaur Courser (3/3) kill the
 * opponent's only creature and survive. The evaluator used to count every land within the next two
 * drops as a whole card, so the fetched Forest outscored the dead Llanowar Elves.
 *
 * The flag-off half of this test (it fetched the Forest) was retired once the AI could read a
 * modal spell's modes: with the fight mode read as removal, it finds the fight even pricing every
 * land as a card.
 */
class SurplusLandsReadTheHandTest : ScenarioTestBase() {

    /** Let the AI play out our precombat main, the opponent passing, until it leaves the step. */
    private fun TestGame.letAiPlayOutMain(profile: AiProfile, limit: Int = 20) {
        val ai = AIPlayer.create(cardRegistry, player1Id, profile)
        repeat(limit) {
            if (state.step != Step.PRECOMBAT_MAIN) return
            val decision = state.pendingDecision
            if (decision != null) {
                execute(SubmitDecision(decision.playerId, ai.respondToDecision(state, decision))).error shouldBe null
                return@repeat
            }
            val priority = state.priorityPlayerId ?: return
            execute(if (priority == player1Id) ai.chooseAction(state) else PassPriority(priority)).error shouldBe null
        }
    }

    private fun bushwhackPosition() = scenario().withPlayers()
        .withTurnNumber(9)
        .withLandsOnBattlefield(1, "Forest", 5)
        .withCardOnBattlefield(1, "Centaur Courser")
        .withCardInHand(1, "Forest")
        .withCardInHand(1, "Forest")
        .withCardInHand(1, "Bushwhack")
        .apply { repeat(6) { withCardInLibrary(1, "Forest").withCardInLibrary(1, "Centaur Courser") } }
        .withLandsOnBattlefield(2, "Island", 5)
        .withCardOnBattlefield(2, "Llanowar Elves")
        .withCardInHand(2, "Island")
        .apply { repeat(6) { withCardInLibrary(2, "Island") } }
        .build()
        .advanceToPriority(1, Step.PRECOMBAT_MAIN)

    init {
        test("with next turn's land already in hand, Bushwhack fights rather than fetching a land nothing needs") {
            val game = bushwhackPosition()
            game.letAiPlayOutMain(AiProfile.LIVE)

            game.isInGraveyard(1, "Bushwhack") shouldBe true
            game.isInGraveyard(2, "Llanowar Elves") shouldBe true
            // This turn's Forest was played; the other is still waiting for next turn's drop.
            game.findCardsInHand(1, "Forest").size shouldBe 1
        }
    }
}
