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
 * Eidolon of Countless Battles: "This creature and enchanted creature each get +1/+1 for each
 * creature you control and +1/+1 for each Aura you control." Covers both halves of the bonus, the
 * bestowed Eidolon counting as an Aura (not a creature), and opponents' creatures not counting.
 */
class EidolonOfCountlessBattlesScenarioTest : ScenarioTestBase() {

    init {
        test("as a creature, it counts itself and your other creatures, not the opponent's") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Eidolon of Countless Battles")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val eidolon = game.findPermanent("Eidolon of Countless Battles")!!
            val projected = game.state.projectedState
            projected.getPower(eidolon) shouldBe 2
            projected.getToughness(eidolon) shouldBe 2
        }

        test("bestowed, the enchanted creature gets +1/+1 per creature and per Aura you control") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withCardInHand(1, "Eidolon of Countless Battles")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val lions = game.findPermanent("Savannah Lions")!!
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Eidolon of Countless Battles").single(),
                    targets = listOf(ChosenTarget.Permanent(lions)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.BESTOW,
                )
            ).error shouldBe null
            game.resolveStack()

            val eidolon = game.findPermanent("Eidolon of Countless Battles")!!
            game.state.getEntity(eidolon)?.get<AttachedToComponent>()?.targetId shouldBe lions
            val projected = game.state.projectedState
            projected.isCreature(eidolon) shouldBe false
            // Two creatures (Lions, Bears) + one Aura (the bestowed Eidolon) = +3/+3 on a 1/1.
            projected.getPower(lions) shouldBe 4
            projected.getToughness(lions) shouldBe 4
            projected.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 2
        }
    }
}
