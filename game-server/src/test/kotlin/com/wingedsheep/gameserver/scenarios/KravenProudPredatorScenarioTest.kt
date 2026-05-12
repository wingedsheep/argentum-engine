package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Kraven, Proud Predator.
 *
 * Card reference:
 * - Kraven, Proud Predator ({1}{R}{G}): Legendary Creature — Human Warrior Villain, cda/4
 *   "Vigilance"
 *   "Kraven, Proud Predator's power is equal to the greatest mana value among permanents
 *    you control." (Layer 7b CDA — not a triggered ability)
 */
class KravenProudPredatorScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Kraven, Proud Predator — cast and baseline characteristics") {

            test("Kraven enters with legendary creature type line, vigilance, toughness 4, and power equals greatest mana value") {
                // Only other permanents are basic lands (mana value 0).
                // After resolving, controller has: 3 tapped lands (MV 0) + Kraven (MV 3).
                // CDA (Layer 7b) sets Kraven's printed power to greatest MV among controller's permanents = 3.
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Kraven, Proud Predator")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Kraven, Proud Predator")
                withClue("Casting Kraven with {1}{R}{G} should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Kraven should be on the battlefield") {
                    game.isOnBattlefield("Kraven, Proud Predator") shouldBe true
                }

                val kravenId = game.findPermanent("Kraven, Proud Predator")!!
                val projected = stateProjector.project(game.state)

                withClue("Kraven should be legendary") {
                    projected.isLegendary(kravenId) shouldBe true
                }
                withClue("Kraven should be a creature") {
                    projected.isCreature(kravenId) shouldBe true
                }
                withClue("Kraven should have subtype Human") {
                    projected.hasSubtype(kravenId, "Human") shouldBe true
                }
                withClue("Kraven should have subtype Warrior") {
                    projected.hasSubtype(kravenId, "Warrior") shouldBe true
                }
                withClue("Kraven should have subtype Villain") {
                    projected.hasSubtype(kravenId, "Villain") shouldBe true
                }
                withClue("Kraven should have vigilance") {
                    projected.hasKeyword(kravenId, Keyword.VIGILANCE) shouldBe true
                }
                withClue("Kraven's toughness should be 4") {
                    projected.getToughness(kravenId) shouldBe 4
                }
                withClue("Kraven's power should equal the greatest mana value among controller's permanents (Kraven MV 3, lands MV 0 → power 3)") {
                    projected.getPower(kravenId) shouldBe 3
                }
            }
        }
    }
}
