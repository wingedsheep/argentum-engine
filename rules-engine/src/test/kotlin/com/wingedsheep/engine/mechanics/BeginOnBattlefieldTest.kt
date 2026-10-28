package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.KeepHand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayBeginGameOnBattlefield
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Begin-on-battlefield opening-hand actions (CR 103.6a) with a condition, entry counters and an
 * "if you do" rider — the axes of [com.wingedsheep.sdk.model.BeginOnBattlefield], all three of which
 * Gemstone Caverns prints: "If this card is in your opening hand and you're not the starting player,
 * you may begin the game with [it] on the battlefield with a luck counter on it. If you do, exile a
 * card from your hand."
 */
class BeginOnBattlefieldTest : FunSpec({

    val caverns = card("Test Caverns") {
        typeLine = "Legendary Land"
        oracleText = "If this card is in your opening hand and you're not the starting player, you may begin " +
            "the game with Test Caverns on the battlefield with a luck counter on it. If you do, exile a card from your hand."
        mayBeginGameOnBattlefield(
            onlyIf = Conditions.IsNotYourTurn,
            counters = mapOf(CounterType.LUCK to 1),
            then = Patterns.Hand.exileFromHand(1, EffectTarget.Controller),
        )
    }

    val chooser = card("Test Chooser") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        oracleText = "If this card is in your opening hand, you may begin the game with it on the battlefield. " +
            "If you do, exile a card from your hand.\nAs this enchantment enters, choose a color."
        replacementEffect(EntersWithChoice(ChoiceType.COLOR))
        mayBeginGameOnBattlefield(then = Patterns.Hand.exileFromHand(1, EffectTarget.Controller))
    }

    val leyline = card("Test Leyline") {
        manaCost = "{2}"
        typeLine = "Enchantment"
        mayBeginGameOnBattlefield()
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(caverns, chooser, leyline))
    }

    /** Swap every library copy of [name] into [playerId]'s hand, sending as many Islands back. */
    fun GameTestDriver.moveToOpeningHand(playerId: EntityId, name: String) {
        var s = state
        val library = ZoneKey(playerId, Zone.LIBRARY)
        val hand = ZoneKey(playerId, Zone.HAND)
        val wanted = s.getLibrary(playerId).filter { s.getEntity(it)?.get<CardComponent>()?.name == name }
        val outgoing = s.getHand(playerId).filter { s.getEntity(it)?.get<CardComponent>()?.name == "Island" }
            .take(wanted.size)
        for (id in wanted) s = s.removeFromZone(library, id).addToZone(hand, id)
        for (id in outgoing) s = s.removeFromZone(hand, id).addToZone(library, id)
        replaceState(s)
    }

    fun GameTestDriver.keepBoth() {
        submit(KeepHand(player1)).error shouldBe null
        submit(KeepHand(player2)).error shouldBe null
    }

    fun GameTestDriver.answerYesNo(choice: Boolean): YesNoDecision {
        val decision = pendingDecision
        decision.shouldBeInstanceOf<YesNoDecision>()
        submitYesNo(decision.playerId, choice).error shouldBe null
        return decision
    }

    fun GameTestDriver.exileFromHandPrompt(): SelectCardsDecision {
        val decision = pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        return decision
    }

    fun GameTestDriver.named(zone: ZoneKey) =
        state.getZone(zone).mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }

    fun GameTestDriver.cardNamed(playerId: EntityId, zone: Zone, name: String): EntityId =
        state.getZone(ZoneKey(playerId, zone)).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    test("the starting player is never offered a 'not the starting player' action") {
        val d = driver()
        d.initGame(Deck.of("Test Caverns" to 1, "Island" to 39), Deck.of("Island" to 40), skipMulligans = false)
        d.moveToOpeningHand(d.player1, "Test Caverns")
        d.keepBoth()

        withClue("P1 starts, so the walk has nothing to ask and turn 1 begins") {
            d.pendingDecision shouldBe null
            d.state.step shouldBe Step.UPKEEP
        }
        d.named(ZoneKey(d.player1, Zone.HAND)).count { it == "Test Caverns" } shouldBe 1
        d.named(ZoneKey(d.player1, Zone.BATTLEFIELD)) shouldBe emptyList()
    }

    test("a non-starting player begins with it on the battlefield, with its counter, then exiles a card") {
        val d = driver()
        d.initGame(Deck.of("Island" to 40), Deck.of("Test Caverns" to 1, "Island" to 39), skipMulligans = false)
        d.moveToOpeningHand(d.player2, "Test Caverns")
        d.keepBoth()

        val offer = d.answerYesNo(true)
        offer.playerId shouldBe d.player2
        offer.prompt shouldBe "Begin the game with Test Caverns on the battlefield?"
        offer.hint shouldBe caverns.oracleText

        val cavernsId = d.cardNamed(d.player2, Zone.BATTLEFIELD, "Test Caverns")
        withClue("The luck counter is part of the entry, already there when the rider asks") {
            d.state.getEntity(cavernsId)?.get<CountersComponent>()?.getCount(CounterType.LUCK) shouldBe 1
        }

        val exile = d.exileFromHandPrompt()
        exile.playerId shouldBe d.player2
        exile.options.size shouldBe 6
        val exiled = exile.options.first()
        d.submitCardSelection(d.player2, listOf(exiled)).error shouldBe null

        d.pendingDecision shouldBe null
        d.state.getZone(ZoneKey(d.player2, Zone.EXILE)) shouldBe listOf(exiled)
        d.state.getHand(d.player2).size shouldBe 5
        withClue("The walk finished, so the first turn began") {
            d.state.step shouldBe Step.UPKEEP
            d.activePlayer shouldBe d.player1
        }
    }

    test("declining leaves the card in hand and exiles nothing") {
        val d = driver()
        d.initGame(Deck.of("Island" to 40), Deck.of("Test Caverns" to 1, "Island" to 39), skipMulligans = false)
        d.moveToOpeningHand(d.player2, "Test Caverns")
        d.keepBoth()

        d.answerYesNo(false)
        d.pendingDecision shouldBe null
        d.state.getHand(d.player2).size shouldBe 7
        d.state.getZone(ZoneKey(d.player2, Zone.EXILE)) shouldBe emptyList()
        d.named(ZoneKey(d.player2, Zone.BATTLEFIELD)) shouldBe emptyList()
    }

    test("the walk resumes after the rider's question: the next card is still offered") {
        val d = driver()
        d.initGame(
            Deck.of("Island" to 40),
            Deck.of("Test Caverns" to 1, "Test Leyline" to 1, "Island" to 38),
            skipMulligans = false
        )
        d.moveToOpeningHand(d.player2, "Test Caverns")
        d.moveToOpeningHand(d.player2, "Test Leyline")
        d.keepBoth()

        // The walk asks in hand order; whichever comes first, the Caverns' exile question must not
        // end it — the other card is still offered afterwards.
        val asked = mutableListOf<String>()
        repeat(2) {
            val offer = d.answerYesNo(true)
            asked += offer.prompt
            if (offer.prompt == "Begin the game with Test Caverns on the battlefield?") {
                val exile = d.exileFromHandPrompt()
                val island = exile.options.first { d.state.getEntity(it)?.get<CardComponent>()?.name == "Island" }
                d.submitCardSelection(d.player2, listOf(island)).error shouldBe null
            }
        }
        asked.toSet() shouldBe setOf(
            "Begin the game with Test Caverns on the battlefield?",
            "Begin the game with Test Leyline on the battlefield?",
        )
        d.pendingDecision shouldBe null
        d.state.getZone(ZoneKey(d.player2, Zone.EXILE)).size shouldBe 1
        d.named(ZoneKey(d.player2, Zone.BATTLEFIELD)).toSet() shouldBe setOf("Test Caverns", "Test Leyline")
        d.state.step shouldBe Step.UPKEEP
    }

    test("a card's as-enters choice is made before its 'if you do' rider runs") {
        val d = driver()
        d.initGame(Deck.of("Test Chooser" to 1, "Island" to 39), Deck.of("Island" to 40), skipMulligans = false)
        d.moveToOpeningHand(d.player1, "Test Chooser")
        d.keepBoth()

        d.answerYesNo(true)
        val colorChoice = d.pendingDecision
        colorChoice.shouldBeInstanceOf<ChooseColorDecision>()
        d.submitDecision(d.player1, ColorChosenResponse(colorChoice.id, Color.RED)).error shouldBe null

        val exile = d.exileFromHandPrompt()
        d.submitCardSelection(d.player1, listOf(exile.options.first())).error shouldBe null

        d.pendingDecision shouldBe null
        d.state.getZone(ZoneKey(d.player1, Zone.EXILE)).size shouldBe 1
        d.named(ZoneKey(d.player1, Zone.BATTLEFIELD)) shouldBe listOf("Test Chooser")
        d.state.step shouldBe Step.UPKEEP
    }
})
