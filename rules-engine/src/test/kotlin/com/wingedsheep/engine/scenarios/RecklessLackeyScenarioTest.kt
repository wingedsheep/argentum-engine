package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Reckless Lackey (OTJ #140) — {R} Goblin Pirate, 1/2, First strike, Haste.
 *
 *   "{2}{R}, Sacrifice this creature: Draw a card and create a Treasure token."
 *
 * Verifies the activated ability: paying {2}{R} and sacrificing the Lackey draws a card and
 * mints a Treasure token.
 */
class RecklessLackeyScenarioTest : ScenarioTestBase() {

    private val lackeyAbilityId =
        cardRegistry.getCard("Reckless Lackey")!!.activatedAbilities.first().id

    init {
        context("Reckless Lackey") {

            test("sacrifice ability draws a card and creates a Treasure") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Reckless Lackey")
                    .withCardInLibrary(1, "Grizzly Bears") // a card to draw
                    .withLandsOnBattlefield(1, "Mountain", 3) // {2}{R}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                val lackey = game.findPermanent("Reckless Lackey")!!

                withClue("No Treasure exists before activation") {
                    game.findPermanent("Treasure") shouldBe null
                }

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = lackey,
                        abilityId = lackeyAbilityId,
                    )
                )
                withClue("Activating Reckless Lackey's ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Reckless Lackey is sacrificed as part of the cost") {
                    game.isOnBattlefield("Reckless Lackey") shouldBe false
                }
                withClue("A card is drawn") {
                    game.handSize(1) shouldBe handBefore + 1
                }
                withClue("Exactly one Treasure token is created") {
                    game.findAllPermanents("Treasure").size shouldBe 1
                }
            }
        }
    }
}
