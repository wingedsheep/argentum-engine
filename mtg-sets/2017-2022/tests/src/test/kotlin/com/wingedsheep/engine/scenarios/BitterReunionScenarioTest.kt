package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Bitter Reunion (BRO #127) — ETB: you may discard a card; if you do, draw two cards.
 * {1}, Sacrifice it: creatures you control gain haste until end of turn.
 */
class BitterReunionScenarioTest : ScenarioTestBase() {

    private val abilityId = cardRegistry.getCard("Bitter Reunion")!!.activatedAbilities.first().id

    init {
        test("entering lets you discard a card to draw two") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Bitter Reunion")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Bitter Reunion").error shouldBe null
            game.resolveStack()

            val fodder = game.findCardsInHand(1, "Grizzly Bears").single()
            game.answerYesNo(true)
            if (game.state.pendingDecision != null) {
                game.selectCards(listOf(fodder))
            }
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.handSize(1) shouldBe 2
        }

        test("declining the discard draws nothing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Bitter Reunion")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Bitter Reunion").error shouldBe null
            game.resolveStack()
            game.answerYesNo(false)
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.handSize(1) shouldBe 1
        }

        test("sacrificing it gives creatures you control haste") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Bitter Reunion")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Glory Seeker")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val reunion = game.findPermanent("Bitter Reunion")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val opposing = game.findPermanent("Glory Seeker")!!

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = reunion, abilityId = abilityId)
            ).error shouldBe null
            game.isInGraveyard(1, "Bitter Reunion") shouldBe true
            game.resolveStack()

            game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
            game.state.projectedState.hasKeyword(opposing, Keyword.HASTE) shouldBe false
        }
    }
}
