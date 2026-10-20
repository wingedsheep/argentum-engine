package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContainIgnoringCase

/**
 * Livonya Silone — "First strike; legendary landwalk." Legendary landwalk (CR 702.14c) makes her
 * unblockable only while the defending player controls a *legendary* land: a basic or an ordinary
 * nonbasic land doesn't open the path.
 */
class LivonyaSiloneScenarioTest : ScenarioTestBase() {

    private fun attackInto(defenderLand: String) = scenario()
        .withPlayers()
        .withCardOnBattlefield(1, "Livonya Silone")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, defenderLand)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()
        .also { game ->
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Livonya Silone" to 2)).error.shouldBeNull()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        }

    init {
        test("can't be blocked while the defending player controls a legendary land") {
            val game = attackInto("Pendelhaven")
            val result = game.declareBlockers(mapOf("Grizzly Bears" to listOf("Livonya Silone")))
            result.error.shouldNotBeNull() shouldContainIgnoringCase "legendary landwalk"
        }

        test("can be blocked when the defending player controls only a basic land") {
            val game = attackInto("Forest")
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Livonya Silone"))).error.shouldBeNull()
        }

        test("a nonlegendary nonbasic land doesn't make her unblockable") {
            val game = attackInto("Adventurers' Guildhouse")
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Livonya Silone"))).error.shouldBeNull()
        }
    }
}
