package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DefilerOfFaith
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DefilerOfFaithScenarioTest : FunSpec({
    val permanent = card("DefilerOfFaith Test Artifact") { manaCost = "{W}"; typeLine = "Artifact" }
    val instant = card("DefilerOfFaith Test Instant") { manaCost = "{W}"; typeLine = "Instant" }
    fun setup() = GameTestDriver().apply {
        registerCards(listOf(DefilerOfFaith, DominariaUnitedForest274, permanent, instant))
        initMirrorMatch(deck = Deck.of("Forest" to 30), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    for (payLife in listOf(false, true)) test("casting a white permanent spell triggers, paying 2 life: $payLife") {
        val d = setup(); val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, DefilerOfFaith.name)
        val id = d.putCardInHand(p, permanent.name)
        if (!payLife) d.giveMana(p, Color.WHITE)
        val action = d.legalActions(p).single {
            (it.action as? CastSpell)?.let { a -> a.cardId == id && a.optionalCostPayments.isNotEmpty() == payLife } == true
        }.action
        d.submit(action).error shouldBe null
        d.getLifeTotal(p) shouldBe if (payLife) 18 else 20
        d.state.stack.size shouldBe 2
        d.bothPass().error shouldBe null
        val tokens = d.getCreatures(p).filter { d.state.projectedState.getSubtypes(it).contains("Soldier") }
        tokens.size shouldBe 1
        d.state.projectedState.getPower(tokens.single()) shouldBe 1
        d.state.stack shouldBe listOf(id)
    }
    test("a white instant gets no discount and no trigger") {
        val d = setup(); val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, DefilerOfFaith.name)
        val id = d.putCardInHand(p, instant.name); d.giveMana(p, Color.WHITE)
        d.legalActions(p).filter { (it.action as? CastSpell)?.cardId == id }.all {
            (it.action as CastSpell).optionalCostPayments.isEmpty()
        } shouldBe true
        d.submit(CastSpell(p, id)).error shouldBe null
        d.state.stack shouldBe listOf(id)
    }
})
