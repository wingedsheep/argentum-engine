package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Zimone and Dina (MOM #257) — {B}{G}{U} Legendary Creature — Human Dryad 3/4.
 *
 *   Whenever you draw your second card each turn, target opponent loses 2 life and you gain 2 life.
 *   {T}, Sacrifice another creature: Draw a card. You may put a land card from your hand onto the
 *   battlefield tapped. If you control eight or more lands, repeat this process once.
 */
class ZimoneAndDinaScenarioTest : ScenarioTestBase() {

    /**
     * Activate the ability, sacrificing Grizzly Bears, and drive every decision: land choices put
     * the first offered land while [landPuts] remain, then decline.
     */
    private fun activateAndDrive(game: TestGame, landPuts: Int) {
        val zimone = game.findPermanent("Zimone and Dina")!!
        val bears = game.findPermanent("Grizzly Bears")!!
        val ability = cardRegistry.requireCard("Zimone and Dina").activatedAbilities.first()
        game.execute(
            ActivateAbility(playerId = game.player1Id, sourceId = zimone, abilityId = ability.id)
        ).error shouldBe null

        var puts = landPuts
        var guard = 0
        while (guard++ < 30) {
            when (val decision = game.getPendingDecision()) {
                is SelectCardsDecision -> when {
                    bears in decision.options -> game.selectCards(listOf(bears))
                    puts > 0 && decision.options.isNotEmpty() -> {
                        puts--
                        game.selectCards(listOf(decision.options.first()))
                    }
                    else -> game.skipSelection()
                }
                is ChooseTargetsDecision -> game.selectTargets(listOf(game.player2Id))
                is YesNoDecision -> game.answerYesNo(true)
                null -> if (game.state.stack.isNotEmpty()) game.resolveStack() else break
                else -> break
            }
        }
    }

    private fun forestCount(game: TestGame) = game.findAllPermanents("Forest").size

    init {
        context("Zimone and Dina") {

            test("with fewer than eight lands after the first pass, the process runs once") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zimone and Dina", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withCardInHand(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                activateAndDrive(game, landPuts = 5)

                withClue("Grizzly Bears was sacrificed as the cost") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("One Forest from hand entered; seven lands is below the repeat threshold") {
                    forestCount(game) shouldBe 7
                    game.findPermanent("Island") shouldBe null
                }
                withClue("Drew exactly one card (1 in hand - 1 Forest put + 1 drawn)") {
                    game.handSize(1) shouldBe 1
                    game.librarySize(1) shouldBe 2
                }
                withClue("The land entered tapped") {
                    game.findAllPermanents("Forest").count { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe 1
                }
            }

            test("reaching eight lands in the first pass repeats the process once") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zimone and Dina", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                activateAndDrive(game, landPuts = 5)

                withClue("Two passes: two lands put, never a third") {
                    (forestCount(game) + game.findAllPermanents("Island").size) shouldBe 9
                }
                withClue("Two cards drawn") {
                    game.librarySize(1) shouldBe 1
                    game.handSize(1) shouldBe 3
                }
                withClue("Both new lands entered tapped") {
                    (game.findAllPermanents("Forest") + game.findAllPermanents("Island"))
                        .count { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe 2
                }
                withClue("Second draw this turn drains the opponent for 2") {
                    game.getLifeTotal(2) shouldBe 18
                    game.getLifeTotal(1) shouldBe 22
                }
            }

            test("drawing your second card this turn drains the target opponent") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zimone and Dina", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardsDrawnThisTurn(1, 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                activateAndDrive(game, landPuts = 0)

                game.getLifeTotal(2) shouldBe 18
                game.getLifeTotal(1) shouldBe 22
            }

            test("a single draw in the turn does not trigger the drain") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Zimone and Dina", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                activateAndDrive(game, landPuts = 0)

                game.getLifeTotal(2) shouldBe 20
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
