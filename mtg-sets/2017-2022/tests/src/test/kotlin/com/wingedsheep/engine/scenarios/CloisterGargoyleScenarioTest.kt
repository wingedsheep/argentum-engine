package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.dungeon.DungeonService
import com.wingedsheep.engine.state.components.player.DungeonProgressComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Cloister Gargoyle (AFR #7): entering ventures into the dungeon, and it's a 3/4 flier only while
 * you've completed a dungeon.
 */
class CloisterGargoyleScenarioTest : ScenarioTestBase() {

    init {
        test("entering ventures into the dungeon of your choice") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Cloister Gargoyle")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Cloister Gargoyle").error shouldBe null
            game.resolveStack()
            val decision = game.getPendingDecision() as ChooseOptionDecision
            game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf("Dungeon of the Mad Mage"))).error shouldBe null
            game.resolveStack()

            DungeonService.marker(game.state, game.player1Id)!!.dungeon shouldBe "Dungeon of the Mad Mage"
        }

        test("gets +3/+0 and flying only once you've completed a dungeon") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Cloister Gargoyle")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val gargoyle = game.findPermanent("Cloister Gargoyle")!!

            withClue("No dungeon completed: a plain 0/4") {
                val projected = game.state.projectedState
                projected.getPower(gargoyle) shouldBe 0
                projected.getToughness(gargoyle) shouldBe 4
                projected.hasKeyword(gargoyle, Keyword.FLYING) shouldBe false
            }

            game.state = game.state.updateEntity(game.player1Id) {
                it.with(DungeonProgressComponent(completed = listOf("Lost Mine of Phandelver")))
            }
            val projected = game.state.projectedState
            projected.getPower(gargoyle) shouldBe 3
            projected.getToughness(gargoyle) shouldBe 4
            projected.hasKeyword(gargoyle, Keyword.FLYING) shouldBe true
        }
    }
}
