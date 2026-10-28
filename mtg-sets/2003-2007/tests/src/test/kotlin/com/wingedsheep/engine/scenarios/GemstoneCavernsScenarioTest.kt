package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.KeepHand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tsp.cards.GemstoneCaverns
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Gemstone Caverns (TSP #274):
 * "If this card is in your opening hand and you're not the starting player, you may begin the game
 * with Gemstone Caverns on the battlefield with a luck counter on it. If you do, exile a card from
 * your hand.
 * {T}: Add {C}. If Gemstone Caverns has a luck counter on it, instead add one mana of any color."
 */
class GemstoneCavernsScenarioTest : FunSpec({

    val genericOne = card("Test Generic One") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(GemstoneCaverns, genericOne))
    }

    fun GameTestDriver.moveToOpeningHand(playerId: EntityId, name: String) {
        var s = state
        val library = ZoneKey(playerId, Zone.LIBRARY)
        val hand = ZoneKey(playerId, Zone.HAND)
        // The shuffle may already have dealt it into the opening hand.
        val wanted = s.getLibrary(playerId).firstOrNull { s.getEntity(it)?.get<CardComponent>()?.name == name } ?: return
        val outgoing = s.getHand(playerId).first { s.getEntity(it)?.get<CardComponent>()?.name != name }
        s = s.removeFromZone(library, wanted).addToZone(hand, wanted)
        s = s.removeFromZone(hand, outgoing).addToZone(library, outgoing)
        replaceState(s)
    }

    test("on the draw it begins on the battlefield with a luck counter and taps for any color") {
        val d = driver()
        d.initGame(Deck.of("Island" to 40), Deck.of("Gemstone Caverns" to 1, "Island" to 39), skipMulligans = false)
        d.moveToOpeningHand(d.player2, "Gemstone Caverns")
        d.submit(KeepHand(d.player1)).error shouldBe null
        d.submit(KeepHand(d.player2)).error shouldBe null

        val offer = d.pendingDecision
        offer.shouldBeInstanceOf<YesNoDecision>()
        offer.playerId shouldBe d.player2
        d.submitYesNo(d.player2, true).error shouldBe null

        val exile = d.pendingDecision
        exile.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(d.player2, listOf(exile.options.first())).error shouldBe null
        d.state.getZone(ZoneKey(d.player2, Zone.EXILE)).size shouldBe 1

        val caverns = d.state.getBattlefield(d.player2).single()
        d.state.getEntity(caverns)?.get<CountersComponent>()?.getCount(CounterType.LUCK) shouldBe 1

        // P2's first turn: the Caverns is the only mana source, and it pays for a red spell.
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe d.player2
        val goblin = d.putCardInHand(d.player2, "Goblin Guide")
        d.castSpell(d.player2, goblin).error shouldBe null
    }

    test("without a luck counter it taps only for colorless") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        val player = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(player, "Gemstone Caverns")

        val goblin = d.putCardInHand(player, "Goblin Guide")
        withClue("{C} can't pay {R}") {
            d.castSpell(player, goblin).error shouldNotBe null
        }
        val generic = d.putCardInHand(player, "Test Generic One")
        withClue("…but it does pay a generic cost") {
            d.castSpell(player, generic).error shouldBe null
        }
    }
})
