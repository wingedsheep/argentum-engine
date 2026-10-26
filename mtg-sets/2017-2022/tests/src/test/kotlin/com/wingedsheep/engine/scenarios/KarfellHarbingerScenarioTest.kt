package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ForetellCard
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Karfell Harbinger (KHM #65) — "{T}: Add {U}. Spend this mana only to foretell a card from your
 * hand or cast an instant or sorcery spell."
 *
 * The auto-payer taps it for the foretell special action's {2} (CR 702.143a) and for an instant,
 * but not for a creature spell.
 */
class KarfellHarbingerScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.harbinger(): com.wingedsheep.sdk.model.EntityId =
        putCreatureOnBattlefield(activePlayer!!, "Karfell Harbinger").also { removeSummoningSickness(it) }

    test("its mana pays to foretell a card from hand") {
        val d = driver()
        val p = d.activePlayer!!
        val harbinger = d.harbinger()
        val cup = d.putCardInHand(p, "Poison the Cup")
        d.giveColorlessMana(p, 1)
        d.legalActions(p).single { it.action == ForetellCard(p, cup) }.affordable shouldBe true
        d.submitSuccess(ForetellCard(p, cup))
        d.getExile(p).contains(cup) shouldBe true
        d.state.getEntity(harbinger)!!.has<TappedComponent>() shouldBe true
    }

    test("its mana pays for an instant spell") {
        val d = driver()
        val p = d.activePlayer!!
        val harbinger = d.harbinger()
        val bears = d.putCreatureOnBattlefield(d.getOpponent(p), "Grizzly Bears")
        val cup = d.putCardInHand(p, "Poison the Cup")
        // No floating mana, so the cast auto-pays: two Swamps for {B}{B}, the Harbinger for {1}.
        repeat(2) { d.putPermanentOnBattlefield(p, "Swamp") }
        d.castSpell(p, cup, listOf(bears)).error shouldBe null
        d.state.getEntity(harbinger)!!.has<TappedComponent>() shouldBe true
        d.bothPass()
        d.getCreatures(d.getOpponent(p)).contains(bears) shouldBe false
    }

    test("its mana can't pay for a creature spell") {
        val d = driver()
        val p = d.activePlayer!!
        d.harbinger()
        val bears = d.putCardInHand(p, "Grizzly Bears")
        // A Forest and the Harbinger would pay {1}{G}, but the Harbinger's mana can't go on a creature.
        d.putPermanentOnBattlefield(p, "Forest")
        (d.castSpell(p, bears).error != null) shouldBe true
    }
})
