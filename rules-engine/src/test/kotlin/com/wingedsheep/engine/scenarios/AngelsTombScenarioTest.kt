package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.cards.AngelsTomb
import com.wingedsheep.mtg.sets.definitions.emn.cards.GrappleWithThePast
import com.wingedsheep.mtg.sets.definitions.mid.cards.EccentricFarmer
import com.wingedsheep.mtg.sets.definitions.mid.cards.SiegeZombie
import com.wingedsheep.mtg.sets.definitions.zen.cards.BlazingTorch
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Scenario tests for Angel's Tomb. */
class AngelsTombScenarioTest : FunSpec({

    val batch = listOf(GrappleWithThePast, EccentricFarmer, SiegeZombie, AngelsTomb, BlazingTorch)
    val projector = StateProjector()

    fun setup(deck: Deck = Deck.of("Forest" to 40)): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + batch)
        initMirrorMatch(deck = deck, startingLife = 20, skipMulligans = true)
    }

    // ── Angel's Tomb ─────────────────────────────────────────────────────────

    test("Angel's Tomb: accepting the trigger animates it into a 3/3 flying Angel") {
        val d = setup(Deck.of("Plains" to 40))
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val tomb = d.putPermanentOnBattlefield(you, "Angel's Tomb")
        projector.project(d.state).isCreature(tomb) shouldBe false

        // Casting a creature triggers the Tomb.
        d.giveMana(you, Color.GREEN, 2)
        val bears = d.putCardInHand(you, "Grizzly Bears")
        d.castSpell(you, bears).error shouldBe null
        d.bothPass() // resolve the creature; the Tomb trigger goes on the stack
        while (d.pendingDecision == null && d.stackSize > 0) d.bothPass()

        (d.pendingDecision is YesNoDecision) shouldBe true
        d.submitYesNo(you, true).error shouldBe null
        while (d.pendingDecision == null && d.stackSize > 0) d.bothPass()

        val projected = projector.project(d.state)
        projected.isCreature(tomb) shouldBe true
        projected.getPower(tomb) shouldBe 3
        projected.getToughness(tomb) shouldBe 3
        projected.hasKeyword(tomb, Keyword.FLYING) shouldBe true
        // Still an artifact — the animation *adds* the creature type.
        projected.hasType(tomb, "ARTIFACT") shouldBe true
    }

    test("Angel's Tomb: declining the trigger leaves it a plain artifact") {
        val d = setup(Deck.of("Plains" to 40))
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val tomb = d.putPermanentOnBattlefield(you, "Angel's Tomb")

        d.giveMana(you, Color.GREEN, 2)
        val bears = d.putCardInHand(you, "Grizzly Bears")
        d.castSpell(you, bears).error shouldBe null
        d.bothPass()
        while (d.pendingDecision == null && d.stackSize > 0) d.bothPass()

        (d.pendingDecision is YesNoDecision) shouldBe true
        d.submitYesNo(you, false).error shouldBe null
        while (d.pendingDecision == null && d.stackSize > 0) d.bothPass()

        projector.project(d.state).isCreature(tomb) shouldBe false
    }
})
