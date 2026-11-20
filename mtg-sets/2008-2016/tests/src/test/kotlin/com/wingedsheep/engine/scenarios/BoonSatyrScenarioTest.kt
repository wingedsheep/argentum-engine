package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Boon Satyr: "Flash. Bestow {3}{G}{G}. Enchanted creature gets +4/+2."
 * The first card pairing flash with bestow — flash applies to the bestow cast too, so it can be
 * bestowed at instant speed on the opponent's turn.
 */
class BoonSatyrScenarioTest : ScenarioTestBase() {

    init {
        test("bestowed at instant speed on the opponent's turn, the host gets +4/+2") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Boon Satyr")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.BEGINNING, Step.UPKEEP)
                .build()

            game.getLegalActions(1).any {
                it.actionType == "CastWithAlternativeCost" && it.description.contains("Bestow Boon Satyr")
            } shouldBe true

            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Boon Satyr").single(),
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.BESTOW,
                )
            ).error shouldBe null
            game.resolveStack()

            val satyr = game.findPermanent("Boon Satyr")!!
            game.state.getEntity(satyr)?.get<AttachedToComponent>()?.targetId shouldBe bears
            val projected = game.state.projectedState
            projected.isCreature(satyr) shouldBe false
            projected.getPower(bears) shouldBe 6
            projected.getToughness(bears) shouldBe 4
        }

        test("cast normally with flash, it is a 4/2 creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Boon Satyr")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.ENDING, Step.END)
                .build()

            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Boon Satyr").single(),
                )
            ).error shouldBe null
            game.resolveStack()

            val satyr = game.findPermanent("Boon Satyr")!!
            val projected = game.state.projectedState
            projected.isCreature(satyr) shouldBe true
            projected.getPower(satyr) shouldBe 4
            projected.getToughness(satyr) shouldBe 2
        }
    }
}
