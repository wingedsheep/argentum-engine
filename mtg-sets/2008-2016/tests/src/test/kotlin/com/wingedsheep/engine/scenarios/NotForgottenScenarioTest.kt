package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Not Forgotten (SOI #30, reprinted J22 #222) — {1}{W} Sorcery.
 *
 * "Put target card from a graveyard on your choice of the top or bottom of its owner's library.
 *  Create a 1/1 white Spirit creature token with flying."
 *
 * Proven here:
 *  - the spell's caster (not the card's owner) makes the top/bottom choice, even for a card in an
 *    opponent's graveyard, and choosing top puts it on top of the *owner's* library;
 *  - choosing bottom puts it on the bottom;
 *  - either way the caster gets a 1/1 flying Spirit token.
 */
class NotForgottenScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseOption(label: String) {
        val decision = getPendingDecision()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        withClue("The caster makes the top/bottom choice") { decision.playerId shouldBe player1Id }
        val index = decision.options.indexOfFirst { it.contains(label) }
        check(index >= 0) { "Option '$label' not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    private fun TestGame.assertSpiritToken() {
        val spirits = findPermanents("Spirit Token")
        withClue("Exactly one Spirit token is created") { spirits.size shouldBe 1 }
        val spirit = spirits.single()
        state.getEntity(spirit)?.get<ControllerComponent>()?.playerId shouldBe player1Id
        state.projectedState.getPower(spirit) shouldBe 1
        state.projectedState.getToughness(spirit) shouldBe 1
        state.projectedState.hasKeyword(spirit, Keyword.FLYING) shouldBe true
    }

    init {
        test("caster puts an opponent's graveyard card on top of its owner's library and gets a Spirit") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Not Forgotten")
                .withCardInGraveyard(2, "Grizzly Bears")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(1, "Plains")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()
            val cast = game.castSpellTargetingGraveyardCard(1, "Not Forgotten", 2, "Grizzly Bears")
            withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.chooseOption("top")

            withClue("Grizzly Bears left the opponent's graveyard") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe false
            }
            withClue("Grizzly Bears is on top of its owner's library") {
                game.state.getLibrary(game.player2Id).first() shouldBe bears
            }
            game.librarySize(2) shouldBe 3
            game.librarySize(1) shouldBe 1
            game.assertSpiritToken()
            game.isInGraveyard(1, "Not Forgotten") shouldBe true
        }

        test("choosing bottom puts the card on the bottom of its owner's library") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Not Forgotten")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val cast = game.castSpellTargetingGraveyardCard(1, "Not Forgotten", 1, "Grizzly Bears")
            withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.chooseOption("bottom")

            val library = game.state.getLibrary(game.player1Id)
            withClue("Grizzly Bears is on the bottom of its owner's library") {
                library.last() shouldBe bears
                library.first() shouldBe library.first { it != bears }
            }
            library.size shouldBe 3
            game.assertSpiritToken()
        }
    }
}
