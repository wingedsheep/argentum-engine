package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.dungeon.DungeonService
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Planar Ally (AFR #31): a 3/3 flier that ventures into the dungeon whenever it attacks.
 */
class PlanarAllyScenarioTest : ScenarioTestBase() {

    init {
        test("attacking ventures into the dungeon of your choice") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Planar Ally", summoningSickness = false)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val ally = game.findPermanent("Planar Ally")!!

            game.state.projectedState.hasKeyword(ally, Keyword.FLYING) shouldBe true
            DungeonService.marker(game.state, game.player1Id) shouldBe null

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Planar Ally" to 2)).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
            game.submitDecision(
                OptionChosenResponse(decision.id, decision.options.indexOf("Lost Mine of Phandelver"))
            ).error shouldBe null
            game.resolveStack()

            DungeonService.marker(game.state, game.player1Id)!!.dungeon shouldBe "Lost Mine of Phandelver"
        }
    }
}
