package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Fade from Memory.
 *
 * Card reference:
 * - Fade from Memory ({B}): Instant
 *   "Exile target card from a graveyard."
 *   Cycling {B}
 */
class FadeFromMemoryScenarioTest : ScenarioTestBase() {

    init {
        context("Fade from Memory") {
            test("exiles a card from opponent's graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Fade from Memory")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpellTargetingGraveyardCard(1, "Fade from Memory", 2, "Grizzly Bears")
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Grizzly Bears should be exiled") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                }
                withClue("Fade from Memory should be in player 1's graveyard") {
                    game.isInGraveyard(1, "Fade from Memory") shouldBe true
                }
            }

            test("exiles a card from own graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Fade from Memory")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpellTargetingGraveyardCard(1, "Fade from Memory", 1, "Grizzly Bears")
                withClue("Cast should succeed targeting own graveyard") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Grizzly Bears should be exiled from own graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }
            }

            test("cannot be cast when all graveyards are empty") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Fade from Memory")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Fade from Memory")
                withClue("Cast should fail when no graveyard cards exist") {
                    castResult.error shouldNotBe null
                }
            }

            test("cycling discards the card and draws a new one") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Fade from Memory")
                    .withCardInLibrary(1, "Mountain")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handSizeBefore = game.handSize(1)

                val cycleResult = game.cycleCard(1, "Fade from Memory")
                withClue("Cycling should succeed") {
                    cycleResult.error shouldBe null
                }

                withClue("Hand size should remain the same after cycling (discard 1, draw 1)") {
                    game.handSize(1) shouldBe handSizeBefore
                }
                withClue("Fade from Memory should be in graveyard after cycling") {
                    game.isInGraveyard(1, "Fade from Memory") shouldBe true
                }
                withClue("Mountain should have been drawn from library") {
                    game.isInHand(1, "Mountain") shouldBe true
                }
            }
        }
    }
}
