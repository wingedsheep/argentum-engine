package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Additive Evolution (Secrets of Strixhaven #139).
 *
 * Additive Evolution ({3}{G}{G} Enchantment):
 *   When this enchantment enters, create a 0/0 green and blue Fractal creature token. Put three
 *     +1/+1 counters on it.
 *   At the beginning of combat on your turn, put a +1/+1 counter on target creature you control.
 *     It gains vigilance until end of turn.
 *
 * Exercises both triggers: the ETB Fractal-with-three-counters recipe, and the begin-combat
 * counter + vigilance grant on a chosen creature you control.
 */
class AdditiveEvolutionScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Additive Evolution") {

            test("ETB creates a 0/0 Fractal token with three +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Additive Evolution")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Additive Evolution").error shouldBe null
                game.resolveStack()

                val fractal = game.findPermanent("Fractal Token")
                withClue("A Fractal token should have been created by the ETB") {
                    (fractal != null) shouldBe true
                }
                withClue("The Fractal enters as a 0/0 with three +1/+1 counters (= 3/3)") {
                    plusOneCounters(game, fractal!!) shouldBe 3
                }
            }

            test("begin-combat trigger puts a +1/+1 counter on a chosen creature and grants vigilance") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Additive Evolution")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("Grizzly Bears has no vigilance before combat") {
                    game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe false
                }

                // Advance to the begin-of-combat step; the trigger goes on the stack and pauses
                // for target selection.
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("Grizzly Bears gains a +1/+1 counter from the begin-combat trigger") {
                    plusOneCounters(game, bears) shouldBe 1
                }
                withClue("Grizzly Bears gains vigilance until end of turn") {
                    game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
                }
            }
        }
    }
}
