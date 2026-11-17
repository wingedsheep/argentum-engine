package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Obelisk of Urd: as it enters, choose a creature type; creatures you control of that type get +2/+2.
 */
class ObeliskOfUrdScenarioTest : ScenarioTestBase() {
    private fun TestGame.chooseType(type: String) {
        val obelisk = findPermanent("Obelisk of Urd")!!
        state = state.updateEntity(obelisk) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice(type))))
        }
    }

    init {
        test("only your creatures of the chosen type get +2/+2") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardOnBattlefield(1, "Obelisk of Urd")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
            game.chooseType("Bear")
            val projected = game.state.projectedState
            val (mine, theirs) = game.findPermanents("Grizzly Bears").partition {
                game.state.getEntity(it)?.get<ControllerComponent>()?.playerId == game.player1Id
            }
            projected.getPower(mine.single()) shouldBe 4
            projected.getToughness(mine.single()) shouldBe 4
            withClue("another type and the opponent's Bear are unchanged") {
                projected.getPower(game.findPermanent("Hill Giant")!!) shouldBe 3
                projected.getPower(theirs.single()) shouldBe 2
            }
        }

        test("convoke: tapping creatures helps pay for it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withCardInHand(1, "Obelisk of Urd")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .build()
            withClue("four lands plus two convoking creatures can pay {6}") {
                game.getLegalActions(1).any { it.action is com.wingedsheep.engine.core.CastSpell && it.isAffordable } shouldBe true
            }
        }
    }
}
