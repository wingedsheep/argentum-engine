package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Stone by Sunlight (HOB) — {1}{W} Instant
 *
 * Choose one —
 * • Destroy target creature with power 4 or greater.
 * • Until end of turn, target creature becomes an artifact in addition to its other types and
 *   gains indestructible.
 *
 * Mode 1 pins the power restriction; mode 2 pins that the creature *keeps* its other types
 * (in addition to, not instead of) and actually survives destruction.
 */
class StoneBySunlightScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Stone by Sunlight") {

            test("mode 1 destroys a creature with power 4 or greater") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Stone by Sunlight")
                    .withCardOnBattlefield(2, "Charging Rhino")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rhino = game.findPermanent("Charging Rhino")!!

                game.castSpellWithMode(1, "Stone by Sunlight", modeIndex = 0, targetId = rhino)
                    .error shouldBe null
                game.resolveStack()

                withClue("The 4/4 Rhino is a legal mode-1 target and is destroyed") {
                    game.isOnBattlefield("Charging Rhino") shouldBe false
                    game.isInGraveyard(2, "Charging Rhino") shouldBe true
                }
            }

            test("mode 1 can't target a creature with power 3 or less") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Stone by Sunlight")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                val result = game.castSpellWithMode(
                    1,
                    "Stone by Sunlight",
                    modeIndex = 0,
                    targetId = bears
                )
                withClue("A 2/2 is below the power-4 threshold") {
                    (result.error != null) shouldBe true
                }
            }

            test("mode 2 adds the artifact type and indestructible without removing creature-ness") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Stone by Sunlight")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpellWithMode(1, "Stone by Sunlight", modeIndex = 1, targetId = bears)
                    .error shouldBe null
                game.resolveStack()

                val projected = projector.project(game.state)
                withClue("It gained the artifact type") {
                    projected.hasType(bears, "ARTIFACT") shouldBe true
                }
                withClue("\"in addition to its other types\" — it is still a creature") {
                    projected.isCreature(bears) shouldBe true
                }
                withClue("It gained indestructible") {
                    projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe true
                }
                withClue("Its printed stats are untouched") {
                    projected.getPower(bears) shouldBe 2
                }
            }
        }
    }
}
