package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mirror Shield (THB) — "Equipped creature gets +0/+2 and has hexproof and 'Whenever a creature with
 * deathtouch blocks or becomes blocked by this creature, destroy that creature.'"
 *
 * Covers the granted combat trigger destroying a deathtouch blocker before combat damage (so the
 * equipped creature survives and stays blocked), and not firing for a blocker without deathtouch.
 */
class MirrorShieldScenarioTest : ScenarioTestBase() {

    private fun blockedBy(blocker: String): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardAttachedTo(1, "Mirror Shield", "Grizzly Bears")
            .withCardOnBattlefield(2, blocker)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            .build()

        game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareBlockers(mapOf(blocker to listOf("Grizzly Bears"))).error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        test("the equipped creature gets +0/+2 and hexproof") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Mirror Shield", "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 2
            game.state.projectedState.getToughness(bears) shouldBe 4
            game.state.projectedState.hasKeyword(bears, Keyword.HEXPROOF) shouldBe true
        }

        test("a deathtouch blocker is destroyed before damage and the attacker stays blocked") {
            val game = blockedBy("Typhoid Rats")

            withClue("the granted trigger destroyed the deathtouch blocker") {
                game.isInGraveyard(2, "Typhoid Rats") shouldBe true
            }

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            withClue("the equipped creature survived and, still blocked, dealt no damage") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.getLifeTotal(2) shouldBe 20
            }
        }

        test("a blocker without deathtouch doesn't trigger it") {
            val game = blockedBy("Hill Giant")

            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
