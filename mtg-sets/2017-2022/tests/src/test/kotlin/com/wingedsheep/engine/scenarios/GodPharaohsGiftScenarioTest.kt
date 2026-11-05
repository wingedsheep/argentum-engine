package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * God-Pharaoh's Gift ({7}, Artifact):
 * "At the beginning of combat on your turn, you may exile a creature card from your graveyard. If
 *  you do, create a token that's a copy of that card, except it's a 4/4 black Zombie. It gains
 *  haste until end of turn."
 *
 * Covers the resolution-time optional choice, the 4/4 black Zombie copy exceptions (mana cost
 * kept), and that haste is an until-end-of-turn grant rather than a copied keyword.
 */
class GodPharaohsGiftScenarioTest : ScenarioTestBase() {

    private fun giftGame(withGraveyard: Boolean = true) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "God-Pharaoh's Gift")
        .apply {
            if (withGraveyard) {
                withCardInGraveyard(1, "Hill Giant")
                withCardInGraveyard(1, "Grizzly Bears")
            }
        }
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("exiling a creature card makes a 4/4 black Zombie copy with haste only this turn") {
            val game = giftGame()
            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
            game.selectCards(listOf(giant))
            game.resolveStack()

            game.isInExile(1, "Hill Giant") shouldBe true
            withClue("the unchosen card stays in the graveyard") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.findPermanent("Grizzly Bears") shouldBe null
            }

            val token = game.findPermanent("Hill Giant")
            token shouldNotBe null
            token!!
            val projected = game.state.projectedState
            game.state.getEntity(token)!!.get<ControllerComponent>()!!.playerId shouldBe game.player1Id
            projected.getPower(token) shouldBe 4
            projected.getToughness(token) shouldBe 4
            projected.getColors(token) shouldBe setOf("BLACK")
            projected.getSubtypes(token) shouldBe setOf("Zombie")
            projected.isCreature(token) shouldBe true
            withClue("unlike eternalize, the token keeps the copied card's mana cost") {
                game.state.getEntity(token)!!.get<CardComponent>()!!.manaCost.cmc shouldBe 4
            }
            withClue("the token has haste this turn") {
                projected.hasKeyword(token, Keyword.HASTE) shouldBe true
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            withClue("haste ended with the turn it was created") {
                game.findPermanent("Hill Giant") shouldBe token
                game.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe false
            }
        }

        test("choosing no card exiles nothing and creates no token") {
            val game = giftGame()
            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.skipSelection()
            game.resolveStack()

            game.isInGraveyard(1, "Hill Giant") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.findPermanent("Hill Giant") shouldBe null
            game.findPermanent("Grizzly Bears") shouldBe null
        }

        test("with no creature card in the graveyard nothing happens") {
            val game = giftGame(withGraveyard = false)
            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            game.getPendingDecision() shouldBe null
            game.state.getBattlefield(game.player1Id).size shouldBe 1
        }
    }
}
