package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Aspect of Gorgon (JOU #60) — {2}{B} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature gets +1/+3 and has deathtouch.
 */
class AspectOfGorgonScenarioTest : ScenarioTestBase() {
    init {
        test("the enchanted creature becomes a 3/5 with deathtouch") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Aspect of Gorgon")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe false

            game.castSpell(1, "Aspect of Gorgon", bears).error shouldBe null
            game.resolveStack()

            withClue("Aspect of Gorgon is on the battlefield") {
                game.isOnBattlefield("Aspect of Gorgon") shouldBe true
            }
            val projected = game.state.projectedState
            withClue("+1/+3") {
                projected.getPower(bears) shouldBe 3
                projected.getToughness(bears) shouldBe 5
            }
            withClue("and deathtouch") {
                projected.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe true
            }
        }
    }
}
