package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Deadly Wanderings (DTK #94) — {3}{B}{B} Enchantment.
 * "As long as you control exactly one creature, that creature gets +2/+0 and has deathtouch and
 * lifelink."
 *
 * Proves the "exactly one" gate turns the bonus on for a lone creature (and its lifelink really
 * gains life on combat damage), and off as soon as a second creature is on your side.
 */
class DeadlyWanderingsScenarioTest : ScenarioTestBase() {
    init {
        test("a lone creature gets +2/+0, deathtouch and lifelink, and its damage gains life") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Deadly Wanderings")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 4
            projected.getToughness(bears) shouldBe 2
            projected.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe true
            projected.hasKeyword(bears, Keyword.LIFELINK) shouldBe true

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("4 unblocked damage, all of it gained back through lifelink") {
                game.getLifeTotal(2) shouldBe 16
                game.getLifeTotal(1) shouldBe 24
            }
        }

        test("with two creatures neither gets the bonus") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Deadly Wanderings")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 2
            projected.getPower(giant) shouldBe 3
            projected.hasKeyword(bears, Keyword.DEATHTOUCH) shouldBe false
            projected.hasKeyword(giant, Keyword.LIFELINK) shouldBe false
        }

        test("an opponent's creatures don't count against the gate") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Deadly Wanderings")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 4
            projected.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
            projected.getPower(giant) shouldBe 3
            projected.hasKeyword(giant, Keyword.DEATHTOUCH) shouldBe false
        }
    }
}
