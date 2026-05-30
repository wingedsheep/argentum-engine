package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Blurred Mongoose.
 *
 * Card reference:
 * - Blurred Mongoose ({1}{G}): Creature — Mongoose 2/1
 *   This spell can't be countered.
 *   Shroud (This creature can't be the target of spells or abilities.)
 */
class BlurredMongooseScenarioTest : ScenarioTestBase() {

    init {
        context("Blurred Mongoose") {

            test("definition marks the spell as uncounterable") {
                val def = cardRegistry.getCard("Blurred Mongoose")!!
                withClue("Blurred Mongoose's spell can't be countered") {
                    def.script.cantBeCountered shouldBe true
                }
            }

            test("resolves to a 2/1 with shroud on the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Blurred Mongoose")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Blurred Mongoose")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                val mongooseId = game.findPermanent("Blurred Mongoose")!!
                val clientState = game.getClientState(1)
                val mongoose = clientState.cards[mongooseId]!!

                withClue("Blurred Mongoose should have shroud") {
                    mongoose.keywords.contains(Keyword.SHROUD) shouldBe true
                }
                withClue("Blurred Mongoose should be a 2/1") {
                    mongoose.power shouldBe 2
                    mongoose.toughness shouldBe 1
                }
            }
        }
    }
}
