package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Judith, the Scourge Diva (RNA #185) — other creatures you control get +1/+0; whenever a nontoken
 * creature you control dies, Judith deals 1 damage to any target.
 */
class JudithTheScourgeDivaScenarioTest : ScenarioTestBase() {
    init {
        test("pumps only her other creatures") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Judith, the Scourge Diva")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val projected = game.state.projectedState
            projected.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 3
            projected.getPower(game.findPermanent("Judith, the Scourge Diva")!!) shouldBe 2
            projected.getPower(game.findPermanent("Hill Giant")!!) shouldBe 3
        }

        test("a nontoken creature of hers dying deals 1 damage to any target") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Judith, the Scourge Diva")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true

            game.getPendingDecision().shouldNotBeNull()
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
        }
    }
}
