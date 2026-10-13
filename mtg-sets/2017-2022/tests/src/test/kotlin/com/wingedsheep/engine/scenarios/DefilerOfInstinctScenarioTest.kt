package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DefilerOfInstinct
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DefilerOfInstinctScenarioTest : FunSpec({
    val permanent = card("DefilerOfInstinct Test Artifact") { manaCost = "{R}"; typeLine = "Artifact" }
    val instant = card("DefilerOfInstinct Test Instant") { manaCost = "{R}"; typeLine = "Instant" }
    fun setup() = GameTestDriver().apply {
        registerCards(listOf(DefilerOfInstinct, DominariaUnitedForest274, permanent, instant))
        initMirrorMatch(deck = Deck.of("Forest" to 30), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    for (payLife in listOf(false, true)) test("casting a red permanent spell triggers, paying 2 life: $payLife") {
        val d = setup(); val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, DefilerOfInstinct.name)
        val id = d.putCardInHand(p, permanent.name)
        if (!payLife) d.giveMana(p, Color.RED)
        val action = d.legalActions(p).single {
            (it.action as? CastSpell)?.let { a -> a.cardId == id && a.optionalCostPayments.isNotEmpty() == payLife } == true
        }.action
        d.submit(action).error shouldBe null
        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitTargetSelection(p, listOf(d.getOpponent(p))).error shouldBe null
        d.getLifeTotal(p) shouldBe if (payLife) 18 else 20
        d.state.stack.size shouldBe 2
        d.bothPass().error shouldBe null
        d.getLifeTotal(d.getOpponent(p)) shouldBe 19
        d.state.stack shouldBe listOf(id)
    }
    test("a red instant gets no discount and no trigger") {
        val d = setup(); val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, DefilerOfInstinct.name)
        val id = d.putCardInHand(p, instant.name); d.giveMana(p, Color.RED)
        d.legalActions(p).filter { (it.action as? CastSpell)?.cardId == id }.all {
            (it.action as CastSpell).optionalCostPayments.isEmpty()
        } shouldBe true
        d.submit(CastSpell(p, id)).error shouldBe null
        d.state.stack shouldBe listOf(id)
    }
})
