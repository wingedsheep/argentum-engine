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
 * Spiteful Returned: "Whenever this creature or enchanted creature attacks, defending player loses
 * 2 life. Enchanted creature gets +1/+1." The attack trigger fires off its own body as a creature
 * and off its host once bestowed — exactly once per attack either way.
 */
class SpitefulReturnedScenarioTest : ScenarioTestBase() {

    init {
        test("as a creature, attacking makes the defending player lose 2 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Spiteful Returned", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Spiteful Returned" to 2)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 20
        }

        test("bestowed, the host gets +1/+1 and its attack makes the defending player lose 2 life once") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardInHand(1, "Spiteful Returned")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Spiteful Returned").single(),
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.BESTOW,
                )
            ).error shouldBe null
            game.resolveStack()

            val spiteful = game.findPermanent("Spiteful Returned")!!
            game.state.getEntity(spiteful)?.get<AttachedToComponent>()?.targetId shouldBe bears
            val projected = game.state.projectedState
            projected.isCreature(spiteful) shouldBe false
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
