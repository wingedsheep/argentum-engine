package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Electrostatic Bolt — {R} Instant (Mirrodin #89)
 *
 * "Electrostatic Bolt deals 2 damage to target creature. If it's an artifact creature,
 *  Electrostatic Bolt deals 4 damage to it instead."
 *
 * Both branches are pinned with 3/3–4/4 bodies so the two amounts are actually distinguishable:
 * a 4/4 nonartifact must survive (proving 2, not 4) and a 3/3 artifact creature must die
 * (proving 4, not 2).
 */
class ElectrostaticBoltScenarioTest : ScenarioTestBase() {

    init {
        context("Electrostatic Bolt") {

            test("deals 2 to a nonartifact creature") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Electrostatic Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Fangren Hunter") // 4/4, nonartifact
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(3) { builder = builder.withCardInLibrary(1, "Mountain") }
                repeat(3) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val hunter = game.findPermanent("Fangren Hunter")!!
                val cast = game.castSpell(1, "Electrostatic Bolt", hunter)
                withClue("Casting Electrostatic Bolt should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                // 4 damage would kill a 4/4; surviving proves the 2-damage branch was taken.
                withClue("Fangren Hunter (4/4) should survive 2 damage") {
                    game.isOnBattlefield("Fangren Hunter") shouldBe true
                }
            }

            test("deals 4 to an artifact creature instead") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Electrostatic Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Titanium Golem") // 3/3 artifact creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(3) { builder = builder.withCardInLibrary(1, "Mountain") }
                repeat(3) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val golem = game.findPermanent("Titanium Golem")!!
                val cast = game.castSpell(1, "Electrostatic Bolt", golem)
                withClue("Casting Electrostatic Bolt should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                // 2 damage would leave a 3/3 alive; dying proves the 4-damage branch was taken.
                withClue("Titanium Golem (3/3 artifact) should die to 4 damage") {
                    game.isOnBattlefield("Titanium Golem") shouldBe false
                    game.isInGraveyard(2, "Titanium Golem") shouldBe true
                }
            }
        }
    }
}
