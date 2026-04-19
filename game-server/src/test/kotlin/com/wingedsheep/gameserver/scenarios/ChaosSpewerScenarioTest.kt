package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Chaos Spewer.
 *
 * Card reference:
 * - Chaos Spewer ({2}{B/R}): 5/4 Goblin Warlock
 *   "When this creature enters, you may pay {2}. If you don't, blight 2.
 *    (To blight 2, put two -1/-1 counters on a creature you control.)"
 */
class ChaosSpewerScenarioTest : ScenarioTestBase() {

    init {
        context("Chaos Spewer ETB trigger") {

            test("declining the mana payment puts 2 -1/-1 counters on a chosen creature") {
                // Elvish Warrior (2/3) survives blight 2 (becomes 0/1) so we can read its counters.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Chaos Spewer")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(1, "Mountain", 2) // 5 lands: 3 to cast, 2 spare for the choice
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val warrior = game.findPermanent("Elvish Warrior")!!

                val castResult = game.castSpell(1, "Chaos Spewer")
                withClue("Chaos Spewer should be cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("ETB trigger should pause for the pay-or-blight decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Decline to pay {2} — forces blight 2
                game.answerYesNo(false)

                withClue("Declining the mana payment should prompt for a blight target") {
                    game.hasPendingDecision() shouldBe true
                }

                // Choose Elvish Warrior to receive the -1/-1 counters
                val blightResult = game.selectCards(listOf(warrior))
                withClue("Selecting the blight target should succeed: ${blightResult.error}") {
                    blightResult.error shouldBe null
                }

                val counters = game.state.getEntity(warrior)?.get<CountersComponent>()
                withClue("Elvish Warrior should have 2 -1/-1 counters from blight 2") {
                    counters.shouldNotBeNull()
                    counters.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 2
                }

                withClue("Elvish Warrior survives blight 2 (2/3 → 0/1)") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe true
                }

                withClue("Chaos Spewer should be on the battlefield") {
                    game.isOnBattlefield("Chaos Spewer") shouldBe true
                }
            }

            test("paying {2} skips blight entirely") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Chaos Spewer")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val warrior = game.findPermanent("Elvish Warrior")!!

                game.castSpell(1, "Chaos Spewer")
                game.resolveStack()

                withClue("ETB trigger should pause for the pay-or-blight decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Pay {2} to avoid blight
                game.answerYesNo(true)

                withClue("Paying the mana should leave no blight decision pending") {
                    game.hasPendingDecision() shouldBe false
                }

                val counters = game.state.getEntity(warrior)?.get<CountersComponent>()
                withClue("Elvish Warrior should have no -1/-1 counters") {
                    (counters?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0) shouldBe 0
                }

                withClue("Chaos Spewer should be on the battlefield") {
                    game.isOnBattlefield("Chaos Spewer") shouldBe true
                }
            }

            test("auto-blights the only eligible creature when the player can't afford {2}") {
                // Only 3 lands — enough to cast {2}{B/R} but nothing left to pay the post-ETB {2}.
                // With only Chaos Spewer on the battlefield, blight 2 auto-selects it and
                // the whole trigger resolves without any player input.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Chaos Spewer")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Chaos Spewer")
                game.resolveStack()

                withClue("Trigger resolves without prompting — can't pay and only one blight target") {
                    game.hasPendingDecision() shouldBe false
                }

                val spewer = game.findPermanent("Chaos Spewer")!!
                val counters = game.state.getEntity(spewer)?.get<CountersComponent>()
                withClue("Chaos Spewer should have 2 -1/-1 counters and remain on the battlefield as a 3/2") {
                    counters.shouldNotBeNull()
                    counters.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 2
                }
            }
        }
    }
}
