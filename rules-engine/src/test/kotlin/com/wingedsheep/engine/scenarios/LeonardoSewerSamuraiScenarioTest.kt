package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Leonardo, Sewer Samurai (TMT) — casting a small creature from your graveyard
 * via Leonardo's permission makes it enter with a finality counter.
 *
 * Exercises the non-self `EntersWithCounters(FINALITY, condition = WasCastFromGraveyard)`:
 * Leonardo's `selfOnly = false` replacement must evaluate the cast-from-graveyard status of the
 * *entering* creature (the fix in EntersWithCountersHelper.applyGlobalEntersWithCounters).
 */
class LeonardoSewerSamuraiScenarioTest : ScenarioTestBase() {

    init {
        context("Leonardo, Sewer Samurai") {

            test("a creature cast from the graveyard via Leonardo enters with a finality counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Leonardo, Sewer Samurai")
                    .withCardInGraveyard(1, "Mons's Goblin Raiders") // {R} 1/1 — power/toughness <= 1
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpellFromGraveyard(1, "Mons's Goblin Raiders")
                withClue("Casting from the graveyard via Leonardo should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                val raider = game.findPermanent("Mons's Goblin Raiders")!!
                val counters = game.state.getEntity(raider)?.get<CountersComponent>()
                withClue("Cast from graveyard via Leonardo → enters with one finality counter") {
                    (counters?.getCount(CounterType.FINALITY) ?: 0) shouldBe 1
                }
            }
        }
    }
}
