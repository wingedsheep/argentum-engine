package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.c16.cards.ThrasiosTritonHero
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Thrasios, Triton Hero — "{4}: Scry 1, then reveal the top card of your library. If it's a land
 * card, put it onto the battlefield tapped. Otherwise, draw a card."
 *
 * Proves both branches of the reveal: a land goes onto the battlefield tapped (no card drawn), and
 * a nonland is *drawn* — a real draw that the turn's draw count sees — rather than moved to hand.
 */
class ThrasiosTritonHeroScenarioTest : ScenarioTestBase() {

    private val abilityId = ThrasiosTritonHero.activatedAbilities.first().id

    private fun TestGame.activateAndResolve() {
        val thrasios = findPermanent("Thrasios, Triton Hero")!!
        execute(ActivateAbility(playerId = player1Id, sourceId = thrasios, abilityId = abilityId)).error shouldBe null
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
        var guard = 0
        while (guard++ < 10) {
            when (getPendingDecision()) {
                is SelectCardsDecision -> skipSelection() // scry: keep the card on top
                is ReorderLibraryDecision -> keepLibraryOrder()
                null -> if (state.stack.isNotEmpty()) resolveStack() else break
                else -> error("unexpected decision ${getPendingDecision()}")
            }
        }
    }

    private fun TestGame.cardsDrawnThisTurn(): Int =
        state.getEntity(player1Id)?.get<CardsDrawnThisTurnComponent>()?.count ?: 0

    init {
        context("Thrasios, Triton Hero") {

            test("a revealed land enters the battlefield tapped and no card is drawn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thrasios, Triton Hero")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val handBefore = game.handSize(1)

                game.activateAndResolve()

                val forest = game.findPermanent("Forest")
                withClue("the land is on the battlefield, tapped") {
                    forest shouldNotBe null
                    game.state.getEntity(forest!!)!!.has<TappedComponent>() shouldBe true
                }
                withClue("nothing was drawn") {
                    game.handSize(1) shouldBe handBefore
                    game.cardsDrawnThisTurn() shouldBe 0
                }
            }

            test("a revealed nonland card is drawn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thrasios, Triton Hero")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateAndResolve()

                withClue("the nonland card was drawn, not left on top or put onto the battlefield") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.cardsDrawnThisTurn() shouldBe 1
                }
            }
        }
    }
}
