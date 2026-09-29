package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Errant and Giada (MOM #224) — "You may cast spells with flash or flying from the top of your
 * library." Flying and flash spells are castable from the top; a plain creature, a land, and an
 * instant (which has no flash keyword) are not.
 */
class ErrantAndGiadaScenarioTest : ScenarioTestBase() {

    // Islands, Forests and Mountains, so every candidate below is affordable and a negative
    // result can only come from the flash/flying filter, never from missing colored mana.
    private fun board(topCard: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Errant and Giada")
        .withCardInLibrary(1, topCard)
        .withLandsOnBattlefield(1, "Island", 3)
        .withLandsOnBattlefield(1, "Forest", 2)
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.canCastFromTop(name: String): Boolean = getLegalActions(1).any {
        it.actionType == "CastSpell" && it.sourceZone == "LIBRARY" && it.description.contains(name)
    }

    init {
        context("Errant and Giada") {
            test("a flying creature on top can be cast from the library") {
                val game = board("Wind Drake")
                game.canCastFromTop("Wind Drake") shouldBe true

                val drake = game.findCardsInLibrary(1, "Wind Drake").single()
                game.execute(CastSpell(game.player1Id, drake)).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Wind Drake") shouldBe true
            }

            test("a flash creature on top can be cast from the library") {
                val game = board("Ambush Viper")
                game.canCastFromTop("Ambush Viper") shouldBe true
            }

            test("a creature without flash or flying cannot be cast from the library") {
                val game = board("Grizzly Bears")
                game.canCastFromTop("Grizzly Bears") shouldBe false
            }

            test("an instant without the flash keyword cannot be cast from the library") {
                val game = board("Lightning Bolt")
                game.canCastFromTop("Lightning Bolt") shouldBe false
            }
        }
    }
}
