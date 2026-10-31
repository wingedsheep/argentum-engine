package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Chandra's Embercat (M20 #129) — "{T}: Add {R}. Spend this mana only to cast an Elemental spell
 * or a Chandra planeswalker spell."
 *
 * The auto-payer taps it for an Elemental spell and for a Chandra planeswalker spell, but not for
 * a non-Elemental creature or a planeswalker of another subtype (Ajani).
 */
class ChandrasEmbercatScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.embercat(): EntityId =
        putCreatureOnBattlefield(activePlayer!!, "Chandra's Embercat").also { removeSummoningSickness(it) }

    test("its mana pays for an Elemental spell") {
        val d = driver()
        val p = d.activePlayer!!
        val embercat = d.embercat()
        val elemental = d.putCardInHand(p, "Fire Elemental")
        // Four Mountains and the Embercat pay {3}{R}{R}.
        repeat(4) { d.putPermanentOnBattlefield(p, "Mountain") }
        d.castSpell(p, elemental).error shouldBe null
        d.state.getEntity(embercat)!!.has<TappedComponent>() shouldBe true
    }

    test("its mana pays for a Chandra planeswalker spell") {
        val d = driver()
        val p = d.activePlayer!!
        val embercat = d.embercat()
        val chandra = d.putCardInHand(p, "Chandra, Flame's Fury")
        // Five Mountains and the Embercat pay {4}{R}{R}.
        repeat(5) { d.putPermanentOnBattlefield(p, "Mountain") }
        d.castSpell(p, chandra).error shouldBe null
        d.state.getEntity(embercat)!!.has<TappedComponent>() shouldBe true
    }

    test("its mana can't pay for a non-Elemental creature spell") {
        val d = driver()
        val p = d.activePlayer!!
        d.embercat()
        val bears = d.putCardInHand(p, "Grizzly Bears")
        // A Forest and the Embercat would pay {1}{G}, but the Embercat's mana can't go on a Bear.
        d.putPermanentOnBattlefield(p, "Forest")
        (d.castSpell(p, bears).error != null) shouldBe true
    }

    test("its mana can't pay for a non-Chandra planeswalker spell") {
        val d = driver()
        val p = d.activePlayer!!
        d.embercat()
        val ajani = d.putCardInHand(p, "Ajani, Strength of the Pride")
        // Three Plains and the Embercat would pay {2}{W}{W}, but Ajani isn't a Chandra planeswalker.
        repeat(3) { d.putPermanentOnBattlefield(p, "Plains") }
        (d.castSpell(p, ajani).error != null) shouldBe true
    }
})
