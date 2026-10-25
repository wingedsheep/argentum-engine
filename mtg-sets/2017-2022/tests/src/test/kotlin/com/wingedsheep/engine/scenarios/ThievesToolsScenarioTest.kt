package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Thieves' Tools (AFR #122) — "Equipped creature can't be blocked as long as its power is 3 or less."
 *
 * The first card to scope `CantBeBlockedWhilePropertyAtMost` to the *attached* creature rather
 * than to itself or a lord's group. The gate must read the equipped creature's projected power
 * and be re-asked on every projection, and it must not leak onto creatures the Tools isn't on.
 */
class ThievesToolsScenarioTest : ScenarioTestBase() {

    private fun TestGame.setCounters(id: EntityId, counters: Map<CounterType, Int>) {
        state = state.updateEntity(id) { it.with(CountersComponent(counters)) }
    }

    init {
        context("Thieves' Tools") {

            test("equipped creature with power 3 or less can't be blocked; others are unaffected") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Thieves' Tools", "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("a 3/3 is at the 'power 3 or less' threshold") {
                    game.state.projectedState.hasKeyword(giant, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
                }
                withClue("an unequipped creature gets nothing") {
                    game.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
                }
            }

            test("growing past 3 power turns the evasion off, and shrinking brings it back") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardAttachedTo(1, "Thieves' Tools", "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.setCounters(giant, mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1))
                withClue("a 4/4 equipped creature is past the gate") {
                    game.state.projectedState.getPower(giant) shouldBe 4
                    game.state.projectedState.hasKeyword(giant, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
                }

                game.setCounters(giant, emptyMap())
                withClue("back to 3 power, the evasion returns") {
                    game.state.projectedState.hasKeyword(giant, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
                }
            }

            test("a large creature carrying the Tools can be blocked") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardAttachedTo(1, "Thieves' Tools", "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm")!!
                game.state.projectedState.hasKeyword(wurm, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
            }
        }
    }
}
