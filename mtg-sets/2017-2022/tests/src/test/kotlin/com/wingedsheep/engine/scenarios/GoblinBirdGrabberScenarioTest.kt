package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Goblin Bird-Grabber (M20 #142) — "{R}: This creature gains flying until end of turn. Activate
 * only if you control a creature with flying."
 */
class GoblinBirdGrabberScenarioTest : ScenarioTestBase() {

    init {
        val abilityId = cardRegistry.getCard("Goblin Bird-Grabber")!!.activatedAbilities.single().id

        test("gains flying while you control a creature with flying") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Goblin Bird-Grabber")
                .withCardOnBattlefield(1, "Ornithopter")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val grabber = game.findPermanent("Goblin Bird-Grabber")!!
            game.state.projectedState.hasKeyword(grabber, Keyword.FLYING) shouldBe false

            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = grabber, abilityId = abilityId))
                .error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.state.projectedState.hasKeyword(grabber, Keyword.FLYING) shouldBe true
        }

        test("can't be activated without a creature with flying") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Goblin Bird-Grabber")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Ornithopter")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val grabber = game.findPermanent("Goblin Bird-Grabber")!!
            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = grabber, abilityId = abilityId))
                .error shouldNotBe null
            game.state.projectedState.hasKeyword(grabber, Keyword.FLYING) shouldBe false
        }
    }
}
