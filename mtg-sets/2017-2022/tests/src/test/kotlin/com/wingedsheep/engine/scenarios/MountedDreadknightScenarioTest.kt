package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Mounted Dreadknight (MID #150) — {4}{R} 5/4 trample that "enters with a +1/+1 counter on it if
 * an opponent lost life this turn." Proves the conditional enters-with-counters replacement.
 */
class MountedDreadknightScenarioTest : ScenarioTestBase() {

    init {
        context("Mounted Dreadknight") {

            fun counters(game: TestGame): Int {
                val knight = game.findPermanent("Mounted Dreadknight")!!
                return game.state.getEntity(knight)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
            }

            test("enters with a +1/+1 counter when an opponent already lost life this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Mounted Dreadknight")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                game.castSpell(1, "Mounted Dreadknight").error shouldBe null
                game.resolveStack()

                counters(game) shouldBe 1
                val knight = game.findPermanent("Mounted Dreadknight")!!
                game.state.projectedState.getPower(knight) shouldBe 6
            }

            test("enters without a counter when no opponent lost life this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Mounted Dreadknight")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Mounted Dreadknight").error shouldBe null
                game.resolveStack()

                counters(game) shouldBe 0
            }
        }
    }
}
