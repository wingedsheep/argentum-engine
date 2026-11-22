package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Mugging (GTC) — 2 damage to target creature; that creature can't block this turn. */
class MuggingScenarioTest : ScenarioTestBase() {
    init {
        test("deals 2 damage and the survivor can't block this turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Mugging")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Mugging", giant).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Hill Giant") shouldBe true
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldNotBe null
        }

        test("kills a 2-toughness creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Mugging")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Mugging", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }
    }
}
