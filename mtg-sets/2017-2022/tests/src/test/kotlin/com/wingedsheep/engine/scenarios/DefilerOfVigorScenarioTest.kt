package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DefilerOfVigor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DefilerOfVigorScenarioTest : FunSpec({
    val permanent = card("DefilerOfVigor Test Artifact") { manaCost = "{G}"; typeLine = "Artifact" }
    val instant = card("DefilerOfVigor Test Instant") { manaCost = "{G}"; typeLine = "Instant" }
    val bear = card("DefilerOfVigor Test Bear") { typeLine = "Creature — Bear"; power = 2; toughness = 2 }
    fun setup() = GameTestDriver().apply {
        registerCards(listOf(DefilerOfVigor, DominariaUnitedForest274, permanent, instant, bear))
        initMirrorMatch(deck = Deck.of("Forest" to 30), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    for (payLife in listOf(false, true)) test("casting a green permanent spell triggers, paying 2 life: $payLife") {
        val d = setup(); val p = d.activePlayer!!
        val source = d.putCreatureOnBattlefield(p, DefilerOfVigor.name)
        val ownBear = d.putCreatureOnBattlefield(p, bear.name)
        val theirBear = d.putCreatureOnBattlefield(d.getOpponent(p), bear.name)
        val id = d.putCardInHand(p, permanent.name)
        if (!payLife) d.giveMana(p, Color.GREEN)
        val action = d.legalActions(p).single {
            (it.action as? CastSpell)?.let { a -> a.cardId == id && a.optionalCostPayments.isNotEmpty() == payLife } == true
        }.action
        d.submit(action).error shouldBe null
        d.getLifeTotal(p) shouldBe if (payLife) 18 else 20
        d.state.stack.size shouldBe 2
        d.bothPass().error shouldBe null
        d.state.projectedState.getPower(source) shouldBe 7
        d.state.projectedState.getToughness(source) shouldBe 7
        d.state.projectedState.getPower(ownBear) shouldBe 3
        d.state.projectedState.getPower(theirBear) shouldBe 2
        d.state.stack shouldBe listOf(id)
    }
    test("a green instant gets no discount and no trigger") {
        val d = setup(); val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, DefilerOfVigor.name)
        val id = d.putCardInHand(p, instant.name); d.giveMana(p, Color.GREEN)
        d.legalActions(p).filter { (it.action as? CastSpell)?.cardId == id }.all {
            (it.action as CastSpell).optionalCostPayments.isEmpty()
        } shouldBe true
        d.submit(CastSpell(p, id)).error shouldBe null
        d.state.stack shouldBe listOf(id)
    }
})
