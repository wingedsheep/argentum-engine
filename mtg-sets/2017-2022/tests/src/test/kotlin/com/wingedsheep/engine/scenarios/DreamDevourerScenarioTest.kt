package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ForetellCard
import com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Dream Devourer (KHM #90) — "Each nonland card in your hand without foretell has foretell. Its
 * foretell cost is equal to its mana cost reduced by {2}. Whenever you foretell a card, this
 * creature gets +2/+0 until end of turn."
 */
class DreamDevourerScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("a card in hand gains foretell for its mana cost minus {2}, and foretelling pumps the Devourer") {
        val d = driver()
        val p = d.activePlayer!!
        val devourer = d.putCreatureOnBattlefield(p, "Dream Devourer")
        val giant = d.putCardInHand(p, "Hill Giant") // {3}{R}
        d.giveColorlessMana(p, 2)
        d.submitSuccess(ForetellCard(p, giant))
        d.state.getEntity(giant)!!.get<PlayWithFixedAlternativeManaCostComponent>()!!.fixedCost shouldBe
            ManaCost.parse("{1}{R}")
        d.bothPass()
        d.state.projectedState.getPower(devourer) shouldBe 2
    }

    test("a card with printed foretell keeps its own cost") {
        val d = driver()
        val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, "Dream Devourer")
        val cup = d.putCardInHand(p, "Poison the Cup") // Foretell {1}{B}
        d.giveColorlessMana(p, 2)
        d.submitSuccess(ForetellCard(p, cup))
        d.state.getEntity(cup)!!.get<PlayWithFixedAlternativeManaCostComponent>()!!.fixedCost shouldBe
            ManaCost.parse("{1}{B}")
    }
})
