package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Searing Light (OGW) — {W} Instant.
 * "Destroy target attacking or blocking creature with power 2 or less."
 */
class SearingLightScenarioTest : ScenarioTestBase() {
    init {
        fun attackWith(attacker: String): TestGame {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(2, attacker)
                .withCardInHand(1, "Searing Light")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(2).inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS).build()
            game.declareAttackers(mapOf(attacker to 1)).error shouldBe null
            game.passPriority().error shouldBe null // active player passes; defender gets priority
            return game
        }

        test("destroys an attacking creature with power 2 or less") {
            val game = attackWith("Grizzly Bears")

            game.castSpell(1, "Searing Light", game.findPermanent("Grizzly Bears")).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }

        test("can't target an attacking creature with power 3") {
            val game = attackWith("Hill Giant")

            game.castSpell(1, "Searing Light", game.findPermanent("Hill Giant")).error shouldNotBe null
            game.isOnBattlefield("Hill Giant") shouldBe true
        }

        test("can't target a creature that isn't attacking or blocking") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Searing Light")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpell(1, "Searing Light", game.findPermanent("Grizzly Bears")).error shouldNotBe null
        }
    }
}
