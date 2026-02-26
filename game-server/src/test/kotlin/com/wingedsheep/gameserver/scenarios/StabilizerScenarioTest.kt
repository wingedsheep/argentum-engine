package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

class StabilizerScenarioTest : ScenarioTestBase() {
    init {
        context("Stabilizer prevents cycling") {
            test("players cannot cycle cards when Stabilizer is on the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Disciple of Grace")
                    .withCardOnBattlefield(1, "Stabilizer")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycleResult = game.cycleCard(1, "Disciple of Grace")
                cycleResult.error.shouldNotBeNull()
                cycleResult.error shouldBe "Cycling is prevented"

                // Card should still be in hand
                game.handSize(1) shouldBe 1
            }

            test("opponent cannot cycle cards when Stabilizer is on the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(2, "Forgotten Cave")
                    .withCardOnBattlefield(1, "Stabilizer")
                    .withLandsOnBattlefield(2, "Mountain", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycleResult = game.cycleCard(2, "Forgotten Cave")
                cycleResult.error.shouldNotBeNull()
                cycleResult.error shouldBe "Cycling is prevented"
            }

            test("cycling works normally after Stabilizer leaves the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Disciple of Grace")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // No Stabilizer on battlefield — cycling should work
                val cycleResult = game.cycleCard(1, "Disciple of Grace")
                cycleResult.error shouldBe null
                game.isInGraveyard(1, "Disciple of Grace") shouldBe true
                game.handSize(1) shouldBe 1
            }

            test("typecycling is also prevented by Stabilizer") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Noble Templar")
                    .withCardOnBattlefield(1, "Stabilizer")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycleResult = game.typecycleCard(1, "Noble Templar")
                cycleResult.error.shouldNotBeNull()
                cycleResult.error shouldBe "Cycling is prevented"
            }
        }
    }
}
