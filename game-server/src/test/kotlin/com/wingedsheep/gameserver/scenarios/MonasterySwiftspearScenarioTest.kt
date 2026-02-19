package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Monastery Swiftspear.
 *
 * Card reference:
 * - Monastery Swiftspear ({R}): Creature — Human Monk, 1/2
 *   Haste
 *   Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 */
class MonasterySwiftspearScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Monastery Swiftspear") {

            test("gets +1/+1 until end of turn when controller casts a noncreature spell") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Monastery Swiftspear")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val swiftspearId = game.findPermanent("Monastery Swiftspear")!!

                // Verify base stats before casting
                val projectedBefore = stateProjector.project(game.state)
                withClue("Monastery Swiftspear should start as 1/2") {
                    projectedBefore.getPower(swiftspearId) shouldBe 1
                    projectedBefore.getToughness(swiftspearId) shouldBe 2
                }

                // Cast Lightning Bolt targeting opponent (noncreature spell triggers prowess)
                val castResult = game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                withClue("Casting Lightning Bolt should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Prowess should have triggered: Swiftspear is now 2/3
                val projectedAfter = stateProjector.project(game.state)
                withClue("Monastery Swiftspear should be 2/3 after prowess trigger") {
                    projectedAfter.getPower(swiftspearId) shouldBe 2
                    projectedAfter.getToughness(swiftspearId) shouldBe 3
                }
            }

            test("does not trigger when controller casts a creature spell") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Monastery Swiftspear")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Grizzly Bears")
                withClue("Casting Grizzly Bears should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // Prowess should NOT trigger for creature spells
                val swiftspearId = game.findPermanent("Monastery Swiftspear")!!
                val projected = stateProjector.project(game.state)
                withClue("Monastery Swiftspear should still be 1/2 (creature spell doesn't trigger prowess)") {
                    projected.getPower(swiftspearId) shouldBe 1
                    projected.getToughness(swiftspearId) shouldBe 2
                }
            }

            test("triggers multiple times from multiple noncreature spells in the same turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Monastery Swiftspear")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val swiftspearId = game.findPermanent("Monastery Swiftspear")!!

                // Cast first Lightning Bolt
                val cast1 = game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                withClue("First Lightning Bolt should succeed: ${cast1.error}") {
                    cast1.error shouldBe null
                }
                game.resolveStack()

                val projectedAfterFirst = stateProjector.project(game.state)
                withClue("Monastery Swiftspear should be 2/3 after first prowess") {
                    projectedAfterFirst.getPower(swiftspearId) shouldBe 2
                    projectedAfterFirst.getToughness(swiftspearId) shouldBe 3
                }

                // Cast second Lightning Bolt
                val cast2 = game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                withClue("Second Lightning Bolt should succeed: ${cast2.error}") {
                    cast2.error shouldBe null
                }
                game.resolveStack()

                val projectedAfterSecond = stateProjector.project(game.state)
                withClue("Monastery Swiftspear should be 3/4 after two prowess triggers") {
                    projectedAfterSecond.getPower(swiftspearId) shouldBe 3
                    projectedAfterSecond.getToughness(swiftspearId) shouldBe 4
                }
            }

            test("prowess boost wears off at end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Monastery Swiftspear")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val swiftspearId = game.findPermanent("Monastery Swiftspear")!!

                // Cast Lightning Bolt to trigger prowess
                val castResult = game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                withClue("Casting Lightning Bolt should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // Verify prowess applied
                val projectedDuring = stateProjector.project(game.state)
                withClue("Monastery Swiftspear should be 2/3 during the turn") {
                    projectedDuring.getPower(swiftspearId) shouldBe 2
                    projectedDuring.getToughness(swiftspearId) shouldBe 3
                }

                // Pass to cleanup step to clear end-of-turn effects
                game.passUntilPhase(Phase.ENDING, Step.CLEANUP)

                val projectedAfter = stateProjector.project(game.state)
                withClue("Monastery Swiftspear should be back to 1/2 after end of turn") {
                    projectedAfter.getPower(swiftspearId) shouldBe 1
                    projectedAfter.getToughness(swiftspearId) shouldBe 2
                }
            }
        }
    }
}
