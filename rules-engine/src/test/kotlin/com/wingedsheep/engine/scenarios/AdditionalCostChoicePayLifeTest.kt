package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A pay-life option inside a cost-vs-cost [AdditionalCost.Choice] ("sacrifice a creature or pay 3
 * life"). Paying life fills no payment field, so the branch rides the declared choice slot.
 */
class AdditionalCostChoicePayLifeTest : FunSpec({
    val slot = ChoiceSlot.ADDITIONAL_COST_BRANCH
    val bargainer = card("Life Bargainer") {
        manaCost = "{0}"
        typeLine = "Creature — Horror"
        power = 2
        toughness = 2
        additionalCost(Costs.additional.Choice(
            Costs.additional.SacrificePermanent(GameObjectFilter.Creature),
            Costs.additional.PayLife(3),
            choiceSlot = slot,
        ))
    }
    fun setup(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + bargainer)
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.castActions(spell: com.wingedsheep.sdk.model.EntityId) =
        legalActions(activePlayer!!).filter { (it.action as? CastSpell)?.cardId == spell }

    test("a pay-life option needs a declared branch slot") {
        shouldThrow<IllegalArgumentException> {
            Costs.additional.Choice(Costs.additional.DiscardCards(1), Costs.additional.PayLife(3))
        }
    }

    test("each payable branch is its own cast action; the pay-life one needs no picker") {
        val d = setup()
        val p = d.activePlayer!!
        d.putPermanentOnBattlefield(p, "Grizzly Bears")
        val spell = d.putCardInHand(p, "Life Bargainer")
        val actions = d.castActions(spell)
        actions.map { (it.action as CastSpell).additionalCostChoices[slot] }.toSet() shouldBe setOf(0, 1)
        val payLife = actions.single { (it.action as CastSpell).additionalCostChoices[slot] == 1 }
        payLife.additionalCostInfo!!.costType shouldBe "PayLife"
        payLife.additionalCostInfo!!.description shouldBe "Pay 3 life"
    }

    test("paying the life branch loses that life and sacrifices nothing") {
        val d = setup()
        val p = d.activePlayer!!
        val bears = d.putPermanentOnBattlefield(p, "Grizzly Bears")
        val spell = d.putCardInHand(p, "Life Bargainer")
        val cast = d.castActions(spell).single { (it.action as CastSpell).additionalCostChoices[slot] == 1 }.action
        d.submit(cast).error shouldBe null
        d.getLifeTotal(p) shouldBe 17
        (bears in d.state.getBattlefield()) shouldBe true
        d.state.getEntity(spell)!!.get<SpellOnStackComponent>()!!.additionalCostChoices shouldBe mapOf(slot to 1)
        d.bothPass()
        (spell in d.state.getBattlefield()) shouldBe true
    }

    test("CR 119.4 - the life branch is offered at exactly that much life, not below") {
        val d = setup()
        val p = d.activePlayer!!
        d.putPermanentOnBattlefield(p, "Grizzly Bears")
        val spell = d.putCardInHand(p, "Life Bargainer")
        d.setLifeTotal(p, 3)
        d.castActions(spell).map { (it.action as CastSpell).additionalCostChoices[slot] }.toSet() shouldBe setOf(0, 1)
        d.setLifeTotal(p, 2)
        d.castActions(spell).map { (it.action as CastSpell).additionalCostChoices[slot] }.toSet() shouldBe setOf(0)
    }

    test("a declared life branch the caster can't afford is rejected without paying anything") {
        val d = setup()
        val p = d.activePlayer!!
        d.putPermanentOnBattlefield(p, "Grizzly Bears")
        val spell = d.putCardInHand(p, "Life Bargainer")
        d.setLifeTotal(p, 2)
        val before = d.state
        d.submit(CastSpell(p, spell, additionalCostChoices = mapOf(slot to 1))).error shouldNotBe null
        d.state shouldBe before
    }

    test("with no creature to sacrifice, paying life is the only way to cast it") {
        val d = setup()
        val p = d.activePlayer!!
        val spell = d.putCardInHand(p, "Life Bargainer")
        d.castActions(spell).map { (it.action as CastSpell).additionalCostChoices[slot] } shouldBe listOf(1)
        d.setLifeTotal(p, 2)
        d.castActions(spell) shouldBe emptyList()
    }

    test("a server-initiated cast offers the life branch and pays it once chosen") {
        val d = setup()
        val p = d.activePlayer!!
        val bears = d.putPermanentOnBattlefield(p, "Grizzly Bears")
        val spell = d.putCardInHand(p, "Life Bargainer")
        val result = d.services.castSpellHandler.execute(d.state, CastSpell(p, spell))
        result.error shouldBe null
        d.replaceState(result.state)
        val decision = d.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.options.size shouldBe 2
        decision.options[1].lowercase() shouldBe "pay 3 life"
        d.submitDecision(p, OptionChosenResponse(decision.id, 1)).error shouldBe null
        d.getLifeTotal(p) shouldBe 17
        (bears in d.state.getBattlefield()) shouldBe true
    }
})
