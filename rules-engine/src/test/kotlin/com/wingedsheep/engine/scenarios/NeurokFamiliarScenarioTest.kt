package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Neurok Familiar (MRD) — "When this creature enters, reveal the top card of your library. If it's
 * an artifact card, put it into your hand. Otherwise, put it into your graveyard."
 *
 * Gather → Select → Move: the top card is gathered revealed, partitioned on `Artifact`, and each
 * side moved to its zone. There is no "may" anywhere, so the trigger must resolve without pausing —
 * that, and the partition landing the card in exactly one zone, is what these tests pin.
 */
class NeurokFamiliarScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Cast the Familiar from hand for {1}{U} and let the enters trigger resolve. */
    fun GameTestDriver.castFamiliar(you: EntityId) {
        val familiar = putCardInHand(you, "Neurok Familiar")
        giveMana(you, Color.BLUE, 2)
        castSpell(you, familiar).isSuccess shouldBe true
        while (!isPaused && state.stack.isNotEmpty()) bothPass()
    }

    test("an artifact card on top goes to your hand") {
        val d = driver()
        val you = d.activePlayer!!
        val top = d.putCardOnTopOfLibrary(you, "Frogmite") // Artifact Creature — Myr

        d.castFamiliar(you)

        d.isPaused shouldBe false // nothing to choose — the partition is mechanical
        d.getHand(you) shouldContain top
        d.getGraveyard(you) shouldNotContain top
    }

    test("a nonartifact card on top goes to your graveyard") {
        val d = driver()
        val you = d.activePlayer!!
        val top = d.putCardOnTopOfLibrary(you, "Grizzly Bears") // green creature, no artifact type

        d.castFamiliar(you)

        d.isPaused shouldBe false
        d.getGraveyard(you) shouldContain top
        d.getHand(you) shouldNotContain top
    }

    test("only the single top card is looked at — the card beneath it stays in the library") {
        val d = driver()
        val you = d.activePlayer!!
        val second = d.putCardOnTopOfLibrary(you, "Frogmite")
        val top = d.putCardOnTopOfLibrary(you, "Grizzly Bears")

        d.castFamiliar(you)

        d.getGraveyard(you) shouldContain top
        d.getHand(you) shouldNotContain second
        d.getGraveyard(you) shouldNotContain second
    }
})
