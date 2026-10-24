package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.stx.cards.JadziOracleOfArcavios
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Jadzi, Oracle of Arcavios // Journey to the Oracle (STX #151).
 *
 * Magecraft reveals the top card: a nonland card may be cast from the library for {1} instead of its
 * mana cost (ignoring its timing and colored pips), a declined card stays on top, a land goes onto
 * the battlefield, and a modal DFC offers each nonland face. Also covers the discard-to-bounce
 * ability and the back face's land drop + self-return.
 */
class JadziOracleOfArcaviosScenarioTest : ScenarioTestBase() {

    init {
        fun board(top: String, lands: Int = 2) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Jadzi, Oracle of Arcavios")
            .withLandsOnBattlefield(1, "Mountain", lands)
            .withCardInHand(1, "Shock")
            .withCardInLibrary(1, top)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.topOfLibrary(): String =
            state.getEntity(state.getLibrary(player1Id).first())!!.get<CardComponent>()!!.name

        fun TestGame.untappedMountains(): Int =
            findAllPermanents("Mountain").count { !state.getEntity(it)!!.has<TappedComponent>() }

        /** Shock the opponent; Jadzi's magecraft trigger goes on top and resolves first. */
        fun TestGame.shockAndTrigger() {
            castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            resolveStack() // stops at Jadzi's offer, or resolves through when there is none
        }

        test("a revealed sorcery is cast from the library for {1}, while Shock is still on the stack") {
            val game = board(top = "Divination")
            val handBefore = game.handSize(1) - 1 // Shock leaves the hand
            game.shockAndTrigger()

            withClue("Jadzi offers the revealed Divination") { game.hasPendingDecision() shouldBe true }
            game.selectCards(game.findCardsInLibrary(1, "Divination"))
            withClue("{1} was paid, not Divination's {2}{U}") { game.untappedMountains() shouldBe 0 }
            withClue("casting Divination is itself magecraft: the next card, an Island, enters") {
                game.resolveStack()
                game.findAllPermanents("Island").size shouldBe 1
            }

            game.isInGraveyard(1, "Divination") shouldBe true
            game.handSize(1) shouldBe handBefore + 2
            game.getLifeTotal(2) shouldBe 18
        }

        test("a declined card stays on top of the library") {
            val game = board(top = "Divination")
            game.shockAndTrigger()
            game.skipSelection()
            game.resolveStack()

            game.topOfLibrary() shouldBe "Divination"
            game.untappedMountains() shouldBe 1
        }

        test("a revealed land is put onto the battlefield") {
            val game = board(top = "Forest")
            game.shockAndTrigger()
            game.resolveStack()

            game.isOnBattlefield("Forest") shouldBe true
            game.topOfLibrary() shouldBe "Island"
        }

        test("with no mana left for {1}, the card can't be cast and stays on top") {
            val game = board(top = "Divination", lands = 1)
            game.shockAndTrigger()
            withClue("the offer is still made; the {1} just can't be paid") { game.hasPendingDecision() shouldBe true }
            game.selectCards(game.findCardsInLibrary(1, "Divination"))
            game.resolveStack()

            game.topOfLibrary() shouldBe "Divination"
            game.isInGraveyard(1, "Divination") shouldBe false
        }

        test("a modal DFC with two nonland faces may be cast as its back face for {1}") {
            val game = board(top = "Valentin, Dean of the Vein")
            game.shockAndTrigger()
            game.selectCards(game.findCardsInLibrary(1, "Valentin, Dean of the Vein"))

            val faceChoice = game.getPendingDecision()
            faceChoice.shouldBeInstanceOf<ChooseOptionDecision>()
            game.submitDecision(
                OptionChosenResponse(faceChoice.id, faceChoice.options.indexOf("Lisette, Dean of the Root"))
            ).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Lisette, Dean of the Root") shouldBe true
        }

        test("discard a card: Jadzi returns to its owner's hand") {
            val game = board(top = "Island")
            val jadzi = game.findPermanent("Jadzi, Oracle of Arcavios")!!
            val shock = game.findCardsInHand(1, "Shock").single()
            game.execute(
                ActivateAbility(
                    game.player1Id, jadzi, JadziOracleOfArcavios.activatedAbilities.first().id,
                    costPayment = AdditionalCostPayment(discardedCards = listOf(shock)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Jadzi, Oracle of Arcavios") shouldBe true
            game.isInGraveyard(1, "Shock") shouldBe true
        }

        fun journeyBoard() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Jadzi, Oracle of Arcavios")
            .withCardInHand(1, "Forest")
            .withCardInHand(1, "Island")
            .withCardInHand(1, "Shock")
            .withLandsOnBattlefield(1, "Forest", 6)
            .withCardInLibrary(1, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        /** Casts Journey to the Oracle and puts both lands from hand: 6 + 2 = 8 lands. */
        fun TestGame.castJourneyToEightLands() {
            val journey = findCardsInHand(1, "Jadzi, Oracle of Arcavios").single()
            execute(com.wingedsheep.engine.core.CastSpell(player1Id, journey, faceIndex = 0)).error shouldBe null
            resolveStack()
            selectCards(findCardsInHand(1, "Forest") + findCardsInHand(1, "Island"))
            findAllPermanents("Forest").size + findAllPermanents("Island").size shouldBe 8
        }

        test("Journey to the Oracle: at eight lands, discarding returns it to its owner's hand") {
            val game = journeyBoard()
            game.castJourneyToEightLands()

            withClue("eight lands → may discard") { game.hasPendingDecision() shouldBe true }
            game.answerYesNo(true)
            if (game.hasPendingDecision()) game.selectCards(game.findCardsInHand(1, "Shock"))
            game.resolveStack()

            game.isInGraveyard(1, "Shock") shouldBe true
            game.isInHand(1, "Jadzi, Oracle of Arcavios") shouldBe true
        }

        test("Journey to the Oracle: declining the discard sends it to the graveyard") {
            val game = journeyBoard()
            game.castJourneyToEightLands()
            game.answerYesNo(false)
            game.resolveStack()

            game.isInHand(1, "Shock") shouldBe true
            game.isInGraveyard(1, "Jadzi, Oracle of Arcavios") shouldBe true
        }

        test("Journey to the Oracle: below eight lands there is no discard offer") {
            val game = journeyBoard()
            val journey = game.findCardsInHand(1, "Jadzi, Oracle of Arcavios").single()
            game.execute(com.wingedsheep.engine.core.CastSpell(game.player1Id, journey, faceIndex = 0)).error shouldBe null
            game.resolveStack()
            game.selectCards(game.findCardsInHand(1, "Forest")) // 7 lands
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.isInHand(1, "Shock") shouldBe true
            game.isInGraveyard(1, "Jadzi, Oracle of Arcavios") shouldBe true
        }
    }
}
