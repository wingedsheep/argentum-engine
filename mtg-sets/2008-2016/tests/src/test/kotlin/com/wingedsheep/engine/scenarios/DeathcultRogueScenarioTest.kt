package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Deathcult Rogue (GTC) — can't be blocked except by Rogues. */
class DeathcultRogueScenarioTest : ScenarioTestBase() {
    init {
        test("a non-Rogue can't block it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Deathcult Rogue")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Deathcult Rogue" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Deathcult Rogue"))).error shouldNotBe null
        }

        test("a Rogue can block it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Deathcult Rogue")
                .withCardOnBattlefield(2, "Deathcult Rogue")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val rogues = game.findAllPermanents("Deathcult Rogue")
            val attacker = rogues.single { it in game.state.getBattlefield(game.player1Id) }
            val blocker = rogues.single { it != attacker }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.execute(DeclareAttackers(game.player1Id, mapOf(attacker to game.player2Id))).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.execute(DeclareBlockers(game.player2Id, mapOf(blocker to listOf(attacker)))).error shouldBe null
        }
    }
}
