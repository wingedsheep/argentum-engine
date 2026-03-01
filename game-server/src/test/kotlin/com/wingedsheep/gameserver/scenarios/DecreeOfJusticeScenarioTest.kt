package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Decree of Justice.
 *
 * Card reference:
 * - Decree of Justice ({X}{X}{2}{W}{W}): Sorcery
 *   "Create X 4/4 white Angel creature tokens with flying."
 *   Cycling {2}{W}
 *   "When you cycle Decree of Justice, you may pay {X}. If you do,
 *    create X 1/1 white Soldier creature tokens."
 *
 * Test scenarios:
 * 1. Casting with X=2 creates 2 Angel tokens (needs 6WW = 8 mana)
 * 2. Cycling and paying X=3 creates 3 Soldier tokens
 * 3. Cycling and paying X=0 creates no tokens
 */
class DecreeOfJusticeScenarioTest : ScenarioTestBase() {

    init {
        context("Decree of Justice main spell") {
            test("casting with X=2 creates 2 Angel tokens") {
                // Setup: Player 1 has Decree of Justice and 8 Plains (enough for {X}{X}{2}{W}{W} with X=2)
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Decree of Justice")
                    .withLandsOnBattlefield(1, "Plains", 8)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast with X=2 (total cost = 2+2+2+WW = 8)
                val castResult = game.castXSpell(1, "Decree of Justice", xValue = 2)
                withClue("Spell should cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Should create 2 Angel tokens
                val angels = game.findAllPermanents("Angel Token")
                withClue("Should have 2 Angel tokens on the battlefield") {
                    angels.size shouldBe 2
                }
            }

            test("casting with X=0 creates no tokens") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Decree of Justice")
                    .withLandsOnBattlefield(1, "Plains", 4) // Minimum: {0}{0}{2}{W}{W} = 4
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castXSpell(1, "Decree of Justice", xValue = 0)
                withClue("Spell should cast successfully") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                val angels = game.findAllPermanents("Angel Token")
                withClue("Should have no Angel tokens") {
                    angels.size shouldBe 0
                }
            }
        }

        context("Decree of Justice cycling trigger") {
            test("cycling and paying X=3 creates 3 Soldier tokens") {
                // Setup: Player 1 has Decree of Justice, 3 Plains for cycling + 3 more for X=3
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Decree of Justice")
                    .withLandsOnBattlefield(1, "Plains", 6) // {2}{W} cycling + {3} for X
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cycle the card - triggers "you may pay {X}"
                val cycleResult = game.cycleCard(1, "Decree of Justice")
                withClue("Cycling should succeed") {
                    cycleResult.error shouldBe null
                }

                // Resolve the cycling trigger (it goes on the stack)
                // This presents the ChooseNumber decision for X value
                game.resolveStack()

                // Choose X=3 (3 remaining untapped lands after cycling cost)
                game.chooseNumber(3)

                // Resolve any remaining stack items
                game.resolveStack()

                // Should create 3 Soldier tokens
                val soldiers = game.findAllPermanents("Soldier Token")
                withClue("Should have 3 Soldier tokens on the battlefield") {
                    soldiers.size shouldBe 3
                }
            }

            test("cycling and paying X=0 creates no tokens") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Decree of Justice")
                    .withLandsOnBattlefield(1, "Plains", 3) // Just enough for cycling cost
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycleResult = game.cycleCard(1, "Decree of Justice")
                withClue("Cycling should succeed") {
                    cycleResult.error shouldBe null
                }

                // Resolve the cycling trigger (it goes on the stack)
                // With only 3 Plains (all tapped for cycling cost), maxAffordable=0
                // so the executor skips the decision and creates no tokens
                game.resolveStack()

                // Should not create any Soldier tokens
                val soldiers = game.findAllPermanents("Soldier Token")
                withClue("Should have no Soldier tokens") {
                    soldiers.size shouldBe 0
                }
            }
        }
    }
}
