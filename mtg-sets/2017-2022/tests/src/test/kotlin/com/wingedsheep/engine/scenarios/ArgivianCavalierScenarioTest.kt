package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.ArgivianCavalier
import com.wingedsheep.mtg.sets.definitions.dmu.cards.DominariaUnitedForest274
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ArgivianCavalierScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(listOf(ArgivianCavalier, DominariaUnitedForest274))
        initMirrorMatch(deck = Deck.of("Forest" to 30), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    test("entering creates a 1/1 white Soldier that can then be enlisted") {
        val d = setup(); val p = d.activePlayer!!
        val id = d.putCardInHand(p, ArgivianCavalier.name)
        d.giveMana(p, Color.WHITE); d.giveColorlessMana(p, 2)
        d.castSpell(p, id).error shouldBe null
        d.bothPass().error shouldBe null // resolve the creature spell
        d.state.stack.size shouldBe 1
        d.bothPass().error shouldBe null // resolve the enters trigger
        val token = d.getCreatures(p).single { it != id }
        d.state.projectedState.getPower(token) shouldBe 1
        d.state.projectedState.getToughness(token) shouldBe 1
        d.state.projectedState.getColors(token) shouldContain Color.WHITE.name
        d.state.projectedState.getSubtypes(token) shouldContain "Soldier"

        d.removeSummoningSickness(id); d.removeSummoningSickness(token)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p, listOf(id), d.getOpponent(p)).error shouldBe null
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().options shouldBe listOf(token)
        d.submitCardSelection(p, listOf(token)).error shouldBe null
        d.bothPass()
        d.state.projectedState.getPower(id) shouldBe 3
    }
})
