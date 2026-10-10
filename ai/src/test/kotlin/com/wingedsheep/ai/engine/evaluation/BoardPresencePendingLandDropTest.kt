package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.ai.engine.AIPlayer
import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `AiProfile.pendingLandDropIsMana`: the idle-mana refund must never make a land drop score below
 * passing.
 *
 * The board is AI game-log review 2026-10-10, game 3 turn 11: tapped out on four lands after
 * casting a 4-drop, a hand of two 5-drops and an untapped land. Read off the battlefield alone,
 * the four tapped lands are idle (nothing in hand costs 1–4) and refunded; play the fifth land and
 * the 5-drops become "castable but for the tapped lands", the refund vanishes, and passing wins.
 */
class BoardPresencePendingLandDropTest : ScenarioTestBase() {

    init {
        val intents = IntentCatalog.of(cardRegistry)

        /** [BoardPresence] for seat 1 with land sequencing on, and the pending drop on or off. */
        fun GameState.presence(pending: Boolean): Double =
            BoardPresence.score(
                this, projectedState, turnOrder[0], intents,
                sequenceLandsByUsableMana = true, pendingLandDropIsMana = pending,
            )

        /** Game 3 turn 11, before the land drop: four tapped lands, two 5-drops and a land in hand. */
        fun tappedOut(land: String = "Shattered Landscape") = scenario().withPlayers()
            .withCardOnBattlefield(1, "Plains", tapped = true)
            .withCardOnBattlefield(1, "Plains", tapped = true)
            .withCardOnBattlefield(1, "Forest", tapped = true)
            .withCardOnBattlefield(1, "Forest", tapped = true)
            .withCardInHand(1, "Voltstorm Angel")
            .withCardInHand(1, "Petrifying Meddler")
            .withCardInHand(1, land)
            .build()

        fun afterLandDrop(land: String = "Shattered Landscape"): GameState {
            val game = tappedOut(land)
            val landId = game.findCardsInHand(1, land).single()
            game.execute(PlayLand(game.player1Id, landId)).error shouldBe null
            return game.state
        }

        test("playing an untapped land scores above passing") {
            val passed = tappedOut().state
            val played = afterLandDrop()
            withClue("without the pending drop the refund vanishes on the land play — the bug") {
                passed.presence(pending = false) shouldBeGreaterThan played.presence(pending = false)
            }
            played.presence(pending = true) shouldBeGreaterThan passed.presence(pending = true)
        }

        test("an untapped land drop is worth exactly a full untapped land") {
            // The refund window is the same before and after the drop, so the only change is the
            // land itself.
            val passed = tappedOut(land = "Plains").state
            val played = afterLandDrop(land = "Plains")
            played.presence(pending = true) shouldBe
                (passed.presence(pending = true) + 0.6 plusOrMinus EPSILON)
        }

        test("after the drop is spent, the flag changes nothing") {
            val played = afterLandDrop()
            played.presence(pending = true) shouldBe (played.presence(pending = false) plusOrMinus EPSILON)
        }

        test("on the opponent's turn the land in hand is not mana") {
            // The same tapped-out board: counting the Plains would pull the 5-drop into the window.
            val theirTurn = scenario().withPlayers()
                .withCardOnBattlefield(1, "Plains", tapped = true)
                .withCardOnBattlefield(1, "Plains", tapped = true)
                .withCardOnBattlefield(1, "Plains", tapped = true)
                .withCardOnBattlefield(1, "Plains", tapped = true)
                .withCardInHand(1, "Voltstorm Angel")
                .withCardInHand(1, "Plains")
                .withActivePlayer(2)
                .build().state
            theirTurn.presence(pending = true) shouldBe (theirTurn.presence(pending = false) plusOrMinus EPSILON)
        }

        test("the AI makes the land drop instead of passing") {
            val game = tappedOut()
            fun choice(profile: AiProfile) =
                AIPlayer.create(cardRegistry, game.player1Id, profile).chooseAction(game.state)

            withClue("flag off: the game-3 pass, reproduced") {
                choice(AiProfile.PRODUCTION_LANDSEQ).shouldBeInstanceOf<PassPriority>()
            }
            choice(AiProfile.PRODUCTION_LANDSEQ.copy(id = "test-pending-land", pendingLandDropIsMana = true))
                .shouldBeInstanceOf<PlayLand>()
            withClue("what real players face") {
                choice(AiProfile.LIVE).shouldBeInstanceOf<PlayLand>()
            }
        }
    }

    private companion object {
        const val EPSILON = 1e-9
    }
}
