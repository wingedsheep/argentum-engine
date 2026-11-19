package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Ghostblade Eidolon: "Double strike. Enchanted creature gets +1/+1 and has double strike."
 * Bestowed, the host gets the bonus and the keyword; on its own, the Eidolon is a 1/1 double striker.
 */
class GhostbladeEidolonScenarioTest : ScenarioTestBase() {

    init {
        test("as a creature it is a 1/1 with double strike") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Ghostblade Eidolon")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val eidolon = game.findPermanent("Ghostblade Eidolon")!!
            val projected = game.state.projectedState
            projected.getPower(eidolon) shouldBe 1
            projected.getToughness(eidolon) shouldBe 1
            projected.hasKeyword(eidolon, Keyword.DOUBLE_STRIKE) shouldBe true
        }

        test("bestowed, the host gets +1/+1 and double strike") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Ghostblade Eidolon")
                .withLandsOnBattlefield(1, "Plains", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Ghostblade Eidolon").single(),
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.BESTOW,
                )
            ).error shouldBe null
            game.resolveStack()

            val eidolon = game.findPermanent("Ghostblade Eidolon")!!
            game.state.getEntity(eidolon)?.get<AttachedToComponent>()?.targetId shouldBe bears
            val projected = game.state.projectedState
            projected.isCreature(eidolon) shouldBe false
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
            projected.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe true
        }
    }
}
