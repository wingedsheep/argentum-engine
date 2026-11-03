package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Q, Stacking the Deck (TRK #165) — {2}{R}{R} 4/4 Legendary Creature — Q.
 *
 * "When Q enters, shuffle your library, then exile cards from the top of your library until you
 * exile a nonland card. That nonland card gains 'Exile a card from your hand: Create a copy of this
 * card. You may cast the copy. Activate only during your turn and only if this card is exiled.'"
 *
 * The engine rules for abilities granted to an exiled card live in
 * `ExileGrantedActivatedAbilityTest`; this pins the card's own flow.
 */
class QStackingTheDeckScenarioTest : ScenarioTestBase() {

    private val qName = "Q, Stacking the Deck"

    private fun newGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, qName)
        .withCardInHand(1, "Swamp")
        .withLandsOnBattlefield(1, "Mountain", 5)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Raging Goblin")
        .withCardInLibrary(1, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.castQ() {
        castSpell(1, qName).error shouldBe null
        resolveStack()
    }

    private fun TestGame.exiledGoblin() =
        state.getExile(player1Id).single { state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Raging Goblin" }

    private fun TestGame.goblinExileActions(playerNumber: Int) =
        getLegalActions(playerNumber).filter { it.sourceZone == "EXILE" && (it.action as? ActivateAbility)?.sourceId == exiledGoblin() }

    init {
        context("Q, Stacking the Deck") {

            test("enters: exiles from the top until the nonland card, which gains the exile ability") {
                val game = newGame()
                game.castQ()

                withClue("only the nonland card stops the walk; lands met on the way are exiled too") {
                    game.isInExile(1, "Raging Goblin") shouldBe true
                    game.librarySize(1) + game.state.getExile(game.player1Id).size shouldBe 3
                }
                game.state.grantedActivatedAbilities.filter { it.entityId == game.exiledGoblin() } shouldHaveSize 1
                game.goblinExileActions(1) shouldHaveSize 1
            }

            test("activating it exiles a card from hand and casts a copy, paying its cost") {
                val game = newGame()
                game.castQ()
                val goblin = game.exiledGoblin()

                game.execute(game.goblinExileActions(1).single().action).error shouldBe null
                var guard = 0
                while ((game.state.stack.isNotEmpty() || game.state.pendingDecision != null) && guard++ < 40) {
                    when (val d = game.getPendingDecision()) {
                        is YesNoDecision -> game.answerYesNo(true)
                        is SelectCardsDecision -> game.selectCards(listOf(d.options.first()))
                        is SelectManaSourcesDecision -> game.submitManaSourcesAutoPay()
                        null -> game.passPriority()
                        else -> error("unexpected decision $d")
                    }
                }

                withClue("the Swamp in hand paid the cost") {
                    game.isInHand(1, "Swamp") shouldBe false
                    game.isInExile(1, "Swamp") shouldBe true
                }
                withClue("the copy's {R} was paid: all five Mountains are tapped (four for Q, one for the copy)") {
                    game.findPermanents("Mountain").count {
                        game.state.getEntity(it)?.has<TappedComponent>() == true
                    } shouldBe 5
                }
                withClue("the copy resolved into a Raging Goblin on the battlefield") {
                    game.findPermanent("Raging Goblin") shouldNotBe null
                }
                withClue("the exiled original stays, still carrying the ability") {
                    game.state.getExile(game.player1Id).contains(goblin) shouldBe true
                    game.state.grantedActivatedAbilities.filter { it.entityId == goblin } shouldHaveSize 1
                }
            }

            test("it can't be activated on the opponent's turn") {
                val game = newGame()
                game.castQ()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                game.goblinExileActions(1).shouldBeEmpty()
            }
        }
    }
}
