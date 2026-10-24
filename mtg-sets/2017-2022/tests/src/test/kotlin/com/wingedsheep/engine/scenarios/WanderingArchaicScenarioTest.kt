package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Wandering Archaic // Explore the Vastlands — STX #6.
 *
 * Front: "Whenever an opponent casts an instant or sorcery spell, they may pay {2}. If they don't,
 * you may copy that spell. You may choose new targets for the copy." — the casting opponent is the
 * payer, and the copy belongs to Archaic's controller, who may aim it back at the caster.
 *
 * Back: "Each player looks at the top five cards of their library and may reveal a land card
 * and/or an instant or sorcery card from among them. …Each player gains 3 life." — each player
 * makes their own picks from their own library, active player first.
 */
class WanderingArchaicScenarioTest : ScenarioTestBase() {

    /** Player 1 controls Wandering Archaic; player 2 (active) casts Lightning Bolt at player 1. */
    private fun boltIntoArchaic(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Wandering Archaic")
            .withCardInHand(2, "Lightning Bolt")
            .withLandsOnBattlefield(2, "Mountain", 3)
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
        game.resolveStack()

        val payPrompt = game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
        withClue("The casting opponent decides whether to pay {2}") {
            payPrompt.playerId shouldBe game.player2Id
        }
        return game
    }

    init {
        context("Wandering Archaic") {
            test("opponent declines to pay - Archaic's controller copies the spell and redirects it") {
                val game = boltIntoArchaic()
                game.answerYesNo(false)

                val mayCopy = game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                mayCopy.playerId shouldBe game.player1Id
                game.answerYesNo(true)

                val retarget = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                retarget.playerId shouldBe game.player1Id
                game.selectTargets(listOf(game.player2Id))

                game.resolveStack()

                withClue("The copy hits the caster; the original still hits Archaic's controller") {
                    game.getLifeTotal(2) shouldBe 17
                    game.getLifeTotal(1) shouldBe 17
                }
            }

            test("opponent declines to pay and Archaic's controller declines to copy - no copy") {
                val game = boltIntoArchaic()
                game.answerYesNo(false)
                game.answerYesNo(false)
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 17
                game.getLifeTotal(2) shouldBe 20
            }

            test("opponent pays {2} - no copy is offered") {
                val game = boltIntoArchaic()
                game.answerYesNo(true)
                if (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }

                withClue("Paying the toll ends the trigger without asking Archaic's controller") {
                    (game.getPendingDecision() as? YesNoDecision)?.playerId shouldBe null
                }
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 17
                game.getLifeTotal(2) shouldBe 20
                withClue("All three Mountains were tapped: one for the Bolt, two for the toll") {
                    game.findAllPermanents("Mountain").count { id ->
                        game.state.getEntity(id)?.has<TappedComponent>() == true
                    } shouldBe 3
                }
            }
        }

        context("Explore the Vastlands") {
            test("each player reveals from their own top five, keeps the reveals, and gains 3 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wandering Archaic")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Hill Giant")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.findCardsInHand(1, "Wandering Archaic").first()
                val forest = game.findCardsInLibrary(1, "Forest").first()
                val bolt = game.findCardsInLibrary(1, "Lightning Bolt").first()
                val mountain = game.findCardsInLibrary(2, "Mountain").first()

                val cast = game.execute(CastSpell(game.player1Id, cardId, faceIndex = 0))
                withClue("Casting Explore the Vastlands should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                // Active player first: the land pick, then the instant/sorcery pick.
                val p1Land = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                p1Land.playerId shouldBe game.player1Id
                game.selectCards(listOf(forest))
                val p1Spell = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                p1Spell.playerId shouldBe game.player1Id
                game.selectCards(listOf(bolt))

                // Then the opponent, from their own library. They have no instant or sorcery.
                val p2Land = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                p2Land.playerId shouldBe game.player2Id
                game.selectCards(listOf(mountain))
                while (game.hasPendingDecision()) {
                    val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                    decision.playerId shouldBe game.player2Id
                    game.skipSelection()
                }

                withClue("Each player's revealed cards go to their own hand") {
                    game.isInHand(1, "Forest") shouldBe true
                    game.isInHand(1, "Lightning Bolt") shouldBe true
                    game.isInHand(2, "Mountain") shouldBe true
                    game.handSize(1) shouldBe 2
                    game.handSize(2) shouldBe 1
                }
                withClue("The rest go to the bottom of their own library") {
                    game.librarySize(1) shouldBe 3
                    game.librarySize(2) shouldBe 4
                }
                withClue("Each player gains 3 life") {
                    game.getLifeTotal(1) shouldBe 23
                    game.getLifeTotal(2) shouldBe 23
                }
                game.isInGraveyard(1, "Wandering Archaic") shouldBe true
            }
        }
    }
}
