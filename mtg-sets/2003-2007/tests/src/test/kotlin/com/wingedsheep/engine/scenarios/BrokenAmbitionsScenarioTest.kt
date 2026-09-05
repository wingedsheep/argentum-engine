package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CantBeCounteredComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lrw.cards.BrokenAmbitions
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class BrokenAmbitionsScenarioTest : FunSpec({
    val boulder = card("Ambitions Boulder") { manaCost = "{5}"; typeLine = "Artifact"; oracleText = "" }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(BrokenAmbitions, boulder))
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    for (mode in listOf("cannot pay", "pay", "decline", "zero", "uncounterable")) {
        for (win in listOf(true, false)) {
            test("$mode and clash win=$win preserves the spell controller independently of its owner") {
                val d = driver()
                val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
                d.giveMana(d.player1, Color.RED, 1)
                d.castSpell(d.player1, bolt, listOf(d.player2)).error shouldBe null
                // Represent a spell cast by someone other than its owner. The real stack caster
                // remains player1, while the card must go to player2's graveyard if countered.
                d.replaceState(d.state.updateEntity(bolt) { entity ->
                    var updated = entity.with(entity.get<CardComponent>()!!.copy(ownerId = d.player2))
                        .with(OwnerComponent(d.player2))
                    if (mode == "uncounterable") updated = updated.with(CantBeCounteredComponent)
                    updated
                })
                d.passPriority(d.player1).error shouldBe null
                val x = if (mode == "zero") 0 else 2
                if (mode == "pay" || mode == "decline") d.giveMana(d.player1, Color.WHITE, 2)
                val ambitions = d.putCardInHand(d.player2, "Broken Ambitions")
                d.giveMana(d.player2, Color.BLUE, x + 1)
                d.submit(CastSpell(d.player2, ambitions, targets = listOf(ChosenTarget.Spell(bolt)), xValue = x, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
                d.putCardOnTopOfLibrary(d.player2, if (win) "Ambitions Boulder" else "Plains")
                val top = d.putCardOnTopOfLibrary(d.player1, "Plains")
                val libraryBefore = d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)).size
                val otherLibraryBefore = d.state.getZone(ZoneKey(d.player2, Zone.LIBRARY)).size
                d.bothPass().error shouldBe null
                if (mode == "pay" || mode == "decline") {
                    d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe d.player1
                    d.submitYesNo(d.player1, mode == "pay").error shouldBe null
                }
                repeat(2) {
                    val choice = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                    d.submitCardSelection(choice.playerId, emptyList()).error shouldBe null
                }
                d.pendingDecision shouldBe null
                val countered = mode == "cannot pay" || mode == "decline"
                (bolt in d.state.stack) shouldBe !countered
                (bolt in d.state.getZone(ZoneKey(d.player2, Zone.GRAVEYARD))) shouldBe countered
                d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)).size shouldBe libraryBefore - if (win) 4 else 0
                d.state.getZone(ZoneKey(d.player2, Zone.LIBRARY)).size shouldBe otherLibraryBefore
                (top in d.state.getZone(ZoneKey(d.player1, Zone.GRAVEYARD))) shouldBe win
                (ambitions in d.state.getZone(ZoneKey(d.player2, Zone.GRAVEYARD))) shouldBe true
            }
        }
    }

    test("a target removed from the stack prevents the clash and milling") {
        val d = driver()
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, bolt, listOf(d.player2)).error shouldBe null
        d.passPriority(d.player1).error shouldBe null
        val ambitions = d.putCardInHand(d.player2, "Broken Ambitions")
        d.giveMana(d.player2, Color.BLUE, 3)
        d.submit(CastSpell(d.player2, ambitions, targets = listOf(ChosenTarget.Spell(bolt)),
            xValue = 2, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.passPriority(d.player2).error shouldBe null
        val counter = d.putCardInHand(d.player1, "Counterspell")
        d.giveMana(d.player1, Color.BLUE, 2)
        d.submit(CastSpell(d.player1, counter, targets = listOf(ChosenTarget.Spell(bolt)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        val libraries = listOf(d.player1, d.player2).map { d.state.getZone(ZoneKey(it, Zone.LIBRARY)) }
        d.bothPass().error shouldBe null
        d.bothPass().error shouldBe null
        d.pendingDecision shouldBe null
        listOf(d.player1, d.player2).map { d.state.getZone(ZoneKey(it, Zone.LIBRARY)) } shouldBe libraries
        (ambitions in d.state.getZone(ZoneKey(d.player2, Zone.GRAVEYARD))) shouldBe true
    }


    test("winning a clash with a third player mills only the targeted spell's controller") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(BrokenAmbitions, boulder))
        val (a, b, c) = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) }, startingPlayer = 1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bolt = d.putCardInHand(b, "Lightning Bolt")
        d.giveMana(b, Color.RED, 1)
        d.castSpell(b, bolt, listOf(a)).error shouldBe null
        while (d.priorityPlayer != a) d.passPriority(d.priorityPlayer!!).error shouldBe null
        val ambitions = d.putCardInHand(a, "Broken Ambitions")
        d.giveMana(a, Color.BLUE, 3)
        d.submit(CastSpell(a, ambitions, targets = listOf(ChosenTarget.Spell(bolt)),
            xValue = 2, paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        d.putCardOnTopOfLibrary(a, "Ambitions Boulder")
        d.putCardOnTopOfLibrary(c, "Plains")
        val libraries = listOf(a, b, c).associateWith { d.state.getZone(ZoneKey(it, Zone.LIBRARY)) }
        repeat(3) { d.passPriority(d.priorityPlayer!!).error shouldBe null }
        val opponent = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        opponent.playerId shouldBe a
        val cIndex = d.state.getOpponents(a).indexOf(c)
        d.submit(SubmitDecision(a, OptionChosenResponse(opponent.id, optionIndex = cIndex))).error shouldBe null
        repeat(2) {
            val choice = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            (choice.playerId in listOf(a, c)) shouldBe true
            d.submitCardSelection(choice.playerId, emptyList()).error shouldBe null
        }
        d.pendingDecision shouldBe null
        (bolt in d.state.getZone(ZoneKey(b, Zone.GRAVEYARD))) shouldBe true
        d.state.getZone(ZoneKey(b, Zone.LIBRARY)) shouldBe libraries.getValue(b).drop(4)
        d.state.getZone(ZoneKey(a, Zone.LIBRARY)) shouldBe libraries.getValue(a)
        d.state.getZone(ZoneKey(c, Zone.LIBRARY)) shouldBe libraries.getValue(c)
    }

})
