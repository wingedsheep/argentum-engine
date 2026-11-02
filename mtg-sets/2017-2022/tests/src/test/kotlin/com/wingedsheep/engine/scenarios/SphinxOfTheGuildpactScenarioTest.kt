package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sphinx of the Guildpact (RNA #241) — {7} Artifact Creature — Sphinx 5/5.
 *
 * "Sphinx of the Guildpact is all colors. Flying. Hexproof from monocolored."
 * Ruling (2020-08-07): the all-colors ability applies in all zones, not just the battlefield.
 */
class SphinxOfTheGuildpactScenarioTest : ScenarioTestBase() {

    private val allColors = Color.entries.mapTo(mutableSetOf()) { it.name }

    init {
        context("Sphinx of the Guildpact") {

            test("is all colors in hand, on the stack and on the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sphinx of the Guildpact")
                    .withLandsOnBattlefield(1, "Island", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val inHand = game.findCardsInHand(1, "Sphinx of the Guildpact").single()
                withClue("in hand") { game.state.projectedState.colorsOf(inHand) shouldBe allColors }

                game.castSpell(1, "Sphinx of the Guildpact").error shouldBe null
                withClue("on the stack") { game.state.projectedState.colorsOf(inHand) shouldBe allColors }

                game.resolveStack()
                val sphinx = game.findPermanent("Sphinx of the Guildpact")!!
                withClue("on the battlefield") { game.state.projectedState.getColors(sphinx) shouldBe allColors }
            }

            test("hexproof from monocolored: an opponent's monocolored spell can't target it, a multicolored one can") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Sphinx of the Guildpact")
                    .withCardInHand(1, "Lightning Helix")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sphinx = game.findPermanent("Sphinx of the Guildpact")!!
                game.castSpell(1, "Lightning Bolt", sphinx).error shouldNotBe null
                game.castSpell(1, "Lightning Helix", sphinx).error shouldBe null
            }
        }
    }
}
