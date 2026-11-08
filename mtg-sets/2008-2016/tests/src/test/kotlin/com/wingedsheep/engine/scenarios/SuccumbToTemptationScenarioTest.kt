package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Succumb to Temptation (EMN #107) — {1}{B}{B} Instant.
 *
 *   You draw two cards and you lose 2 life.
 */
class SuccumbToTemptationScenarioTest : ScenarioTestBase() {

    init {
        test("the caster draws two cards and loses 2 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Succumb to Temptation")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpell(1, "Succumb to Temptation")
            withClue("${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("both library cards were drawn") {
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInHand(1, "Hill Giant") shouldBe true
            }
            withClue("the caster lost 2 life, the opponent nothing") {
                game.getLifeTotal(1) shouldBe 18
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
