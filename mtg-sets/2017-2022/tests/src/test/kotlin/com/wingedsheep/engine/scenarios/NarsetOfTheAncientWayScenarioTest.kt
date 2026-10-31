package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Narset of the Ancient Way (IKO #195) — the −2:
 *
 *   Draw a card, then you may discard a card. When you discard a nonland card this way, Narset
 *   deals damage equal to that card's mana value to target creature or planeswalker.
 *
 * Pins: any card may be discarded, but only a nonland discard arms the reflexive trigger; its
 * target is picked after the discard and its damage is the discarded card's mana value; declining
 * the discard does nothing.
 */
class NarsetOfTheAncientWayScenarioTest : ScenarioTestBase() {

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun setup(handCard: String): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Narset of the Ancient Way")
            .withCardInHand(1, handCard)
            .withCardInLibrary(1, "Island")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        seedLoyalty(game, game.findPermanent("Narset of the Ancient Way")!!, 4)
        return game
    }

    private fun activateMinusTwo(game: TestGame) {
        val narset = game.findPermanent("Narset of the Ancient Way")!!
        val ability = cardRegistry.getCard("Narset of the Ancient Way")!!.script.activatedAbilities[1]
        game.execute(ActivateAbility(game.player1Id, narset, ability.id)).error shouldBe null
        game.resolveStack()
    }

    init {
        test("discarding a nonland card deals its mana value to target creature") {
            val game = setup("Gray Ogre")
            activateMinusTwo(game)

            withClue("the drawn card is in hand before the discard choice") {
                game.findCardsInHand(1, "Island").size shouldBe 1
            }
            game.selectCards(game.findCardsInHand(1, "Gray Ogre")).error shouldBe null

            withClue("the reflexive trigger asks for its target after the discard") {
                game.isInGraveyard(1, "Gray Ogre") shouldBe true
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            }
            game.selectTargets(listOf(game.findPermanent("Hill Giant")!!)).error shouldBe null
            game.resolveStack()

            withClue("Gray Ogre has mana value 3, enough to kill the 3/3 Hill Giant") {
                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
        }

        test("discarding a land is allowed but arms no reflexive trigger") {
            val game = setup("Forest")
            activateMinusTwo(game)

            game.selectCards(game.findCardsInHand(1, "Forest")).error shouldBe null

            game.isInGraveyard(1, "Forest") shouldBe true
            game.hasPendingDecision() shouldBe false
            game.state.stack.isEmpty() shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
        }

        test("declining the discard keeps the hand and deals no damage") {
            val game = setup("Gray Ogre")
            activateMinusTwo(game)

            game.skipSelection().error shouldBe null

            game.handSize(1) shouldBe 2
            game.hasPendingDecision() shouldBe false
            game.state.stack.isEmpty() shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
