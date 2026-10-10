package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SurveiledEvent
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyKeywordActionAmount
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Surveil [ModifyKeywordActionAmount] (CR 701.25): "You may look at an additional two cards each time you
 * surveil." Applied once at the surveil pipeline's gather — for every surveil spelling (the
 * literal macro, a dynamic X, and the graveyard-recording variant) — so the extra cards are part
 * of what is surveiled (CR 701.25b): every one of them may go to the graveyard, and the
 * [SurveiledEvent] count includes them. A surveil 0 is no surveil event (CR 701.25c) and is never
 * modified.
 */
class SurveilAmountReplacementTest : FunSpec({

    fun surveilSpell(name: String, n: Int) = card(name) {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.Surveil(n) }
    }
    val surveilTwo = surveilSpell("Surveil Two", 2)
    val surveilZero = surveilSpell("Surveil Zero", 0)
    val scryTwo = card("Scry Two") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.Scry(2) }
    }
    val scryLens = card("Your Scry Lens") {
        manaCost = "{0}"
        typeLine = "Artifact"
        replacementEffect(ModifyKeywordActionAmount(EventPattern.ScryEvent(), modifier = 2))
    }
    val surveilX = card("Surveil X") {
        manaCost = "{X}"
        typeLine = "Sorcery"
        spell { effect = Effects.Surveil(DynamicAmounts.xValue()) }
    }
    val recordingSurveil = card("Recording Surveil") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Patterns.Library.surveil(1, storeGraveyardAs = "binned") }
    }
    val yourLens = card("Your Surveil Lens") {
        manaCost = "{0}"
        typeLine = "Artifact"
        replacementEffect(ModifyKeywordActionAmount(EventPattern.SurveilEvent(), modifier = 2))
    }
    val opponentsLens = card("Opponents' Surveil Lens") {
        manaCost = "{0}"
        typeLine = "Artifact"
        replacementEffect(ModifyKeywordActionAmount(EventPattern.SurveilEvent(Player.EachOpponent), modifier = 1))
    }

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(
            TestCards.all + listOf(surveilTwo, surveilZero, scryTwo, scryLens, surveilX, recordingSurveil, yourLens, opponentsLens)
        )
        initMirrorMatch(deck = Deck.of("Mountain" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /**
     * Answer every surveil decision by putting *every* looked-at card into the graveyard — proving
     * the extra cards are among those that may go there — and return each look's option count.
     */
    fun GameTestDriver.binEverything(): List<Int> {
        val looks = mutableListOf<Int>()
        repeat(6) {
            when (val decision = pendingDecision) {
                is SelectCardsDecision -> {
                    looks += decision.options.size
                    decision.maxSelections shouldBe decision.options.size
                    submitDecision(decision.playerId, CardsSelectedResponse(decision.id, decision.options))
                }
                is ReorderLibraryDecision ->
                    submitDecision(decision.playerId, OrderedResponse(decision.id, decision.cards))
                else -> return looks
            }
        }
        return looks
    }

    fun GameTestDriver.library(player: EntityId): List<EntityId> = state.getZone(ZoneKey(player, Zone.LIBRARY))

    fun GameTestDriver.surveiledCounts(before: Int): List<Int> =
        events.drop(before).filterIsInstance<SurveiledEvent>().map { it.count }

    fun GameTestDriver.cast(player: EntityId, name: String): List<Int> {
        castSpell(player, putCardInHand(player, name))
        bothPass()
        return binEverything()
    }

    fun GameTestDriver.castX(player: EntityId, name: String, x: Int): List<Int> {
        giveMana(player, Color.RED, x)
        castXSpell(player, putCardInHand(player, name), x)
        bothPass()
        return binEverything()
    }

    test("a literal surveil looks at two more cards, and all of them may go to the graveyard") {
        val d = setup()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Your Surveil Lens")
        val graveyardBefore = d.getGraveyard(you).size
        val libraryBefore = d.library(you).size

        val before = d.events.size
        d.cast(you, "Surveil Two") shouldBe listOf(4)
        d.surveiledCounts(before) shouldBe listOf(4)
        // Four library cards plus the spell itself.
        d.getGraveyard(you).size shouldBe graveyardBefore + 5
        d.library(you).size shouldBe libraryBefore - 4
    }

    test("the extra cards may also go back on top, in an order the surveilling player chooses") {
        val d = setup()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Your Surveil Lens")
        // Distinct cards: copies of one card have no order to choose, so no prompt.
        listOf("Grizzly Bears", "Island", "Plains", "Forest").forEach { d.putCardOnTopOfLibrary(you, it) }
        val looked = d.library(you).take(4)
        val libraryBefore = d.library(you).size

        d.castSpell(you, d.putCardInHand(you, "Surveil Two"))
        d.bothPass()
        val select = d.pendingDecision as SelectCardsDecision
        select.options.toSet() shouldBe looked.toSet()
        // Bin one card; the other three — two of them the extra look — go back on top.
        d.submitDecision(you, CardsSelectedResponse(select.id, listOf(looked[0])))
        val reorder = d.pendingDecision as ReorderLibraryDecision
        reorder.cards.toSet() shouldBe looked.drop(1).toSet()
        val chosen = looked.drop(1).reversed()
        d.submitDecision(you, OrderedResponse(reorder.id, chosen))

        d.library(you).size shouldBe libraryBefore - 1
        d.library(you).take(3) shouldBe chosen
        d.getGraveyard(you).contains(looked[0]) shouldBe true
    }

    test("a dynamic surveil X is modified too") {
        val d = setup()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Your Surveil Lens")

        val before = d.events.size
        d.castX(you, "Surveil X", 1) shouldBe listOf(3)
        d.surveiledCounts(before) shouldBe listOf(3)
    }

    test("the graveyard-recording surveil is modified too") {
        val d = setup()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Your Surveil Lens")

        d.cast(you, "Recording Surveil") shouldBe listOf(3)
    }

    test("two instances stack") {
        val d = setup()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Your Surveil Lens")
        d.putPermanentOnBattlefield(you, "Your Surveil Lens")

        d.cast(you, "Surveil Two") shouldBe listOf(6)
    }

    test("surveil 0 is no surveil event and is not modified") {
        val d = setup()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Your Surveil Lens")

        val before = d.events.size
        d.cast(you, "Surveil Zero").shouldBeEmpty()
        d.surveiledCounts(before).shouldBeEmpty()
    }

    test("surveil X with X = 0 stays a non-event") {
        val d = setup()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Your Surveil Lens")

        val before = d.events.size
        d.castX(you, "Surveil X", 0).shouldBeEmpty()
        d.surveiledCounts(before).shouldBeEmpty()
    }

    test("the look is capped by the library") {
        val d = setup()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Your Surveil Lens")
        d.library(you).drop(3).forEach { d.moveToGraveyard(it) }

        d.cast(you, "Surveil Two") shouldBe listOf(3)
    }

    test("your you-scoped lens and an opponent's opponent-scoped lens both resize your surveil") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(you, "Your Surveil Lens")
        d.putPermanentOnBattlefield(opponent, "Opponents' Surveil Lens")

        // Your lens (+2) applies to you; the opponent's opponent-scoped lens (+1) applies to you too.
        d.cast(you, "Surveil Two") shouldBe listOf(5)
    }

    test("an opponent's you-scoped lens does not resize your surveil") {
        val d = setup()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(d.getOpponent(you), "Your Surveil Lens")

        d.cast(you, "Surveil Two") shouldBe listOf(2)
    }

    test("each lens resizes only its own keyword action") {
        val d = setup()
        val you = d.activePlayer!!
        d.putPermanentOnBattlefield(you, "Your Surveil Lens")
        d.cast(you, "Scry Two") shouldBe listOf(2)

        val e = setup()
        val me = e.activePlayer!!
        e.putPermanentOnBattlefield(me, "Your Scry Lens")
        e.cast(me, "Surveil Two") shouldBe listOf(2)
    }
})
