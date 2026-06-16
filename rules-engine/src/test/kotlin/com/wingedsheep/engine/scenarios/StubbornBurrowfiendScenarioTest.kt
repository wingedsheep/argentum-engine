package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Stubborn Burrowfiend (OTJ) and the "becomes saddled for the first time each
 * turn" trigger it exercises (CR 702.171b).
 *
 * Oracle: "Whenever this creature becomes saddled for the first time each turn, mill two cards,
 * then this creature gets +X/+X until end of turn, where X is the number of creature cards in your
 * graveyard. Saddle 2"
 *
 * Covered:
 *  - The trigger fires when the Mount becomes saddled; mill happens before X is counted, so milled
 *    creatures are included in X, and the buff is locked for the turn.
 *  - "for the first time each turn": a second saddle activation in the same turn does NOT re-fire.
 *  - A fresh turn re-arms the trigger (SaddledComponent is cleared at cleanup), so saddling again
 *    next turn fires again.
 *  - Only creature cards count toward X (a milled noncreature is excluded).
 */
class StubbornBurrowfiendScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.power(id: EntityId): Int = state.projectedState.getPower(id) ?: 0
    fun GameTestDriver.toughness(id: EntityId): Int = state.projectedState.getToughness(id) ?: 0

    fun GameTestDriver.advanceToNextPrecombatMain() {
        passPriorityUntil(Step.END)
        bothPass()
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("becoming saddled mills two and buffs by creature cards in graveyard (mill counted)") {
        val driver = newDriver()
        val p1 = driver.player1
        // Two creatures on top of library to be milled — both count toward X after the mill.
        driver.putCardOnTopOfLibrary(p1, "Grizzly Bears")
        driver.putCardOnTopOfLibrary(p1, "Grizzly Bears")

        val burrowfiend = driver.putCreatureOnBattlefield(p1, "Stubborn Burrowfiend")
        // Saddle 2 needs total power >= 2; one Grizzly Bears (power 2) suffices.
        val saddler = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(burrowfiend)

        driver.submitSuccess(SaddleMount(p1, burrowfiend, listOf(saddler)))
        driver.bothPass() // resolve saddle ability → BecameSaddledEvent → trigger goes on stack
        driver.bothPass() // resolve the trigger (mill 2, then +X/+X)

        // Two creature cards milled → X = 2 → 2/2 base becomes 4/4.
        driver.getGraveyard(p1).size shouldBe 2
        driver.power(burrowfiend) shouldBe 4
        driver.toughness(burrowfiend) shouldBe 4
    }

    test("only creature cards count toward X") {
        val driver = newDriver()
        val p1 = driver.player1
        // Top two milled: one creature, one land — only the creature counts.
        driver.putCardOnTopOfLibrary(p1, "Forest")
        driver.putCardOnTopOfLibrary(p1, "Grizzly Bears")

        val burrowfiend = driver.putCreatureOnBattlefield(p1, "Stubborn Burrowfiend")
        val saddler = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(burrowfiend)

        driver.submitSuccess(SaddleMount(p1, burrowfiend, listOf(saddler)))
        driver.bothPass()
        driver.bothPass()

        // One creature card in graveyard → X = 1 → 3/3.
        driver.power(burrowfiend) shouldBe 3
        driver.toughness(burrowfiend) shouldBe 3
    }

    test("re-saddling the same turn does not fire the trigger again") {
        val driver = newDriver()
        val p1 = driver.player1
        driver.putCardOnTopOfLibrary(p1, "Grizzly Bears")
        driver.putCardOnTopOfLibrary(p1, "Grizzly Bears")

        val burrowfiend = driver.putCreatureOnBattlefield(p1, "Stubborn Burrowfiend")
        val saddlerA = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val saddlerB = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(burrowfiend)

        // First saddle this turn → fires, mills 2, buffs to 4/4.
        driver.submitSuccess(SaddleMount(p1, burrowfiend, listOf(saddlerA)))
        driver.bothPass()
        driver.bothPass()
        driver.getGraveyard(p1).size shouldBe 2
        driver.power(burrowfiend) shouldBe 4

        // Second saddle the same turn → "first time each turn" gate blocks it: no extra mill.
        driver.submitSuccess(SaddleMount(p1, burrowfiend, listOf(saddlerB)))
        driver.bothPass()
        driver.bothPass()
        driver.getGraveyard(p1).size shouldBe 2 // unchanged — trigger did not re-fire
        driver.power(burrowfiend) shouldBe 4
    }

    test("a new turn re-arms the trigger") {
        val driver = newDriver()
        val p1 = driver.player1
        driver.putCardOnTopOfLibrary(p1, "Grizzly Bears")
        driver.putCardOnTopOfLibrary(p1, "Grizzly Bears")

        val burrowfiend = driver.putCreatureOnBattlefield(p1, "Stubborn Burrowfiend")
        val saddlerA = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        driver.removeSummoningSickness(burrowfiend)

        driver.submitSuccess(SaddleMount(p1, burrowfiend, listOf(saddlerA)))
        driver.bothPass()
        driver.bothPass()
        driver.getGraveyard(p1).size shouldBe 2

        // Advance to player1's next turn (skips player2's turn). SaddledComponent cleared at cleanup.
        driver.advanceToNextPrecombatMain() // -> player2 main
        driver.advanceToNextPrecombatMain() // -> player1 main again
        driver.player1 shouldBe driver.activePlayer

        // Provide two more creatures to mill this turn.
        driver.putCardOnTopOfLibrary(p1, "Grizzly Bears")
        driver.putCardOnTopOfLibrary(p1, "Grizzly Bears")
        val saddlerB = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")

        driver.submitSuccess(SaddleMount(p1, burrowfiend, listOf(saddlerB)))
        driver.bothPass()
        driver.bothPass()
        // Fired again: two more creatures milled (4 total in graveyard).
        driver.getGraveyard(p1).size shouldBe 4
    }
})
