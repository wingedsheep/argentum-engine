package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.event.TriggerContext
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class AdditionalCostChoiceTest : FunSpec({
    val slot = ChoiceSlot.ADDITIONAL_COST_BRANCH
    val cost = Costs.additional.Choice(
        Costs.additional.SacrificePermanent(GameObjectFilter.Creature),
        Costs.additional.SacrificePermanent(GameObjectFilter.Land),
        choiceSlot = slot,
    )
    val creature = card("Branch Keeper") {
        manaCost = "{0}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        additionalCost(cost)
        triggeredAbility {
            trigger = Triggers.self.isCast()
            interveningIf = Conditions.CastChoiceIs(slot, "1")
            effect = Effects.GainLife(3)
        }
    }
    fun setup(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + creature)
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("explicit choice serializes with its identity in SDK and cast action") {
        Json.decodeFromString<AdditionalCost>(Json.encodeToString(cost)) shouldBe cost
        val d = setup()
        val action = CastSpell(d.activePlayer!!, d.putCardInHand(d.activePlayer!!, "Branch Keeper"),
            additionalCostChoices = mapOf(slot to 1))
        Json.decodeFromString<CastSpell>(Json.encodeToString(action)) shouldBe action
    }

    for (index in listOf(-1, 2)) {
        test("forged branch $index is rejected without payment") {
            val d = setup()
            val p = d.activePlayer!!
            val fodder = d.putPermanentOnBattlefield(p, "Forest")
            val spell = d.putCardInHand(p, "Branch Keeper")
            val before = d.state
            d.submit(CastSpell(p, spell, additionalCostChoices = mapOf(slot to index),
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)))).error shouldNotBe null
            d.state shouldBe before
        }
    }

    test("undeclared cost slot is rejected") {
        val d = setup()
        val p = d.activePlayer!!
        val spell = d.putCardInHand(p, "Grizzly Bears")
        d.giveMana(p, Color.GREEN, 2)
        d.submit(CastSpell(p, spell, additionalCostChoices = mapOf(slot to 1))).error shouldNotBe null
    }

    test("enumeration keeps the declared index when earlier options are unpayable") {
        val d = setup()
        val p = d.activePlayer!!
        d.putPermanentOnBattlefield(p, "Forest")
        val spell = d.putCardInHand(p, "Branch Keeper")
        val actions = d.legalActions(p).filter { (it.action as? CastSpell)?.cardId == spell }
        actions.size shouldBe 1
        (actions.single().action as CastSpell).additionalCostChoices shouldBe mapOf(slot to 1)
    }

    test("unannounced branch prompts before payment and cancel leaves costs untouched") {
        val d = setup()
        val p = d.activePlayer!!
        val land = d.putPermanentOnBattlefield(p, "Forest")
        val spell = d.putCardInHand(p, "Branch Keeper")
        d.submit(CastSpell(p, spell)).error shouldBe null
        val decision = d.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        d.submitDecision(p, CancelDecisionResponse(decision.id)).error shouldBe null
        (spell in d.state.getHand(p)) shouldBe true
        (land in d.state.getBattlefield()) shouldBe true
        d.state.stack shouldBe emptyList()
    }

    test("server-initiated cast asks for a branch then pays exactly that branch") {
        val d = setup()
        val p = d.activePlayer!!
        d.putPermanentOnBattlefield(p, "Grizzly Bears")
        val land = d.putPermanentOnBattlefield(p, "Forest")
        val spell = d.putCardInHand(p, "Branch Keeper")
        val result = d.services.castSpellHandler.execute(d.state, CastSpell(p, spell))
        result.error shouldBe null
        d.replaceState(result.state)
        val decision = d.state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        d.submitDecision(p, OptionChosenResponse(decision.id, 1)).error shouldBe null
        (land in d.state.getBattlefield()) shouldBe false
        d.state.getEntity(spell)!!.get<SpellOnStackComponent>()!!.additionalCostChoices shouldBe mapOf(slot to 1)
        d.bothPass() // self-cast life trigger
        d.getLifeTotal(p) shouldBe 23
        d.bothPass() // creature
        d.state.getEntity(spell)!!.get<CastChoicesComponent>()!!.chosen[slot] shouldBe ChoiceValue.NumberChoice(1)
        d.replaceState(d.zones.moveToZone(d.state, spell, Zone.GRAVEYARD).state)
        d.state.getEntity(spell)!!.get<CastChoicesComponent>() shouldBe null
    }

    test("self-cast branch snapshot survives source removal and does not read a later cast") {
        val d = setup()
        val p = d.activePlayer!!
        val land = d.putPermanentOnBattlefield(p, "Forest")
        val spell = d.putCardInHand(p, "Branch Keeper")
        d.submit(CastSpell(p, spell, additionalCostChoices = mapOf(slot to 1),
            additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(land)))).error shouldBe null
        d.state.stack.size shouldBe 2
        val context = EffectContext(sourceId = spell, controllerId = p, triggerContext = TriggerContext(
            triggeringEntityId = spell, selfCastAdditionalCostChoices = mapOf(slot to 1)))
        d.replaceState(d.zones.moveToZone(d.state, spell, Zone.GRAVEYARD).state)
        d.services.conditionEvaluator.evaluate(d.state, Conditions.CastChoiceIs(slot, "1"), context) shouldBe true
        d.services.conditionEvaluator.evaluate(d.state, Conditions.CastChoiceMade(slot), context) shouldBe true
        d.services.dynamicAmountEvaluator.evaluate(d.state,
            com.wingedsheep.sdk.scripting.values.DynamicAmount.CastChoice(slot), context) shouldBe 1
        val other = context.copy(sourceId = d.state.turnOrder.first { it != p })
        d.services.conditionEvaluator.evaluate(d.state, Conditions.CastChoiceIs(slot, "1"), other) shouldBe false
        val later = d.state.updateEntity(spell) { it.with(SpellOnStackComponent(
            casterId = p, additionalCostChoices = mapOf(slot to 0))) }
        d.services.conditionEvaluator.evaluate(later, Conditions.CastChoiceIs(slot, "1"), context) shouldBe true
        d.services.dynamicAmountEvaluator.evaluate(later,
            com.wingedsheep.sdk.scripting.values.DynamicAmount.CastChoice(slot), context) shouldBe 1
        d.services.conditionEvaluator.evaluate(d.state, Conditions.CastChoiceIs(slot, "0"), context) shouldBe false
        d.bothPass()
        d.getLifeTotal(p) shouldBe 23
    }
})
