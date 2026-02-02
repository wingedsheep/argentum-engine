package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Elvish Vanguard's triggered ability.
 *
 * Card reference:
 * - Elvish Vanguard (1G): 1/1 Creature - Elf Warrior
 *   "Whenever another Elf enters the battlefield, put a +1/+1 counter on Elvish Vanguard."
 */
class ElvishVanguardScenarioTest : ScenarioTestBase() {

    /**
     * Helper to get the number of +1/+1 counters on a permanent.
     */
    private fun ScenarioTestBase.TestGame.getCounters(entityId: com.wingedsheep.sdk.model.EntityId): Int {
        return state.getEntity(entityId)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        context("Elvish Vanguard tribal counter trigger") {
            test("gets +1/+1 counter when another Elf enters under your control") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Elvish Vanguard")
                    .withCardInHand(1, "Elvish Warrior")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vanguard = game.findPermanent("Elvish Vanguard")!!

                // Verify initial state - no counters
                withClue("Elvish Vanguard should start with 0 counters") {
                    game.getCounters(vanguard) shouldBe 0
                }

                // Cast another Elf
                val castResult = game.castSpell(1, "Elvish Warrior")
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve creature spell then the trigger
                game.resolveStack()
                game.resolveStack()

                // Elvish Vanguard should have 1 counter
                withClue("Elvish Vanguard should have 1 +1/+1 counter after Elf entered") {
                    game.getCounters(vanguard) shouldBe 1
                }
            }

            test("gets counter when opponent's Elf enters") {
                val game = scenario()
                    .withPlayers("Elf Player", "Elf Opponent")
                    .withCardOnBattlefield(1, "Elvish Vanguard")
                    .withCardInHand(2, "Wirewood Elf")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vanguard = game.findPermanent("Elvish Vanguard")!!

                // Opponent casts an Elf
                game.castSpell(2, "Wirewood Elf")
                game.resolveStack()
                game.resolveStack()

                // Elvish Vanguard should get a counter
                withClue("Elvish Vanguard should get counter from opponent's Elf") {
                    game.getCounters(vanguard) shouldBe 1
                }
            }

            test("does not trigger for non-Elf creatures") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Elvish Vanguard")
                    .withCardInHand(1, "Grizzly Bears") // Not an Elf
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vanguard = game.findPermanent("Elvish Vanguard")!!

                // Cast a non-Elf
                game.castSpell(1, "Grizzly Bears")
                game.resolveStack()

                // Elvish Vanguard should still have no counters
                withClue("Elvish Vanguard should not trigger for non-Elf") {
                    game.getCounters(vanguard) shouldBe 0
                }
            }

            test("does not trigger for itself entering") {
                // The ability says "another Elf", so it shouldn't trigger when Elvish Vanguard itself enters
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardInHand(1, "Elvish Vanguard")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Elvish Vanguard
                game.castSpell(1, "Elvish Vanguard")
                game.resolveStack()

                val vanguard = game.findPermanent("Elvish Vanguard")!!

                // Should have no counters - it doesn't trigger for itself
                withClue("Elvish Vanguard should not trigger for itself entering") {
                    game.getCounters(vanguard) shouldBe 0
                }
            }

            test("accumulates counters from multiple Elves entering") {
                val game = scenario()
                    .withPlayers("Elf Player", "Opponent")
                    .withCardOnBattlefield(1, "Elvish Vanguard")
                    .withCardInHand(1, "Elvish Warrior")
                    .withCardInHand(1, "Wirewood Elf")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val vanguard = game.findPermanent("Elvish Vanguard")!!

                // Cast first Elf
                game.castSpell(1, "Elvish Warrior")
                game.resolveStack()
                game.resolveStack()

                withClue("Elvish Vanguard should have 1 counter after first Elf") {
                    game.getCounters(vanguard) shouldBe 1
                }

                // Cast second Elf
                game.castSpell(1, "Wirewood Elf")
                game.resolveStack()
                game.resolveStack()

                withClue("Elvish Vanguard should have 2 counters after second Elf") {
                    game.getCounters(vanguard) shouldBe 2
                }
            }
        }
    }
}
