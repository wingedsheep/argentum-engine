package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.dungeon.DungeonService
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Veteran Dungeoneer (AFR #40): entering ventures into the dungeon.
 */
class VeteranDungeoneerScenarioTest : ScenarioTestBase() {

    init {
        test("entering ventures into the dungeon of your choice") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Veteran Dungeoneer")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Veteran Dungeoneer").error shouldBe null
            game.resolveStack()
            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
            game.submitDecision(
                OptionChosenResponse(decision.id, decision.options.indexOf("Dungeon of the Mad Mage"))
            ).error shouldBe null
            game.resolveStack()

            DungeonService.marker(game.state, game.player1Id)!!.dungeon shouldBe "Dungeon of the Mad Mage"
        }
    }
}
