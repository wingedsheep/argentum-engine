package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

class PreciseRedactionScenarioTest : ScenarioTestBase() {
    init {
        for ((spell, land, allowed) in listOf(
            Triple("Savannah Lions", "Plains", true),
            Triple("Walking Corpse", "Swamp", true),
            Triple("Grizzly Bears", "Forest", false)
        )) {
            test("color restriction for $spell") {
                val game = scenario().withPlayers()
                    .withCardInHand(1, "Precise Redaction")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(2, spell)
                    .withLandsOnBattlefield(2, land, 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
                game.castSpell(2, spell).error shouldBe null
                game.passPriority()
                val result = game.castSpellTargetingStackSpell(1, "Precise Redaction", spell)
                if (allowed) {
                    result.error shouldBe null
                    game.resolveStack()
                    game.isInGraveyard(2, spell) shouldBe true
                } else {
                    result.error.shouldNotBeNull()
                    game.isInHand(1, "Precise Redaction") shouldBe true
                }
            }
        }
    }
}
