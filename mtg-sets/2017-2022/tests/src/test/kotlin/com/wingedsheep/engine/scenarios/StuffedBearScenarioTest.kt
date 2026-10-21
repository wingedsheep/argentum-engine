package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Stuffed Bear (MID #259) — {2}: This artifact becomes a 4/4 green Bear artifact creature until
 * end of turn.
 */
class StuffedBearScenarioTest : ScenarioTestBase() {

    init {
        test("activating makes it a 4/4 green Bear artifact creature that reverts at end of turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Stuffed Bear")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bear = game.findPermanent("Stuffed Bear")!!
            game.state.projectedState.isCreature(bear) shouldBe false

            val abilityId = cardRegistry.getCard("Stuffed Bear")!!.script.activatedAbilities[0].id
            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = bear, abilityId = abilityId))
                .error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.isCreature(bear) shouldBe true
            projected.hasType(bear, "ARTIFACT") shouldBe true
            projected.hasSubtype(bear, "Bear") shouldBe true
            projected.hasColor(bear, Color.GREEN) shouldBe true
            projected.getPower(bear) shouldBe 4
            projected.getToughness(bear) shouldBe 4

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

            val after = game.state.projectedState
            after.isCreature(bear) shouldBe false
            after.hasType(bear, "ARTIFACT") shouldBe true
            after.hasColor(bear, Color.GREEN) shouldBe false
        }
    }
}
