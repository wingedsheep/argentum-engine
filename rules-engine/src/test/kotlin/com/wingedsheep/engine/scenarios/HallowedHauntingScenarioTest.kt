package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hallowed Haunting (VOW #17).
 *
 *   As long as you control seven or more enchantments, creatures you control have flying and
 *   vigilance.
 *   Whenever you cast an enchantment spell, create a white Spirit Cleric creature token with "This
 *   token's power and toughness are each equal to the number of Spirits you control."
 *
 * Exercises the cast-an-enchantment → Spirit Cleric token trigger, the token's self-referential
 * characteristic-defining P/T (equal to the number of Spirits you control), and the "seven or more
 * enchantments" anthem that grants flying and vigilance.
 */
class HallowedHauntingScenarioTest : ScenarioTestBase() {

    init {
        context("Hallowed Haunting") {

            test("casting an enchantment makes a Spirit Cleric whose P/T equals Spirits you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hallowed Haunting")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInHand(1, "Test Enchantment") // {1}{W} vanilla enchantment
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Enchantment")
                if (game.getPendingDecision() is com.wingedsheep.engine.core.SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                val spirits = game.findPermanents("Spirit Cleric Token")
                withClue("casting one enchantment created exactly one Spirit Cleric token") {
                    spirits.size shouldBe 1
                }
                withClue("its P/T equals Spirits you control — one Spirit (itself) → 1/1") {
                    val spirit = spirits.first()
                    game.state.projectedState.getPower(spirit) shouldBe 1
                    game.state.projectedState.getToughness(spirit) shouldBe 1
                }
            }

            test("with seven or more enchantments, creatures you control gain flying and vigilance") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hallowed Haunting")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                // Six more enchantments (plus Hallowed Haunting itself = seven) trip the anthem.
                repeat(6) { builder = builder.withCardOnBattlefield(1, "Test Enchantment") }
                val game = builder.build()

                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("seven enchantments → creatures you control have flying") {
                    game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
                }
                withClue("seven enchantments → creatures you control have vigilance") {
                    game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
                }
            }

            test("with fewer than seven enchantments, the anthem does not apply") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hallowed Haunting")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                // Hallowed Haunting + four = five enchantments, below the seven threshold.
                repeat(4) { builder = builder.withCardOnBattlefield(1, "Test Enchantment") }
                val game = builder.build()

                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("only five enchantments → no flying") {
                    game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe false
                }
                withClue("only five enchantments → no vigilance") {
                    game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe false
                }
            }
        }
    }
}
