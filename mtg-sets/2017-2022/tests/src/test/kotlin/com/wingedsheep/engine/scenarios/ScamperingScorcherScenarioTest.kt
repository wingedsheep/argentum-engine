package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scampering Scorcher (M20 #158) — {3}{R} 1/1 Elemental.
 *
 * "When this creature enters, create two 1/1 red Elemental creature tokens. Elementals you
 *  control gain haste until end of turn."
 *
 * Covers that the two tokens and the Scorcher itself gain haste, while a non-Elemental creature
 * you control does not.
 */
class ScamperingScorcherScenarioTest : ScenarioTestBase() {

    init {
        context("Scampering Scorcher") {

            test("creates two Elemental tokens and gives every Elemental you control haste") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Scampering Scorcher")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Scampering Scorcher").error shouldBe null
                game.resolveStack()

                val tokens = game.findPermanents("Elemental Token")
                withClue("two 1/1 Elemental tokens") {
                    tokens.size shouldBe 2
                    tokens.forEach {
                        game.state.projectedState.getPower(it) shouldBe 1
                        game.state.projectedState.getToughness(it) shouldBe 1
                    }
                }

                val projected = game.state.projectedState
                withClue("the tokens and the Scorcher have haste") {
                    tokens.forEach { projected.hasKeyword(it, Keyword.HASTE) shouldBe true }
                    projected.hasKeyword(game.findPermanent("Scampering Scorcher")!!, Keyword.HASTE) shouldBe true
                }
                withClue("a non-Elemental creature you control does not") {
                    projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.HASTE) shouldBe false
                }
            }
        }
    }
}
