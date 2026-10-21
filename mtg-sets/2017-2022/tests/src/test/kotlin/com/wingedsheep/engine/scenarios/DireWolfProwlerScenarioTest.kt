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
 * Dire Wolf Prowler (AFR #179) — "{1}{G}: This creature gets +2/+2 and gains haste until end of
 * turn. Activate only once each turn."
 */
class DireWolfProwlerScenarioTest : ScenarioTestBase() {

    init {
        test("pumps itself with haste, and only once each turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Dire Wolf Prowler", summoningSickness = true)
                .withLandsOnBattlefield(1, "Forest", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wolf = game.findPermanent("Dire Wolf Prowler")!!
            val abilityId = cardRegistry.getCard("Dire Wolf Prowler")!!.activatedAbilities.single().id

            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = wolf, abilityId = abilityId))
                .error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(wolf) shouldBe 4
            projected.getToughness(wolf) shouldBe 4
            projected.hasKeyword(wolf, Keyword.HASTE) shouldBe true

            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = wolf, abilityId = abilityId))
                .error shouldNotBe null
        }
    }
}
