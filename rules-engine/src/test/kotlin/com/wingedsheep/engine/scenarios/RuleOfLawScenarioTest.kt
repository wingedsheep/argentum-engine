package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Rule of Law (MRD #19) — {2}{W} Enchantment.
 *
 *   Each player can't cast more than one spell each turn.
 *
 * The global (`eachPlayer = true`) form of `RestrictSpellsCastPerTurn`, so the cap binds the
 * opponent as well as the controller. The engine reads the per-player spells-cast-this-turn
 * tally at cast-legality time, so a spell cast earlier in the turn counts.
 */
class RuleOfLawScenarioTest : ScenarioTestBase() {

    init {
        context("Rule of Law") {

            test("the controller can't cast a second spell in the same turn") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Rule of Law")
                    .withCardsInHand(1, "Shock", 2)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val firstShock = game.findCardsInHand(1, "Shock").first()
                game.castSpellTargetingPlayer(1, "Shock", 2).isSuccess shouldBe true
                game.resolveStack()

                val secondShock = game.findCardsInHand(1, "Shock").first()
                withClue("A different card should be left in hand") {
                    (secondShock == firstShock) shouldBe false
                }

                val castActions = game.getLegalActions(1).filter {
                    val a = it.action
                    a is CastSpell && a.cardId == secondShock
                }
                withClue("Rule of Law must block the second spell this turn") {
                    castActions.isEmpty() shouldBe true
                }
            }

            test("the opponent, who doesn't control it, is capped too") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Rule of Law")
                    .withCardsInHand(2, "Shock", 2)
                    .withLandsOnBattlefield(2, "Mountain", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(2, "Shock", 1).isSuccess shouldBe true
                game.resolveStack()

                val secondShock = game.findCardsInHand(2, "Shock").first()
                val castActions = game.getLegalActions(2).filter {
                    val a = it.action
                    a is CastSpell && a.cardId == secondShock
                }
                withClue("The restriction is global, so the opponent is capped as well") {
                    castActions.isEmpty() shouldBe true
                }
            }

            test("control: without Rule of Law a second spell is castable") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardsInHand(1, "Shock", 2)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Shock", 2).isSuccess shouldBe true
                game.resolveStack()

                val secondShock = game.findCardsInHand(1, "Shock").first()
                val castActions = game.getLegalActions(1).filter {
                    val a = it.action
                    a is CastSpell && a.cardId == secondShock
                }
                withClue("Without the enchantment the second spell is legal") {
                    castActions.isNotEmpty() shouldBe true
                }
            }
        }
    }
}
