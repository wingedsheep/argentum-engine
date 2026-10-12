package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.mtg.sets.definitions.pip.cards.RuthlessRadrat

class RuthlessRadratScenarioTest : FunSpec({
    fun setup(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + RuthlessRadrat)
        it.initMirrorMatch(Deck.of("Swamp" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("two squad payments exile eight distinct cards and create two copies") {
        val d = setup()
        val player = d.activePlayer!!
        repeat(3) { d.putLandOnBattlefield(player, "Swamp") }
        val grave = List(9) { d.putCardInGraveyard(player, "Swamp") }
        val source = d.putCardInHand(player, "Ruthless Radrat")
        d.submit(CastSpell(player, source, declaredCostSlot = ChoiceSlot.SQUAD, declaredCostTimes = 2,
            additionalCostPayment = AdditionalCostPayment(exiledCards = grave.take(8)))).outcome shouldBe Outcome.Done
        d.getGraveyard(player).size shouldBe 1
        d.bothPass()
        d.bothPass()
        d.getPermanents(player).count { d.state.getEntity(it)?.has<TokenComponent>() == true } shouldBe 2
    }

    test("insufficient or reused exile payments are rejected without moving cards") {
        val d = setup()
        val player = d.activePlayer!!
        repeat(3) { d.putLandOnBattlefield(player, "Swamp") }
        val grave = List(7) { d.putCardInGraveyard(player, "Swamp") }
        val source = d.putCardInHand(player, "Ruthless Radrat")
        for (payment in listOf(grave, grave + grave.first())) {
            d.submit(CastSpell(player, source, declaredCostSlot = ChoiceSlot.SQUAD, declaredCostTimes = 2,
                additionalCostPayment = AdditionalCostPayment(exiledCards = payment))).outcome shouldNotBe Outcome.Done
            d.getGraveyard(player).size shouldBe 7
        }
    }

    test("a fixed squad exile payment cannot exile extra cards") {
        val d = setup()
        val player = d.activePlayer!!
        repeat(3) { d.putLandOnBattlefield(player, "Swamp") }
        val grave = List(9) { d.putCardInGraveyard(player, "Swamp") }
        val source = d.putCardInHand(player, "Ruthless Radrat")
        d.submit(CastSpell(player, source, declaredCostSlot = ChoiceSlot.SQUAD, declaredCostTimes = 2,
            additionalCostPayment = AdditionalCostPayment(exiledCards = grave))).outcome shouldNotBe Outcome.Done
        d.getGraveyard(player).size shouldBe 9
    }

    test("the cast menu offers only affordable squad counts with the scaled exile picker") {
        val d = setup()
        val player = d.activePlayer!!
        repeat(3) { d.putLandOnBattlefield(player, "Swamp") }
        repeat(9) { d.putCardInGraveyard(player, "Swamp") }
        d.putCardInHand(player, "Ruthless Radrat")
        val actions = LegalActionEnumerator.create(d.cardRegistry).enumerate(d.state, player)
        val squad = actions.filter { (it.action as? CastSpell)?.declaredCostSlot == ChoiceSlot.SQUAD }
        squad.map { (it.action as CastSpell).declaredCostTimes } shouldBe listOf(1, 2)
        squad.map { it.description } shouldBe listOf("Cast Ruthless Radrat (Squad ×1)", "Cast Ruthless Radrat (Squad ×2)")
        squad.all { it.affordable } shouldBe true
    }
})
