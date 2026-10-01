package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class BeaconOfDestinyScenarioTest : ScenarioTestBase() {
    init {
        test("only the chosen damage source is redirected to Beacon") {
            val game = scenario().withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Beacon of Destiny")
                .withCardOnBattlefield(2, "Mountain").withCardOnBattlefield(2, "Mountain")
                .withCardInHand(2, "Lightning Bolt").withCardInHand(2, "Lightning Bolt")
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withActivePlayer(2).withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            val chosen = game.state.stack.last()
            game.passPriority().error shouldBe null
            game.execute(ActivateAbility(game.player1Id, game.findPermanent("Beacon of Destiny")!!,
                cardRegistry.getCard("Beacon of Destiny")!!.script.activatedAbilities.single().id)).error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(chosen)).error shouldBe null
            game.passPriority().error shouldBe null
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 17
            game.findPermanent("Beacon of Destiny") shouldBe null
        }
    }
}
