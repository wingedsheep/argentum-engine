package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for So Tiny (ELD #64) — {U} Enchantment — Aura.
 *
 *   Flash
 *   Enchant creature
 *   Enchanted creature gets -2/-0. It gets -6/-0 instead as long as its controller has seven or
 *   more cards in their graveyard.
 *
 * "Its controller" is the enchanted creature's controller, and the check is continuous (ELD ruling).
 */
class SoTinyScenarioTest : ScenarioTestBase() {

    init {
        context("So Tiny") {

            test("six cards in the enchanted creature's controller's graveyard: -2/-0") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "So Tiny")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 1)
                repeat(6) { builder.withCardInGraveyard(2, "Forest") }
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "So Tiny", targetId = giant).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant (3/3) gets -2/-0") {
                    game.state.projectedState.getPower(giant) shouldBe 1
                    game.state.projectedState.getToughness(giant) shouldBe 3
                }
            }

            test("seven cards in the enchanted creature's controller's graveyard: -6/-0 instead") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(1, "So Tiny", "Hill Giant")
                repeat(7) { builder.withCardInGraveyard(2, "Forest") }
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                withClue("Hill Giant (3/3) gets -6/-0, not -2/-0 and -6/-0 stacked") {
                    game.state.projectedState.getPower(giant) shouldBe -3
                    game.state.projectedState.getToughness(giant) shouldBe 3
                }
            }

            test("the Aura controller's graveyard does not count") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(1, "So Tiny", "Hill Giant")
                repeat(10) { builder.withCardInGraveyard(1, "Island") }
                repeat(6) { builder.withCardInGraveyard(2, "Forest") }
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                withClue("Player2 has only six cards in their graveyard, so it stays -2/-0") {
                    game.state.projectedState.getPower(giant) shouldBe 1
                }
            }

            test("the bonus updates continuously when the seventh card hits the graveyard") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardAttachedTo(1, "So Tiny", "Hill Giant")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                repeat(6) { builder.withCardInGraveyard(1, "Island") }
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.state.projectedState.getPower(giant) shouldBe 1

                game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
                game.resolveStack()

                withClue("Shock is the seventh card in Player1's graveyard: 3 - 6 = -3") {
                    game.graveyardSize(1) shouldBe 7
                    game.state.projectedState.getPower(giant) shouldBe -3
                    game.state.projectedState.getToughness(giant) shouldBe 3
                }
            }
        }
    }
}
