package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Radioactive Spider.
 *
 * Card reference:
 * - Radioactive Spider ({G}): Creature — Spider, 1/1
 *   "Reach"
 *   "Deathtouch"
 */
class RadioactiveSpiderScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Radioactive Spider — cast") {

            test("enters the battlefield as a 1/1 with reach and deathtouch when cast for {G}") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Radioactive Spider")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Radioactive Spider")
                withClue("Casting Radioactive Spider for {G} should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Radioactive Spider should be on the battlefield") {
                    game.isOnBattlefield("Radioactive Spider") shouldBe true
                }

                val spiderId = game.findPermanent("Radioactive Spider")!!
                val projected = stateProjector.project(game.state)

                withClue("Radioactive Spider should be a 1/1") {
                    stateProjector.getProjectedPower(game.state, spiderId) shouldBe 1
                    stateProjector.getProjectedToughness(game.state, spiderId) shouldBe 1
                }

                withClue("Radioactive Spider should have reach") {
                    projected.hasKeyword(spiderId, Keyword.REACH) shouldBe true
                }

                withClue("Radioactive Spider should have deathtouch") {
                    projected.hasKeyword(spiderId, Keyword.DEATHTOUCH) shouldBe true
                }
            }
        }
    }
}
