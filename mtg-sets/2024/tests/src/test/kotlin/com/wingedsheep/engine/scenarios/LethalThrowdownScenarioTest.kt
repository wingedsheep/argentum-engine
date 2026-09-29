package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.LethalThrowdown
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class LethalThrowdownScenarioTest : FunSpec({
    fun setup(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + LethalThrowdown)
        it.initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    for (branch in listOf(0, 1)) {
        test("modified creature paid through branch $branch preserves the voluntary choice") {
            val d = setup()
            val p = d.activePlayer!!
            val opponent = d.state.turnOrder.first { it != p }
            val fodder = d.putPermanentOnBattlefield(p, "Grizzly Bears")
            d.replaceState(d.state.updateEntity(fodder) { it.with(CountersComponent(mapOf(CounterType.CHARGE to 1))) })
            val victim = d.putPermanentOnBattlefield(opponent, "Grizzly Bears")
            val spell = d.putCardInHand(p, "Lethal Throwdown")
            d.giveMana(p, Color.BLACK, 1)
            val actions = d.legalActions(p).filter { (it.action as? CastSpell)?.cardId == spell }
            actions.map { (it.action as CastSpell).additionalCostChoices[ChoiceSlot.ADDITIONAL_COST_BRANCH] }.toSet() shouldBe setOf(0, 1)
            val cast = actions.single { (it.action as CastSpell).additionalCostChoices[ChoiceSlot.ADDITIONAL_COST_BRANCH] == branch }.action as CastSpell
            val handBefore = d.state.getHand(p).size
            d.submit(cast.copy(
                targets = listOf(ChosenTarget.Permanent(victim)),
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
                paymentStrategy = PaymentStrategy.FromPool,
            )).error shouldBe null
            (fodder in d.state.getZone(ZoneKey(p, Zone.GRAVEYARD))) shouldBe true
            d.bothPass()
            (victim in d.state.getZone(ZoneKey(opponent, Zone.GRAVEYARD))) shouldBe true
            d.state.getHand(p).size shouldBe handBefore - 1 + branch
        }
    }

    test("modified branch rejects an unmodified sacrifice without paying anything") {
        val d = setup()
        val p = d.activePlayer!!
        val fodder = d.putPermanentOnBattlefield(p, "Grizzly Bears")
        val victim = d.putPermanentOnBattlefield(p, "Grizzly Bears")
        val spell = d.putCardInHand(p, "Lethal Throwdown")
        d.giveMana(p, Color.BLACK, 1)
        val before = d.state
        d.submit(CastSpell(p, spell, targets = listOf(ChosenTarget.Permanent(victim)),
            additionalCostChoices = mapOf(ChoiceSlot.ADDITIONAL_COST_BRANCH to 1),
            additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldNotBe null
        d.state shouldBe before
    }

    test("no legal target on resolution means no draw even after modified sacrifice") {
        val d = setup()
        val p = d.activePlayer!!
        val fodder = d.putPermanentOnBattlefield(p, "Grizzly Bears")
        d.replaceState(d.state.updateEntity(fodder) { it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1))) })
        val victim = d.putPermanentOnBattlefield(p, "Grizzly Bears")
        val spell = d.putCardInHand(p, "Lethal Throwdown")
        d.giveMana(p, Color.BLACK, 1)
        d.submit(CastSpell(p, spell, targets = listOf(ChosenTarget.Permanent(victim)),
            additionalCostChoices = mapOf(ChoiceSlot.ADDITIONAL_COST_BRANCH to 1),
            additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
            paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
        val before = d.state.getHand(p).size
        d.moveToGraveyard(victim)
        d.bothPass()
        d.state.getHand(p).size shouldBe before
    }
})
