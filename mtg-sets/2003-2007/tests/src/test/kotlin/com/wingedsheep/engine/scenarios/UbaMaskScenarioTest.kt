package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.UbaMask
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Uba Mask (CHK #272) — "If a player would draw a card, that player exiles that card face up
 * instead. Each player may play lands and cast spells from among cards they exiled with this
 * artifact this turn."
 */
class UbaMaskScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + UbaMask)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.mayPlayHolders(cardId: EntityId): List<EntityId> =
        state.mayPlayPermissions.filter { cardId in it.cardIds }.map { it.controllerId }

    test("an opponent's draw exiles their own top card face up, and they may cast it this turn") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Uba Mask")
        val bolt = d.putCardOnTopOfLibrary(d.player2, "Lightning Bolt")
        val handBefore = d.getHandSize(d.player2)

        d.passPriorityUntil(Step.DRAW)

        withClue("the draw was replaced") { d.getHandSize(d.player2) shouldBe handBefore }
        withClue("the card that would have been drawn is exiled from the drawer's own library") {
            d.getExileCardNames(d.player2) shouldBe listOf("Lightning Bolt")
        }
        withClue("face up") { d.state.getEntity(bolt)?.has<FaceDownComponent>() shouldBe false }
        withClue("the exiler holds the permission") { d.mayPlayHolders(bolt) shouldBe listOf(d.player2) }

        d.giveMana(d.player2, Color.RED, 1)
        d.castSpell(d.player2, bolt, listOf(d.player1)).error shouldBe null
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe 17
    }

    test("the controller's own draws are replaced too, and unplayed cards stay exiled after the turn") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Uba Mask")
        val bolt = d.putCardOnTopOfLibrary(d.player1, "Lightning Bolt")
        val handBefore = d.getHandSize(d.player1)

        // Player 2's turn, then player 1's draw step.
        d.passPriorityUntil(Step.DRAW)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.DRAW)

        d.getHandSize(d.player1) shouldBe handBefore
        d.getExileCardNames(d.player1) shouldBe listOf("Lightning Bolt")
        d.mayPlayHolders(bolt) shouldBe listOf(d.player1)

        // Pass into the next turn without casting it.
        d.passPriorityUntil(Step.UPKEEP)
        withClue("cards exiled on a previous turn can't be played (ruling)") {
            d.mayPlayHolders(bolt) shouldBe emptyList()
        }
        withClue("they just remain exiled (ruling)") {
            d.getExileCardNames(d.player1) shouldBe listOf("Lightning Bolt")
        }
    }

    test("with Uba Mask gone, its exiled cards can no longer be cast") {
        val d = driver()
        val mask = d.putPermanentOnBattlefield(d.player1, "Uba Mask")
        val bolt = d.putCardOnTopOfLibrary(d.player2, "Lightning Bolt")

        d.passPriorityUntil(Step.DRAW)
        d.mayPlayHolders(bolt) shouldBe listOf(d.player2)

        d.moveToGraveyard(mask)
        d.giveMana(d.player2, Color.RED, 1)
        withClue("the second sentence is a static ability of the artifact") {
            d.castSpell(d.player2, bolt, listOf(d.player1)).error shouldNotBe null
        }
        d.getExileCardNames(d.player2) shouldBe listOf("Lightning Bolt")
    }
})
