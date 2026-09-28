package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Wu Spy (Portal Three Kingdoms): look at the top two cards of target player's library,
 * put one of them into their graveyard. The other stays on top.
 */
class WuSpyScenarioTest : ScenarioTestBase() {

    init {
        context("Wu Spy") {
            test("puts the chosen card into the graveyard and leaves the other on top") {
                val game = scenario()
                    .withPlayers("Spy", "Victim")
                    .withCardInHand(1, "Wu Spy")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Hill Giant")
                    .withCardInLibrary(2, "Elvish Warrior")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = game.state.getLibrary(game.player2Id)
                game.castSpell(1, "Wu Spy").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                // Put the top card (Grizzly Bears) into the graveyard.
                game.selectCards(listOf(before[0])).error shouldBe null
                game.resolveStack()

                val after = game.state.getLibrary(game.player2Id)
                withClue("chosen card left the library; the other is untouched on top") {
                    after shouldBe before.drop(1)
                }
                game.graveyardSize(2) shouldBe 1
                game.state.getEntity(game.state.getGraveyard(game.player2Id).first())
                    ?.get<CardComponent>()?.name shouldBe "Grizzly Bears"
            }
        }
    }
}
