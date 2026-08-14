package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.cards.AngelsTomb
import com.wingedsheep.mtg.sets.definitions.emn.cards.GrappleWithThePast
import com.wingedsheep.mtg.sets.definitions.mid.cards.EccentricFarmer
import com.wingedsheep.mtg.sets.definitions.mid.cards.SiegeZombie
import com.wingedsheep.mtg.sets.definitions.zen.cards.BlazingTorch
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/** Scenario tests for Eccentric Farmer. */
class EccentricFarmerScenarioTest : FunSpec({

    val batch = listOf(GrappleWithThePast, EccentricFarmer, SiegeZombie, AngelsTomb, BlazingTorch)
    val projector = StateProjector()

    fun setup(deck: Deck = Deck.of("Forest" to 40)): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + batch)
        initMirrorMatch(deck = deck, startingLife = 20, skipMulligans = true)
    }

    // ── Eccentric Farmer ─────────────────────────────────────────────────────

    test("Eccentric Farmer: ETB mills three and offers only land cards from the graveyard") {
        val d = setup()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val creatureOnTop = d.putCardOnTopOfLibrary(you, "Grizzly Bears")
        val landOnTop = d.putCardOnTopOfLibrary(you, "Forest")
        val otherCreature = d.putCardOnTopOfLibrary(you, "Hill Giant")

        d.giveMana(you, Color.GREEN, 3)
        val farmer = d.putCardInHand(you, "Eccentric Farmer")
        d.castSpell(you, farmer).error shouldBe null
        d.bothPass() // resolve the creature spell
        while (d.pendingDecision == null && d.stackSize > 0) d.bothPass()

        val decision = d.pendingDecision as SelectCardsDecision
        decision.options shouldContain landOnTop
        decision.options.contains(creatureOnTop) shouldBe false
        decision.options.contains(otherCreature) shouldBe false

        d.submitCardSelection(you, listOf(landOnTop)).error shouldBe null
        while (d.pendingDecision == null && d.stackSize > 0) d.bothPass()

        d.getHand(you) shouldContain landOnTop
        d.getGraveyard(you) shouldContain creatureOnTop
        d.getGraveyard(you) shouldContain otherCreature
    }
})
