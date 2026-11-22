package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Agoraphobia (GTC) — enchanted creature gets -5/-0; {2}{U}: return this Aura to its owner's hand.
 */
class AgoraphobiaScenarioTest : ScenarioTestBase() {
    init {
        test("shrinks the enchanted creature's power, and returns to hand for {2}{U}") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardAttachedTo(1, "Agoraphobia", "Craw Wurm")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val wurm = game.findPermanent("Craw Wurm")!!
            game.state.projectedState.getPower(wurm) shouldBe 1
            game.state.projectedState.getToughness(wurm) shouldBe 4

            val aura = game.findPermanent("Agoraphobia")!!
            val abilityId = cardRegistry.getCard("Agoraphobia")!!.script.activatedAbilities.single().id
            val result = game.execute(ActivateAbility(game.player1Id, aura, abilityId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isInHand(1, "Agoraphobia") shouldBe true
            game.state.projectedState.getPower(wurm) shouldBe 6
        }
    }
}
