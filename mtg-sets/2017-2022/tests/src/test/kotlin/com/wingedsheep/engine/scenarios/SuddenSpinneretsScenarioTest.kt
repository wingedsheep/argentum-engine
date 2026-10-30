package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sudden Spinnerets (IKO #171) — {G} Instant.
 *
 *   "Target creature gets +1/+3 until end of turn. Put a reach counter on it. Untap it."
 */
class SuddenSpinneretsScenarioTest : ScenarioTestBase() {

    init {
        test("a tapped creature gets +1/+3, a reach counter, and untaps") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Sudden Spinnerets")
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            withClue("Grizzly Bears starts tapped and without reach") {
                game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                game.state.projectedState.hasKeyword(bears, Keyword.REACH) shouldBe false
            }

            game.castSpell(1, "Sudden Spinnerets", bears).error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears is a 3/5 until end of turn") {
                game.state.projectedState.getPower(bears) shouldBe 3
                game.state.projectedState.getToughness(bears) shouldBe 5
            }
            withClue("Grizzly Bears carries a reach counter, which grants reach") {
                val counters = game.state.getEntity(bears)?.get<CountersComponent>()
                (counters?.counters?.get(CounterType.REACH) ?: 0) shouldBe 1
                game.state.projectedState.hasKeyword(bears, Keyword.REACH) shouldBe true
            }
            withClue("Grizzly Bears is untapped") {
                game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
            }
        }
    }
}
