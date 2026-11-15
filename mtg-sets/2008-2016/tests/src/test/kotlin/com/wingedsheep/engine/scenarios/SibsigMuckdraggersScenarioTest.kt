package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sibsig Muckdraggers (Fate Reforged #83) — {8}{B} Creature — Zombie 3/6.
 *
 * "Delve. When this creature enters, return target creature card from your graveyard to your hand."
 */
class SibsigMuckdraggersScenarioTest : ScenarioTestBase() {
    init {
        test("entering returns target creature card from its controller's graveyard to hand") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Sibsig Muckdraggers")
                .withLandsOnBattlefield(1, "Swamp", 9)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Forest")
                .withCardInGraveyard(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Sibsig Muckdraggers").error shouldBe null
            game.resolveStack()

            val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val legal = decision.legalTargets[0].orEmpty()
            legal shouldContain bears
            legal shouldNotContain game.findCardsInGraveyard(1, "Forest").single()
            legal shouldNotContain game.findCardsInGraveyard(2, "Hill Giant").single()

            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe false
        }
    }
}
