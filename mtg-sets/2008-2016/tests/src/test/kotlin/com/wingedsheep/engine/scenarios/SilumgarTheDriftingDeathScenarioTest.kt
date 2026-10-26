package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.SilumgarTheDriftingDeath
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Silumgar, the Drifting Death (FRF #157) — "Whenever a Dragon you control attacks, creatures
 * defending player controls get -1/-1 until end of turn."
 *
 * Three players, the card's own multiplayer ruling: each attacking Dragon shrinks the creatures of
 * the player *it* attacks (CR 508.5) — Silumgar attacking one opponent and two other Dragons the
 * other gives -1/-1 and -2/-2. A non-Dragon attacker triggers nothing.
 */
class SilumgarTheDriftingDeathScenarioTest : FunSpec({

    val dragon = card("Test Whelp Dragon") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Dragon"
        power = 2
        toughness = 2
    }

    val wall = card("Test Sturdy Wall") {
        manaCost = "{3}"
        typeLine = "Creature — Wall"
        power = 3
        toughness = 7
    }

    test("each Dragon shrinks the creatures of the player it attacks; non-Dragons don't trigger") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + SilumgarTheDriftingDeath + dragon + wall)
        val (p1, p2, p3) = d.initMultiplayer(List(3) { Deck.of("Island" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe p1

        fun ready(name: String) = d.putCreatureOnBattlefield(p1, name).also { d.removeSummoningSickness(it) }
        val silumgar = ready("Silumgar, the Drifting Death")
        val dragonA = ready("Test Whelp Dragon")
        val dragonB = ready("Test Whelp Dragon")
        val bears = ready("Grizzly Bears")
        val p2Wall = d.putCreatureOnBattlefield(p2, "Test Sturdy Wall")
        val p3Wall = d.putCreatureOnBattlefield(p3, "Test Sturdy Wall")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(silumgar to p2, dragonA to p3, dragonB to p3, bears to p3)).error shouldBe null
        withClue("one trigger per attacking Dragon, none for the Bears") { d.stackSize shouldBe 3 }
        var guard = 0
        while (d.stackSize > 0 && guard++ < 10) d.bothPass()

        withClue("only Silumgar attacks p2") { d.state.projectedState.getPower(p2Wall) shouldBe 2 }
        withClue("two Dragons attack p3") {
            d.state.projectedState.getPower(p3Wall) shouldBe 1
            d.state.projectedState.getToughness(p3Wall) shouldBe 5
        }
        withClue("your own creatures are untouched") { d.state.projectedState.getPower(bears) shouldBe 2 }
    }
})
