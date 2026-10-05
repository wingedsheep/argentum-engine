package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.SedgeTroll
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SedgeTrollScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    test("only its controller's Swamp grants the bonus and multiple Swamps do not stack") {
        val d = driver(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        val troll = d.putCreatureOnBattlefield(me, "Sedge Troll")
        d.putPermanentOnBattlefield(other, "Swamp")
        d.state.projectedState.getPower(troll) shouldBe 2
        repeat(2) { d.putPermanentOnBattlefield(me, "Swamp") }
        d.state.projectedState.getPower(troll) shouldBe 3
        d.state.projectedState.getToughness(troll) shouldBe 3
    }
    test("regeneration works without controlling a Swamp and saves it from lethal damage") {
        val d = driver(); val me = d.activePlayer!!
        val troll = d.putCreatureOnBattlefield(me, "Sedge Troll")
        d.giveMana(me, Color.BLACK)
        d.submit(ActivateAbility(me, troll, SedgeTroll.activatedAbilities.first().id)).error shouldBe null
        d.bothPass()
        d.giveMana(me, Color.RED)
        d.castSpell(me, d.putCardInHand(me, "Lightning Bolt"), listOf(troll)).error shouldBe null
        d.bothPass()
        d.state.getBattlefield().contains(troll) shouldBe true
        d.isTapped(troll) shouldBe true
        d.state.projectedState.getToughness(troll) shouldBe 2
    }
})
