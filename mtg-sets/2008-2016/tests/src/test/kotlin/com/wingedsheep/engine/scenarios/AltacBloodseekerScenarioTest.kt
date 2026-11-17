package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Altac Bloodseeker: whenever a creature an opponent controls dies, it gets +2/+0 and gains first
 * strike and haste until end of turn. Your own creatures dying don't trigger it.
 */
class AltacBloodseekerScenarioTest : ScenarioTestBase() {
    init {
        test("an opponent's creature dying pumps it and grants first strike and haste") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardOnBattlefield(1, "Altac Bloodseeker")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .build()
            game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            val altac = game.findPermanent("Altac Bloodseeker")!!
            val projected = game.state.projectedState
            projected.getPower(altac) shouldBe 4
            projected.getToughness(altac) shouldBe 1
            projected.hasKeyword(altac, Keyword.FIRST_STRIKE) shouldBe true
            projected.hasKeyword(altac, Keyword.HASTE) shouldBe true
        }

        test("your own creature dying does not trigger it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardOnBattlefield(1, "Altac Bloodseeker")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .build()
            game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.state.projectedState.getPower(game.findPermanent("Altac Bloodseeker")!!) shouldBe 2
        }
    }
}
