package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Menacing Ogre.
 *
 * Card reference:
 * - Menacing Ogre ({3}{R}{R}): Creature — Ogre 3/3
 *   Trample, haste
 *   When Menacing Ogre enters, each player secretly chooses a number. Then those
 *   numbers are revealed. Each player with the highest number loses that much life.
 *   If you are one of those players, put two +1/+1 counters on Menacing Ogre.
 */
class MenacingOgreScenarioTest : ScenarioTestBase() {

    init {
        context("Menacing Ogre enters the battlefield trigger") {
            test("controller bids highest - loses life and gets counters") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Menacing Ogre")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Menacing Ogre
                val castResult = game.castSpell(1, "Menacing Ogre")
                withClue("Menacing Ogre should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve the creature spell (enters battlefield, triggers ETB)
                game.resolveStack()

                // Player 1 (active player) should be prompted first
                withClue("There should be a pending decision for player 1") {
                    game.hasPendingDecision() shouldBe true
                    game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
                }

                // Player 1 chooses 5
                game.chooseNumber(5)

                // Player 2 should be prompted next
                withClue("There should be a pending decision for player 2") {
                    game.hasPendingDecision() shouldBe true
                    game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
                }

                // Player 2 chooses 3
                game.chooseNumber(3)

                // Player 1 had the highest bid (5), so loses 5 life and gets counters
                withClue("Player 1 should have lost 5 life (20 - 5 = 15)") {
                    game.getLifeTotal(1) shouldBe 15
                }

                withClue("Player 2 should still have 20 life (not highest bidder)") {
                    game.getLifeTotal(2) shouldBe 20
                }

                // Menacing Ogre should have two +1/+1 counters
                val ogreId = game.findPermanent("Menacing Ogre")
                withClue("Menacing Ogre should be on the battlefield") {
                    ogreId shouldNotBe null
                }

                val counters = game.state.getEntity(ogreId!!)?.get<CountersComponent>()
                withClue("Menacing Ogre should have 2 +1/+1 counters") {
                    counters shouldNotBe null
                    counters!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                }
            }

            test("opponent bids highest - opponent loses life, no counters on ogre") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Menacing Ogre")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Menacing Ogre")
                game.resolveStack()

                // Player 1 chooses 2
                game.chooseNumber(2)

                // Player 2 chooses 7
                game.chooseNumber(7)

                // Player 2 had the highest bid (7), so loses 7 life
                withClue("Player 1 should still have 20 life (not highest bidder)") {
                    game.getLifeTotal(1) shouldBe 20
                }

                withClue("Player 2 should have lost 7 life (20 - 7 = 13)") {
                    game.getLifeTotal(2) shouldBe 13
                }

                // Menacing Ogre should NOT have counters (controller was not highest bidder)
                val ogreId = game.findPermanent("Menacing Ogre")
                withClue("Menacing Ogre should be on the battlefield") {
                    ogreId shouldNotBe null
                }

                val counters = game.state.getEntity(ogreId!!)?.get<CountersComponent>()
                val plusOneCount = counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                withClue("Menacing Ogre should have no +1/+1 counters") {
                    plusOneCount shouldBe 0
                }
            }

            test("both players bid the same - both lose life, controller gets counters") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Menacing Ogre")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Menacing Ogre")
                game.resolveStack()

                // Both players choose 4
                game.chooseNumber(4)
                game.chooseNumber(4)

                // Both players have the highest bid (4), both lose 4 life
                withClue("Player 1 should have lost 4 life (20 - 4 = 16)") {
                    game.getLifeTotal(1) shouldBe 16
                }

                withClue("Player 2 should have lost 4 life (20 - 4 = 16)") {
                    game.getLifeTotal(2) shouldBe 16
                }

                // Controller (player 1) is one of the highest bidders, so gets counters
                val ogreId = game.findPermanent("Menacing Ogre")
                val counters = game.state.getEntity(ogreId!!)?.get<CountersComponent>()
                withClue("Menacing Ogre should have 2 +1/+1 counters") {
                    counters shouldNotBe null
                    counters!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                }
            }

            test("both players bid 0 - no life loss, no counters") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Menacing Ogre")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Menacing Ogre")
                game.resolveStack()

                // Both players choose 0
                game.chooseNumber(0)
                game.chooseNumber(0)

                // No one loses life (highest bid is 0, and we skip 0-value bids)
                withClue("Player 1 should still have 20 life") {
                    game.getLifeTotal(1) shouldBe 20
                }

                withClue("Player 2 should still have 20 life") {
                    game.getLifeTotal(2) shouldBe 20
                }

                // No counters (controller's bid of 0 doesn't count)
                val ogreId = game.findPermanent("Menacing Ogre")
                val counters = game.state.getEntity(ogreId!!)?.get<CountersComponent>()
                val plusOneCount = counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                withClue("Menacing Ogre should have no +1/+1 counters") {
                    plusOneCount shouldBe 0
                }
            }
        }
    }
}
