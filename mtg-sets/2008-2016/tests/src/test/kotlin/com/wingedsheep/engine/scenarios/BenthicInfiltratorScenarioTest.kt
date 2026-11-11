package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Benthic Infiltrator — an unblockable ingest creature: each hit makes the damaged player exile
 * the top card of their library (CR 702.115a), face up in that player's exile.
 */
class BenthicInfiltratorScenarioTest : ScenarioTestBase() {
    init {
        test("it can't be blocked, and combat damage to a player makes that player exile the top card of their library") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Benthic Infiltrator")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Benthic Infiltrator" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("it can't be blocked") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Benthic Infiltrator"))).error shouldNotBe null
            }
            game.declareBlockers(emptyMap()).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.getLifeTotal(2) shouldBe 19
            game.isInExile(2, "Island") shouldBe true
            game.librarySize(2) shouldBe 0
            // Only the damaged player's library is touched.
            game.librarySize(1) shouldBe 1
        }
    }
}
