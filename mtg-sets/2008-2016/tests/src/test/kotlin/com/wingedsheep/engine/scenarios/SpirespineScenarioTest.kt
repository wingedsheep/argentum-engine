package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Spirespine: "This creature blocks each combat if able. Enchanted creature gets +4/+1 and blocks
 * each combat if able." First card to put `MustBlock` on the creature an Aura enchants.
 */
class SpirespineScenarioTest : ScenarioTestBase() {

    init {
        test("as a creature, Spirespine must block if able") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Spirespine")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            game.declareNoBlockers().error shouldNotBe null
            game.declareBlockers(mapOf("Spirespine" to listOf("Grizzly Bears"))).error shouldBe null
        }

        test("bestowed, the enchanted creature gets +4/+1 and must block if able; others stay free") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withCardInHand(2, "Spirespine")
                .withLandsOnBattlefield(2, "Forest", 5)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                .build()

            val lions = game.findPermanent("Savannah Lions")!!
            game.execute(
                CastSpell(
                    playerId = game.player2Id,
                    cardId = game.findCardsInHand(2, "Spirespine").single(),
                    targets = listOf(ChosenTarget.Permanent(lions)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.BESTOW,
                )
            ).error shouldBe null
            game.resolveStack()

            val spirespine = game.findPermanent("Spirespine")!!
            game.state.getEntity(spirespine)?.get<AttachedToComponent>()?.targetId shouldBe lions
            val projected = game.state.projectedState
            projected.isCreature(spirespine) shouldBe false
            projected.getPower(lions) shouldBe 5 // test Savannah Lions is 1/1
            projected.getToughness(lions) shouldBe 2

            // Player 1's turn: they attack, and only the enchanted Lions are forced to block.
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.state.activePlayerId shouldBe game.player1Id
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            game.declareBlockers(mapOf("Centaur Courser" to listOf("Grizzly Bears"))).error shouldNotBe null
            game.declareBlockers(mapOf("Savannah Lions" to listOf("Grizzly Bears"))).error shouldBe null
        }
    }
}
