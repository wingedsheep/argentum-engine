package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Welcoming Vampire (VOW #46) — {2}{W} Creature — Vampire, 2/3, flying.
 *
 *   Whenever one or more other creatures you control with power 2 or less enter, draw a card.
 *   This ability triggers only once each turn.
 *
 * Exercises the ETB draw trigger gated on power <= 2, and the once-per-turn cap when multiple
 * small creatures enter in the same turn.
 */
class WelcomingVampireScenarioTest : ScenarioTestBase() {

    init {
        context("Welcoming Vampire — draw when a small creature you control enters") {

            test("another creature you control with power 2 or less entering draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Welcoming Vampire", summoningSickness = false)
                    .withCardInHand(1, "Savannah Lions") // 1/1, power <= 2
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handSizeBefore = game.handSize(1)

                game.castSpell(1, "Savannah Lions").error shouldBe null
                game.resolveStack()

                withClue("drew a card from the small creature entering (net: -1 for casting, +1 from draw)") {
                    game.handSize(1) shouldBe handSizeBefore
                }
                withClue("Welcoming Vampire is a 2/3 flyer") {
                    val vamp = game.findPermanent("Welcoming Vampire")!!
                    game.state.projectedState.getPower(vamp) shouldBe 2
                    game.state.projectedState.getToughness(vamp) shouldBe 3
                    game.state.projectedState.hasKeyword(vamp, Keyword.FLYING) shouldBe true
                }
            }

            test("a creature with power greater than 2 entering does not trigger the draw") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Welcoming Vampire", summoningSickness = false)
                    .withCardInHand(1, "Hill Giant") // 3/3, power > 2
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .build()

                val handSizeBefore = game.handSize(1)

                game.castSpell(1, "Hill Giant").error shouldBe null
                game.resolveStack()

                withClue("casting Hill Giant costs a card and no draw replaces it") {
                    game.handSize(1) shouldBe handSizeBefore - 1
                }
            }

            test("only triggers once per turn even if two small creatures enter") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Welcoming Vampire", summoningSickness = false)
                    .withCardInHand(1, "Savannah Lions")
                    .withCardInHand(1, "Glory Seeker") // 2/2, power <= 2
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Savannah Lions").error shouldBe null
                game.resolveStack()
                val handSizeAfterFirst = game.handSize(1)

                game.castSpell(1, "Glory Seeker").error shouldBe null
                game.resolveStack()

                withClue("second small creature this turn does not draw again (once per turn)") {
                    game.handSize(1) shouldBe handSizeAfterFirst - 1
                }
            }
        }
    }
}
