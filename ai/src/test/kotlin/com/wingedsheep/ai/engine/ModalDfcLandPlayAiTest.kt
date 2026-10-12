package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.PlayerTurnsTakenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * [AiProfile.surplusAwareLands]: a modal double-faced card whose front is a spell is played as its
 * land face only when the land is needed, never in place of a land already in hand.
 *
 * 2026-10-12 AI-vs-AI logs (`live`), game 3. Turn 1: `PlayLand(Drowner of Truth, asBackFace=true)`
 * with Forest, Mountain and Shattered Landscape in hand. Turn 13: Pinnacle Monk played as a seventh
 * land with the Monk castable the turn after. The land-drop earmark in `CardAdvantage` only ever
 * held back a *land*, and a modal DFC is not one in hand, so either play came out card-neutral.
 */
class ModalDfcLandPlayAiTest : ScenarioTestBase() {

    private val withoutFlag = AiProfile.LIVE.copy(id = "live-without-surpluslands", surplusAwareLands = false)

    private fun TestGame.choose(profile: AiProfile): GameAction =
        AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)

    private fun TestGame.nameOf(action: PlayLand): String =
        state.getEntity(action.cardId)!!.get<CardComponent>()!!.name

    private fun TestGame.withTurnsTaken(count: Int): TestGame = apply {
        state = state.updateEntity(player1Id) { it.with(PlayerTurnsTakenComponent(count)) }
    }

    init {
        test("g3 T1: a basic is played, not Drowner of Truth's land face") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Drowner of Truth")
                .withCardInHand(1, "Bridgeworks Battle")
                .withCardInHand(1, "Forest")
                .withCardInHand(1, "Mountain")
                .withCardInHand(1, "Siege Smash")
                .withCardInHand(1, "Six")
                .withCardInHand(1, "Shattered Landscape")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withTurnNumber(1)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
                .withTurnsTaken(1)

            withClue("the position must still reproduce the misplay without the flag") {
                val old = game.choose(withoutFlag).shouldBeInstanceOf<PlayLand>()
                game.nameOf(old) shouldBe "Drowner of Truth"
                old.asBackFace shouldBe true
            }
            val play = game.choose(AiProfile.LIVE).shouldBeInstanceOf<PlayLand>()
            play.asBackFace shouldBe false
        }

        test("g3 T13: Pinnacle Monk stays in hand rather than becoming a seventh land") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Mountain", tapped = true)
                .withCardOnBattlefield(1, "Mountain", tapped = true)
                .withCardOnBattlefield(1, "Mountain", tapped = true)
                .withCardOnBattlefield(1, "Mountain", tapped = true)
                .withCardOnBattlefield(1, "Forest", tapped = true)
                .withCardOnBattlefield(1, "Forest", tapped = true)
                .withCardOnBattlefield(1, "Six")
                .withCardOnBattlefield(1, "Nightshade Dryad")
                .withCardInHand(1, "Pinnacle Monk")
                .withCardOnBattlefield(2, "Ambush Commander")
                .withLandsOnBattlefield(2, "Forest", 3)
                .withLandsOnBattlefield(2, "Swamp", 2)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withLifeTotal(2, 7)
                .withTurnNumber(13)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
                .withTurnsTaken(7)

            withClue("the position must still reproduce the misplay without the flag") {
                val old = game.choose(withoutFlag).shouldBeInstanceOf<PlayLand>()
                old.asBackFace shouldBe true
            }
            val action = game.choose(AiProfile.LIVE)
            withClue("chose $action") {
                (action is PlayLand && action.asBackFace) shouldBe false
            }
        }

        test("the land face is still played when it is the only land and the curve is behind") {
            // Turn 3 of the game for player 1, one land in play, nothing else to play as a land:
            // this is the spot the land face is printed for.
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Mountain")
                .withCardInHand(1, "Pinnacle Monk")
                .withCardInHand(1, "Siege Smash")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withTurnNumber(5)
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
                .withTurnsTaken(3)

            val play = game.choose(AiProfile.LIVE).shouldBeInstanceOf<PlayLand>()
            game.nameOf(play) shouldBe "Pinnacle Monk"
            play.asBackFace shouldBe true
        }
    }
}
