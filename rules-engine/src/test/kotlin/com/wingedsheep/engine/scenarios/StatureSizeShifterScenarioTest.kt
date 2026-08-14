package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Stature, Size Shifter — "Stature can't be blocked if her power is 1 or less."
 *
 * The evasion is *conditional on her own current power*, so it has to switch off the moment she
 * grows — including from her own power-up, which is the tension the card is built around. That
 * makes this a projection-layer claim, not a resolution-time one: `CantBeBlockedWhilePropertyAtMost`
 * is re-asked in a post-layer pass every time state is projected, and it must read her
 * **projected** power (counters, lords, pumps included), not her printed 1. A
 * `ConditionalStaticAbility` over a power comparison would read the printed value and latch on
 * forever, which is exactly what these tests are here to catch.
 */
class StatureSizeShifterScenarioTest : ScenarioTestBase() {

    /** Set a permanent's counters outright — the builder has no counters option. */
    private fun TestGame.setCounters(id: EntityId, counters: Map<CounterType, Int>) {
        state = state.updateEntity(id) { it.with(CountersComponent(counters)) }
    }

    init {
        context("Stature, Size Shifter") {

            test("with printed power 1 she can't be blocked") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Stature, Size Shifter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val stature = game.findPermanent("Stature, Size Shifter")!!
                game.state.projectedState.getPower(stature) shouldBe 1
                game.state.projectedState.hasKeyword(stature, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
            }

            test("a +1/+1 counter takes her past 1 power and she loses the evasion") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Stature, Size Shifter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val stature = game.findPermanent("Stature, Size Shifter")!!
                game.setCounters(stature, mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1))

                withClue("a 2/2 Stature is past the 'power 1 or less' gate") {
                    game.state.projectedState.getPower(stature) shouldBe 2
                }
                withClue("the evasion must re-evaluate against projected power") {
                    game.state.projectedState.hasKeyword(stature, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
                }
            }

            test("shrinking back to 1 power brings the evasion back") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Stature, Size Shifter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val stature = game.findPermanent("Stature, Size Shifter")!!
                game.setCounters(stature, mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2))
                game.state.projectedState.hasKeyword(stature, AbilityFlag.CANT_BE_BLOCKED) shouldBe false

                game.setCounters(stature, emptyMap())
                withClue("back to base 1/1 with no counters") {
                    game.state.projectedState.getPower(stature) shouldBe 1
                }
                withClue("the post-layer pass is re-asked, not latched: the evasion returns") {
                    game.state.projectedState.hasKeyword(stature, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
                }
            }
        }
    }
}
