package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Springsage Ritual (EMN #172) — {3}{G} Instant.
 *
 *   Destroy target artifact or enchantment. You gain 4 life.
 */
class SpringsageRitualScenarioTest : ScenarioTestBase() {

    init {
        test("destroys the target artifact and gains 4 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Springsage Ritual")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardOnBattlefield(2, "Ornithopter")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ornithopter = game.findPermanent("Ornithopter")!!
            val cast = game.castSpell(1, "Springsage Ritual", ornithopter)
            withClue("${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.isInGraveyard(2, "Ornithopter") shouldBe true
            game.getLifeTotal(1) shouldBe 24
        }
    }
}
