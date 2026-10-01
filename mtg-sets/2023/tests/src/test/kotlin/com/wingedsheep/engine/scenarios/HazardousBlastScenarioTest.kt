package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hazardous Blast (ONE #135) — {3}{R} Sorcery.
 * 1 damage to each creature your opponents control; those creatures can't block this turn.
 */
class HazardousBlastScenarioTest : ScenarioTestBase() {
    init {
        test("damages only opponents' creatures and they can't block this turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Hazardous Blast")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Hazardous Blast").error shouldBe null
            game.resolveStack()

            game.findPermanent("Llanowar Elves") shouldBe null
            game.findPermanent("Hill Giant") shouldNotBe null
            game.findPermanent("Savannah Lions") shouldNotBe null
            game.findPermanent("Grizzly Bears") shouldNotBe null

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldNotBe null
        }

        test("control: without the spell the same block is legal") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
        }
    }
}
