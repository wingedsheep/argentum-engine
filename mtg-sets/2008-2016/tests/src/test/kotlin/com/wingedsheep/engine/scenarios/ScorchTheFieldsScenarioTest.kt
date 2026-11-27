package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class ScorchTheFieldsScenarioTest : ScenarioTestBase() {
    init {
        test("destroys target land and deals 1 damage to each Human creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Scorch the Fields")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withCardOnBattlefield(1, "Elite Vanguard")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val plains = game.findPermanent("Plains")!!

            val result = game.castSpell(1, "Scorch the Fields", plains)
            withClue("${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isInGraveyard(2, "Plains") shouldBe true
            // Each Human — including the caster's own Elite Vanguard (Human Soldier, 2/1) — takes 1;
            // Savannah Lions is a Cat, Grizzly Bears a Bear, so neither is hit.
            game.isInGraveyard(1, "Elite Vanguard") shouldBe true
            game.isOnBattlefield("Savannah Lions") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
