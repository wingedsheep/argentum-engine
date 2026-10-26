package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.SilumgarTheDriftingDeath
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Silumgar, the Drifting Death (FRF #157) — "Whenever a Dragon you control attacks, creatures
 * defending player controls get -1/-1 until end of turn."
 *
 * Three players: Silumgar attacks one opponent and nothing else attacks, so only that opponent's
 * creatures shrink; a non-Dragon attacker triggers nothing.
 */
class SilumgarTheDriftingDeathScenarioTest : FunSpec({

    test("only the creatures of the player Silumgar attacks get -1/-1; non-Dragons don't trigger") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + SilumgarTheDriftingDeath)
        val (p1, p2, p3) = d.initMultiplayer(List(3) { Deck.of("Island" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe p1

        val silumgar = d.putCreatureOnBattlefield(p1, "Silumgar, the Drifting Death")
            .also { d.removeSummoningSickness(it) }
        val bears = d.putCreatureOnBattlefield(p1, "Grizzly Bears").also { d.removeSummoningSickness(it) }
        val p2Bears = d.putCreatureOnBattlefield(p2, "Grizzly Bears")
        val p3Bears = d.putCreatureOnBattlefield(p3, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(silumgar to p2, bears to p3)).error shouldBe null
        withClue("one trigger: only the Dragon attacking") { d.stackSize shouldBe 1 }
        var guard = 0
        while (d.stackSize > 0 && guard++ < 10) d.bothPass()

        d.state.projectedState.getPower(p2Bears) shouldBe 1
        d.state.projectedState.getToughness(p2Bears) shouldBe 1
        withClue("p3 is attacked only by a non-Dragon") { d.state.projectedState.getPower(p3Bears) shouldBe 2 }
        withClue("your own creatures are untouched") { d.state.projectedState.getPower(bears) shouldBe 2 }
    }
})
