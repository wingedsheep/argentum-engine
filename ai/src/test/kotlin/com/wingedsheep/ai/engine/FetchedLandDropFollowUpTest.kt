package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.TypecycleCard
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [AiProfile.fetchedLandDropFollowUp], as [AiProfile.LIVE] ships it — a line that puts a land in
 * hand with the land drop still unused is credited with playing that land.
 *
 * The misplay is an AI self-play game (2026-10-16 playtest, 2p game 03, turns 7 and 11): stuck on
 * three lands with no land in hand and a hand of four-to-six drops, the AI passed both main phases
 * with Plainscycling Noble Templar on offer — the cycle that finds the fourth land it could play
 * the same turn. The one-ply search scored "a Plains in hand" against "a Noble Templar in hand",
 * and the Templar won.
 */
class FetchedLandDropFollowUpTest : ScenarioTestBase() {

    private val before = AiProfile.LIVE.copy(id = "live-without-fetched-land", fetchedLandDropFollowUp = false)

    private fun TestGame.choice(profile: AiProfile = AiProfile.LIVE): GameAction =
        AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)

    private fun TestGame.nameOf(action: GameAction): String? = when (action) {
        is TypecycleCard -> state.getEntity(action.cardId)?.get<CardComponent>()?.name
        is PlayLand -> state.getEntity(action.cardId)?.get<CardComponent>()?.name
        else -> null
    }

    /** Let the AI play out our precombat main, the opponent passing, until it leaves the step. */
    private fun TestGame.letAiPlayOutMain(limit: Int = 20) {
        val ai = AIPlayer.create(cardRegistry, player1Id, AiProfile.LIVE)
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

    private fun position(landInHand: Boolean = false) = scenario().withPlayers()
        .withTurnNumber(7)
        .withLandsOnBattlefield(1, "Plains", 2)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInHand(1, "Noble Templar")
        .withCardInHand(1, "Dragonstalker")
        .withCardInHand(1, "Siege-Gang Commander")
        .apply { if (landInHand) withCardInHand(1, "Mountain") }
        .apply { repeat(6) { withCardInLibrary(1, "Plains").withCardInLibrary(2, "Forest") } }
        .withLandsOnBattlefield(2, "Forest", 3)
        .build()
        .advanceToPriority(1, Step.PRECOMBAT_MAIN)

    init {

        test("stuck on three lands, Plainscycling finds the fourth instead of passing") {
            val game = position()

            game.choice(before).shouldBeInstanceOf<PassPriority>()
            val action = game.choice()
            action.shouldBeInstanceOf<TypecycleCard>()
            game.nameOf(action) shouldBe "Noble Templar"
        }

        test("the fetched Plains is played the same turn") {
            val game = position()
            game.letAiPlayOutMain()

            game.isInGraveyard(1, "Noble Templar") shouldBe true
            // The opponent has no Plains: two were ours already, the third is the one the cycle found.
            game.findPermanents("Plains").size shouldBe 3
        }

        test("with a land already in hand, the land is played first") {
            val game = position(landInHand = true)

            val action = game.choice()
            action.shouldBeInstanceOf<PlayLand>()
            game.nameOf(action) shouldBe "Mountain"
        }

        test("with the land drop already used, it does not cycle for a land it cannot play") {
            val game = position(landInHand = true)
            val mountain = game.findCardsInHand(1, "Mountain").single()
            game.execute(PlayLand(game.player1Id, mountain)).error shouldBe null

            val action = game.choice()
            (action is TypecycleCard) shouldBe false
        }
    }
}
