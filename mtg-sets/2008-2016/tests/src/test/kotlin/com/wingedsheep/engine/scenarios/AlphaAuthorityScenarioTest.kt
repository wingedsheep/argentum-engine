package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Alpha Authority: "Enchanted creature has hexproof and can't be blocked by more than one
 * creature." The blocker cap comes from the Aura (attached-creature scope), not the host's own
 * card — the first Aura-scoped use of `CantBeBlockedByMoreThan`.
 */
class AlphaAuthorityScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
        .withCardAttachedTo(1, "Alpha Authority", "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
        .withCardOnBattlefield(2, "Glory Seeker", summoningSickness = false)
        .withActivePlayer(1)
        .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
        .build()

    init {
        test("enchanted creature has hexproof") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!
            game.state.projectedState.hasKeyword(giant, Keyword.HEXPROOF) shouldBe true
        }

        test("enchanted creature can't be blocked by two creatures") {
            val game = board()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(
                mapOf("Grizzly Bears" to listOf("Hill Giant"), "Glory Seeker" to listOf("Hill Giant"))
            ).error shouldNotBe null
        }

        test("enchanted creature can still be blocked by one creature") {
            val game = board()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
        }
    }
}
