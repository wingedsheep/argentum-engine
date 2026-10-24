package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.stx.cards.FerventMastery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Fervent Mastery (STX #101): "You may pay {2}{R}{R} rather than pay this spell's mana cost. If
 * the {2}{R}{R} cost was paid, an opponent discards any number of cards, then draws that many
 * cards. Search your library for up to three cards, put them into your hand, shuffle, then discard
 * three cards at random."
 */
class FerventMasteryScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + FerventMastery)
        it.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Answers the caster's search by picking the three marked cards. */
    fun GameTestDriver.searchFor(player: com.wingedsheep.sdk.model.EntityId, cards: List<com.wingedsheep.sdk.model.EntityId>) {
        val search = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        search.playerId shouldBe player
        search.maxSelections shouldBe 3
        submitCardSelection(player, cards).error shouldBe null
    }

    test("cast for {2}{R}{R}: the opponent rummages as many cards as they choose, then you tutor three and discard three at random") {
        val d = createDriver()
        val me = d.player1
        val opponent = d.player2
        val wanted = listOf(
            d.putCardOnTopOfLibrary(me, "Centaur Courser"),
            d.putCardOnTopOfLibrary(me, "Lightning Bolt"),
            d.putCardOnTopOfLibrary(me, "Goblin Guide"),
        )
        val mastery = d.putCardInHand(me, "Fervent Mastery")
        d.giveMana(me, Color.RED, 4)
        val myHand = d.getHand(me).size
        val opponentHand = d.getHand(opponent).size

        d.submit(CastSpell(me, mastery, useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE)).error shouldBe null
        d.bothPass()

        val rummage = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        rummage.playerId shouldBe opponent
        rummage.minSelections shouldBe 0
        val tossed = rummage.options.take(2)
        d.submitCardSelection(opponent, tossed).error shouldBe null

        d.getGraveyard(opponent).toSet() shouldBe tossed.toSet()
        d.getHand(opponent).size shouldBe opponentHand

        d.searchFor(me, wanted)

        d.getHand(me).size shouldBe myHand - 1 + 3 - 3
        d.getGraveyard(me).size shouldBe 1 + 3
        d.getGraveyard(me).contains(mastery) shouldBe true
    }

    test("cast for {3}{R}{R}: the opponent is untouched; you tutor three and discard three at random") {
        val d = createDriver()
        val me = d.player1
        val opponent = d.player2
        val wanted = listOf(
            d.putCardOnTopOfLibrary(me, "Centaur Courser"),
            d.putCardOnTopOfLibrary(me, "Lightning Bolt"),
            d.putCardOnTopOfLibrary(me, "Goblin Guide"),
        )
        val mastery = d.putCardInHand(me, "Fervent Mastery")
        d.giveMana(me, Color.RED, 5)
        val myHand = d.getHand(me).size
        val opponentHand = d.getHand(opponent).size

        d.castSpell(me, mastery).error shouldBe null
        d.bothPass()

        d.searchFor(me, wanted)

        d.getGraveyard(opponent).size shouldBe 0
        d.getHand(opponent).size shouldBe opponentHand
        d.getHand(me).size shouldBe myHand - 1 + 3 - 3
        d.getGraveyard(me).size shouldBe 1 + 3
    }
})
