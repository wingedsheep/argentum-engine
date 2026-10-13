package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.mtg.sets.definitions.dmu.cards.KeldonFlamesage
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class KeldonFlamesageScenarioTest : FunSpec({
    val two = card("Flamesage Two") { manaCost = "{1}{R}"; typeLine = "Sorcery"; spell { effect = Effects.GainLife(5) } }
    val three = card("Flamesage Three") { manaCost = "{2}{R}"; typeLine = "Sorcery"; spell { effect = Effects.GainLife(7) } }
    val helper = card("Flamesage Helper") { typeLine = "Creature — Bear"; power = 1; toughness = 1 }
    fun setup() = GameTestDriver().apply {
        registerCards(listOf(KeldonFlamesage, DominariaUnitedForest274, two, three, helper))
        initMirrorMatch(deck = Deck.of("Forest" to 30), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.attack(enlist: List<EntityId> = emptyList()): EntityId {
        val p = activePlayer!!
        val a = putCreatureOnBattlefield(p, KeldonFlamesage.name); removeSummoningSickness(a)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(p, listOf(a), getOpponent(p)).error shouldBe null
        if (pendingDecision is SelectCardsDecision) submitCardSelection(p, enlist).error shouldBe null
        return a
    }
    test("looks at the top X cards, exiles an instant or sorcery with mana value X or less, and casts it free") {
        val d = setup(); val p = d.activePlayer!!
        val rest = d.state.getLibrary(p).first()
        val spell = d.putCardOnTopOfLibrary(p, two.name)
        d.state.stack.size shouldBe 0
        d.attack()
        d.state.stack.size shouldBe 1
        d.bothPass().error shouldBe null
        val choice = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        // Only the eligible card is selectable; the other looked-at card is shown greyed out.
        choice.options shouldContainExactly listOf(spell)
        choice.nonSelectableOptions.size shouldBe 1
        d.submitCardSelection(p, listOf(spell)).error shouldBe null
        d.getExile(p) shouldContain spell
        // The other looked-at card goes to the bottom of the library.
        d.state.getLibrary(p).last() shouldBe rest
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(p, true).error shouldBe null
        repeat(3) { if (d.pendingDecision == null && d.state.stack.isNotEmpty()) d.bothPass() }
        d.getLifeTotal(p) shouldBe 25
        d.getGraveyard(p) shouldContain spell
    }
    test("a card with mana value greater than X can't be chosen") {
        val d = setup(); val p = d.activePlayer!!
        val spell = d.putCardOnTopOfLibrary(p, three.name)
        d.attack()
        d.bothPass().error shouldBe null
        val choice = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        choice.options shouldBe emptyList()
        choice.nonSelectableOptions shouldContain spell
        d.submitCardSelection(p, emptyList()).error shouldBe null
        d.pendingDecision shouldBe null
        d.getExile(p).contains(spell) shouldBe false
    }
    test("an enlist boost that resolves first raises X") {
        val d = setup(); val p = d.activePlayer!!
        val spell = d.putCardOnTopOfLibrary(p, three.name)
        val h = d.putCreatureOnBattlefield(p, helper.name); d.removeSummoningSickness(h)
        d.attack(enlist = listOf(h))
        // The enlist boost is put on the stack above the attack trigger, so it resolves first.
        d.state.stack.size shouldBe 2
        d.bothPass().error shouldBe null
        if (d.pendingDecision is SelectCardsDecision) error("attack trigger resolved before the enlist boost")
        d.bothPass().error shouldBe null
        val choice = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.state.projectedState.getPower(d.findPermanent(p, KeldonFlamesage.name)!!) shouldBe 3
        choice.options shouldContainExactly listOf(spell)
        choice.nonSelectableOptions.size shouldBe 2
        d.submitCardSelection(p, listOf(spell)).error shouldBe null
        d.submitYesNo(p, false).error shouldBe null
        d.getExile(p) shouldContainExactly listOf(spell)
    }
})
