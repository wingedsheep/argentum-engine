package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Despoil — {3}{B} Sorcery. "Destroy target land. Its controller loses 2 life."
 *
 * The life loss names the land's *controller*: the card used to aim it at the land itself, so
 * nobody lost life.
 */
class DespoilScenarioTest : ScenarioTestBase() {

    init {
        context("Despoil") {
            test("destroys the land and its controller loses 2 life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Despoil")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forest = game.findPermanent("Forest")!!
                val cast = game.castSpell(1, "Despoil", forest)
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Forest should be destroyed") { game.isOnBattlefield("Forest") shouldBe false }
                withClue("The land's controller loses 2 life, the caster none") {
                    game.getLifeTotal(2) shouldBe 18
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
