package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Eerie Gravestone (SPM #163) — {2} Artifact.
 *
 *   When this artifact enters, draw a card.
 *   {1}{B}, Sacrifice this artifact: Mill four cards. You may put a creature card from among
 *   them into your hand.
 *
 * Covers (1) the ETB "draw a card" trigger firing when the artifact resolves, and (2) the
 * sacrifice-activated mill-four + optional creature-grab pipeline.
 */
class EerieGravestoneScenarioTest : ScenarioTestBase() {

    init {
        context("Eerie Gravestone") {

            test("ETB: drawing a card when the artifact enters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Eerie Gravestone")
                    .withLandsOnBattlefield(1, "Swamp", 2) // {2}
                    .withCardInLibrary(1, "Grizzly Bears") // something to draw
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Eerie Gravestone")
                withClue("Casting Eerie Gravestone should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("The artifact is on the battlefield") {
                    game.isOnBattlefield("Eerie Gravestone") shouldBe true
                }
                // Hand started with just Eerie Gravestone (1). Cast removes it (0), ETB draws 1 → 1.
                withClue("The ETB trigger drew the library card into hand") {
                    game.handSize(1) shouldBe 1
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
            }

            test("Sacrifice ability: mill four and put a creature card into hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Eerie Gravestone")
                    .withLandsOnBattlefield(1, "Swamp", 2) // {1}{B}
                    // Library is exactly four cards, so all four are milled regardless of order.
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val abilityId = cardRegistry.getCard("Eerie Gravestone")!!
                    .activatedAbilities.first().id
                val gravestone = game.findPermanent("Eerie Gravestone")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = gravestone,
                        abilityId = abilityId
                    )
                )
                withClue("Activating {1}{B}, Sacrifice should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // The artifact is sacrificed as a cost, so it leaves the battlefield immediately.
                withClue("Eerie Gravestone was sacrificed to pay the cost") {
                    game.isOnBattlefield("Eerie Gravestone") shouldBe false
                }

                // Resolve the ability; it pauses on the "you may put a creature card" selection.
                game.resolveStack()

                val decision = game.getPendingDecision()
                    .shouldNotBeNull()
                    .shouldBeInstanceOf<SelectCardsDecision>()
                val creatureOption = decision.options.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                }

                game.selectCards(listOf(creatureOption))

                withClue("The three land cards were milled into the graveyard") {
                    game.state.getGraveyard(game.player1Id).count {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Forest"
                    } shouldBe 3
                }
                withClue("The chosen creature card went to hand, not the graveyard") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }
            }
        }
    }
}
