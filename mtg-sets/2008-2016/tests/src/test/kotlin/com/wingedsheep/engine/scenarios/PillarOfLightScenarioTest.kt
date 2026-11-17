package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Pillar of Light: exile target creature with toughness 4 or greater.
 */
class PillarOfLightScenarioTest : ScenarioTestBase() {
    init {
        test("exiles a creature with toughness 4 or greater") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardInHand(1, "Pillar of Light")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(2, "Craw Wurm")
                .build()
            game.castSpell(1, "Pillar of Light", game.findPermanent("Craw Wurm")!!).error shouldBe null
            game.resolveStack()
            game.isInExile(2, "Craw Wurm") shouldBe true
        }

        test("a creature with toughness 3 or less is not a legal target") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardInHand(1, "Pillar of Light")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
            game.castSpell(1, "Pillar of Light", game.findPermanent("Hill Giant")!!).error shouldNotBe null
        }
    }
}
