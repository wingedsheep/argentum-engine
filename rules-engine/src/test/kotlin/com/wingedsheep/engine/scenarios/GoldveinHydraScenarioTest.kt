package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Goldvein Hydra (OTJ #167) — {X}{G} Creature — Hydra, 0/0.
 *   Vigilance, trample, haste.
 *   This creature enters with X +1/+1 counters on it.
 *   When this creature dies, create a number of tapped Treasure tokens equal to its power.
 *
 * Two things to prove end-to-end:
 *   1. Casting with X=3 makes the Hydra enter with three +1/+1 counters (a 3/3).
 *   2. When it dies, it mints a Treasure token equal to its LAST-KNOWN power (3),
 *      and every minted Treasure enters tapped.
 */
class GoldveinHydraScenarioTest : ScenarioTestBase() {

    init {
        context("Goldvein Hydra") {

            test("enters with X +1/+1 counters and dies into that many tapped Treasures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Goldvein Hydra")
                    // {X}{G} for X=3 → 4 mana, plus {R} for Lightning Bolt to kill it.
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    // Instant removal cast by the active player at their own Hydra.
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Goldvein Hydra", xValue = 3).error shouldBe null
                game.resolveStack()
                // Player 1 retains priority in their own main phase, so they cast the
                // (instant) Lightning Bolt at their own Hydra to kill it below.

                val hydra = game.findPermanent("Goldvein Hydra")!!
                val counters = game.state.getEntity(hydra)?.get<CountersComponent>()
                withClue("Goldvein Hydra entered with X = 3 +1/+1 counters") {
                    (counters?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 3
                }

                withClue("No Treasure exists before the Hydra dies") {
                    game.findAllPermanents("Treasure").size shouldBe 0
                }

                // Lightning Bolt (3 damage) kills the 3/3 Hydra.
                game.castSpell(1, "Lightning Bolt", targetId = hydra).error shouldBe null
                game.resolveStack()

                val treasures = game.findAllPermanents("Treasure")
                withClue("Dies trigger mints Treasures equal to last-known power (3)") {
                    treasures.size shouldBe 3
                }
                withClue("Every minted Treasure enters tapped") {
                    treasures.all { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
                }
            }
        }
    }
}
