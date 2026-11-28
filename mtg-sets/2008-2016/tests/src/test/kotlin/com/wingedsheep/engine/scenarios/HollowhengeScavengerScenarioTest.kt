package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hollowhenge Scavenger (ISD #188) — {3}{G}{G} 4/5 Elemental.
 *
 *   Morbid — When this creature enters, if a creature died this turn, you gain 5 life.
 */
class HollowhengeScavengerScenarioTest : ScenarioTestBase() {

    private fun buildGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Hollowhenge Scavenger")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Forest", 5)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("gains 5 life when a creature died this turn") {
            val game = buildGame()
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true

            val cast = game.castSpell(1, "Hollowhenge Scavenger")
            withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 25
        }

        test("gains nothing when no creature died this turn") {
            val game = buildGame()
            game.castSpell(1, "Hollowhenge Scavenger").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Hollowhenge Scavenger") shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
