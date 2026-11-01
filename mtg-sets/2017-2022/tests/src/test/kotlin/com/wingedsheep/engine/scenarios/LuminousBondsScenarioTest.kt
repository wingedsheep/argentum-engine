package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Luminous Bonds (RIX #12) — {2}{W} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature can't attack or block.
 */
class LuminousBondsScenarioTest : ScenarioTestBase() {

    init {
        test("cast on an opposing creature, it can't block") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Luminous Bonds")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Luminous Bonds", game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Luminous Bonds") shouldBe true

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldNotBe null
            game.declareBlockers(mapOf("Savannah Lions" to listOf("Grizzly Bears"))).error shouldBe null
        }

        test("the enchanted creature can't attack; an unenchanted one can") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(2, "Luminous Bonds", "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldNotBe null
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        }
    }
}
