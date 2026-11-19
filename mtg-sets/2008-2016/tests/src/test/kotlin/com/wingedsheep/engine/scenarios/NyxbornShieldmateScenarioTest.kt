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
 * Nyxborn Shieldmate: "Bestow {2}{W}. Enchanted creature gets +1/+2."
 * Bestowed, the host gets the bonus; on its own, it is a 1/2 creature.
 */
class NyxbornShieldmateScenarioTest : ScenarioTestBase() {

    init {
        test("as a creature it is a 1/2") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Nyxborn Shieldmate")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val card = game.findPermanent("Nyxborn Shieldmate")!!
            val projected = game.state.projectedState
            projected.getPower(card) shouldBe 1
            projected.getToughness(card) shouldBe 2
        }

        test("bestowed, the host gets +1/+2") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Nyxborn Shieldmate")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Nyxborn Shieldmate").single(),
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.BESTOW,
                )
            ).error shouldBe null
            game.resolveStack()

            val card = game.findPermanent("Nyxborn Shieldmate")!!
            game.state.getEntity(card)?.get<AttachedToComponent>()?.targetId shouldBe bears
            val projected = game.state.projectedState
            projected.isCreature(card) shouldBe false
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 4
        }
    }
}
