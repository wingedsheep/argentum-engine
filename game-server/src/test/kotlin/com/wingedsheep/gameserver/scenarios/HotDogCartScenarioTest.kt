package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Hot Dog Cart.
 *
 * Hot Dog Cart {3}
 * Artifact
 *
 * When this artifact enters, create a Food token. (It's an artifact with "{2}, {T},
 * Sacrifice this token: You gain 3 life.")
 * {T}: Add one mana of any color.
 */
class HotDogCartScenarioTest : ScenarioTestBase() {

    init {
        context("Hot Dog Cart — ETB creates a Food token") {

            test("casting Hot Dog Cart resolves into a Food token on the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Hot Dog Cart")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Food should not exist before Hot Dog Cart resolves") {
                    game.findPermanent("Food") shouldBe null
                }

                val castResult = game.castSpell(1, "Hot Dog Cart")
                withClue("Should cast Hot Dog Cart: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("Hot Dog Cart should be on the battlefield") {
                    game.isOnBattlefield("Hot Dog Cart") shouldBe true
                }
                withClue("ETB trigger should have created a Food token") {
                    game.findPermanent("Food") shouldNotBe null
                }
            }
        }
    }
}
