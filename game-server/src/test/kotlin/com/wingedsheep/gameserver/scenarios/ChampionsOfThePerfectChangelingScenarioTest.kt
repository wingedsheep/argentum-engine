package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Behold-an-Elf must accept a changeling in hand. Gangly Stompling is a Shapeshifter with
 * the Changeling keyword, so it counts as an Elf in every zone (Rule 702.73a).
 */
class ChampionsOfThePerfectChangelingScenarioTest : ScenarioTestBase() {

    private fun isInExile(game: ScenarioTestBase.TestGame, playerNumber: Int, cardName: String): Boolean {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getZone(ZoneKey(playerId, Zone.EXILE)).any {
            game.state.getEntity(it)?.get<CardComponent>()?.name == cardName
        }
    }

    init {
        test("can behold a changeling in hand for Champions of the Perfect") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Champions of the Perfect")
                .withCardInHand(1, "Gangly Stompling")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val castResult = game.castSpellWithBeholdCost(1, "Champions of the Perfect", "Gangly Stompling")
            withClue("Should cast successfully: ${castResult.error}") {
                castResult.error shouldBe null
            }

            withClue("Gangly Stompling should be in exile") {
                isInExile(game, 1, "Gangly Stompling") shouldBe true
            }
        }
    }
}
