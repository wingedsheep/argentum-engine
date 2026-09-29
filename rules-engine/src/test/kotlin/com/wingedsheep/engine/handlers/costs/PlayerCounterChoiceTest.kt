package com.wingedsheep.engine.handlers.costs

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.PlayWithAdditionalCostComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PlayerCounterChoiceTest : FunSpec({
    val spender = card("Counter Choice Spender") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Composite(
                Costs.PayPlayerCounters(CounterType.ENERGY, 1),
                Costs.PayPlayerCounters(CounterType.ENERGY, DynamicAmount.XValue),
                Costs.PayPlayerCounters(CounterType.ENERGY, DynamicAmount.XValue)
            )
            effect = Effects.GainLife(DynamicAmount.XValue)
        }
    }

    test("bare activation prompts for X and reserves fixed and repeated counter payments") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + spender)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(me, spender.name)
        d.addComponent(me, CountersComponent(mapOf(CounterType.ENERGY to 5)))
        val action = d.legalActions(me).single { (it.action as? ActivateAbility)?.sourceId == source }
        action.maxAffordableX shouldBe 2
        (d.submit(action.action).outcome is Outcome.Paused) shouldBe true
        val question = d.pendingDecision as ChooseNumberDecision
        question.maxValue shouldBe 2
        d.state.getEntity(me)!!.get<CountersComponent>()!!.getCount(CounterType.ENERGY) shouldBe 5
        d.submitDecision(me, NumberChosenResponse(question.id, 2)).error shouldBe null
        d.state.getEntity(me)!!.get<CountersComponent>()!!.getCount(CounterType.ENERGY) shouldBe 0
        d.bothPass()
        d.state.lifeTotal(me) shouldBe 22
    }

    test("defined X on a bare activation pays the fixed value without a number prompt") {
        val defined = card("Defined Counter Choice Spender") {
            manaCost = "{1}"
            typeLine = "Artifact"
            activatedAbility {
                cost = Costs.PayPlayerCounters(CounterType.ENERGY, DynamicAmount.XValue)
                xDefinedAs = DynamicAmount.Fixed(3)
                effect = Effects.GainLife(DynamicAmount.XValue)
            }
        }
        val d = GameTestDriver()
        d.registerCards(TestCards.all + defined)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(me, defined.name)
        d.addComponent(me, CountersComponent(mapOf(CounterType.ENERGY to 5)))
        val action = d.legalActions(me).single { (it.action as? ActivateAbility)?.sourceId == source }
        action.hasXCost shouldBe false
        d.submit(action.action).error shouldBe null
        d.pendingDecision shouldBe null
        d.state.getEntity(me)!!.get<CountersComponent>()!!.getCount(CounterType.ENERGY) shouldBe 2
        d.bothPass()
        d.state.lifeTotal(me) shouldBe 23
    }


    test("flashback exposes player-counter X and reserves its bundled fixed additional cost") {
        val spell = card("Counter Flashback Spell") {
            manaCost = "{0}"
            typeLine = "Sorcery"
            keywordAbility(KeywordAbility.flashback("{0}", Costs.additional.PayPlayerCounters(CounterType.ENERGY, 2)))
            additionalCost(Costs.additional.PayPlayerCounters(CounterType.ENERGY, DynamicAmount.XValue))
            spell { effect = Effects.GainLife(DynamicAmount.XValue) }
        }
        val d = GameTestDriver()
        d.registerCards(TestCards.all + spell)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val cardId = d.putCardInGraveyard(me, spell.name)
        d.addComponent(me, CountersComponent(mapOf(CounterType.ENERGY to 5)))
        val offer = d.legalActions(me).single { (it.action as? CastSpell)?.cardId == cardId }
        offer.hasXCost shouldBe true
        offer.maxAffordableX shouldBe 3
        d.submit((offer.action as CastSpell).copy(xValue = 3)).error shouldBe null
        d.state.getEntity(me)!!.get<CountersComponent>()!!.getCount(CounterType.ENERGY) shouldBe 0
        d.bothPass()
        d.state.lifeTotal(me) shouldBe 23
    }

    test("exile permission costs combine with printed counter costs for affordability and X") {
        val spell = card("Permission Counter Spell") {
            manaCost = "{0}"
            typeLine = "Sorcery"
            additionalCost(Costs.additional.PayPlayerCounters(CounterType.ENERGY, 2))
            additionalCost(Costs.additional.PayPlayerCounters(CounterType.ENERGY, DynamicAmount.XValue))
            spell { effect = Effects.GainLife(DynamicAmount.XValue) }
        }
        val d = GameTestDriver()
        d.registerCards(TestCards.all + spell)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val cardId = d.putCardInExile(me, spell.name)
        d.replaceState(d.state.addMayPlayPermission(MayPlayPermission(
            id = EntityId.generate(), cardIds = setOf(cardId), controllerId = me, timestamp = d.state.timestamp
        )))
        d.addComponent(cardId, PlayWithAdditionalCostComponent(me,
            listOf(Costs.additional.PayPlayerCounters(CounterType.ENERGY, 2))))
        d.addComponent(me, CountersComponent(mapOf(CounterType.ENERGY to 3)))
        d.legalActions(me).filter { (it.action as? CastSpell)?.cardId == cardId }
            .any { it.affordable } shouldBe false
        d.addComponent(me, CountersComponent(mapOf(CounterType.ENERGY to 5)))
        val offer = d.legalActions(me).single { (it.action as? CastSpell)?.cardId == cardId }
        offer.affordable shouldBe true
        offer.hasXCost shouldBe true
        offer.maxAffordableX shouldBe 1
        d.submit((offer.action as CastSpell).copy(xValue = 1)).error shouldBe null
        d.state.getEntity(me)!!.get<CountersComponent>()!!.getCount(CounterType.ENERGY) shouldBe 0
    }

})
