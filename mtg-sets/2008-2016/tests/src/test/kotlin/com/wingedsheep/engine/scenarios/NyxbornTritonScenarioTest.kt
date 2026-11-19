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
 * Nyxborn Triton: "Bestow {4}{U}. Enchanted creature gets +2/+3."
 * Bestowed, the host gets the bonus; on its own, it is a 2/3 creature.
 */
class NyxbornTritonScenarioTest : ScenarioTestBase() {

    init {
        test("as a creature it is a 2/3") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Nyxborn Triton")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val card = game.findPermanent("Nyxborn Triton")!!
            val projected = game.state.projectedState
            projected.getPower(card) shouldBe 2
            projected.getToughness(card) shouldBe 3
        }

        test("bestowed, the host gets +2/+3") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Nyxborn Triton")
                .withLandsOnBattlefield(1, "Island", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Nyxborn Triton").single(),
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.BESTOW,
                )
            ).error shouldBe null
            game.resolveStack()

            val card = game.findPermanent("Nyxborn Triton")!!
            game.state.getEntity(card)?.get<AttachedToComponent>()?.targetId shouldBe bears
            val projected = game.state.projectedState
            projected.isCreature(card) shouldBe false
            projected.getPower(bears) shouldBe 4
            projected.getToughness(bears) shouldBe 5
        }
    }
}
