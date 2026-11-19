package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Reckless Reveler (BNG #106) — "{R}, Sacrifice this creature: Destroy target artifact." */
class RecklessRevelerScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = cardRegistry.getCard("Reckless Reveler")!!.script.activatedAbilities.single().id

        test("{R} and sacrificing it destroys target artifact") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Reckless Reveler")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Ornithopter")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val reveler = game.findPermanent("Reckless Reveler")!!
            val thopter = game.findPermanent("Ornithopter")!!

            game.execute(
                ActivateAbility(game.player1Id, reveler, abilityId, listOf(ChosenTarget.Permanent(thopter)))
            ).error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            withClue("the Reveler is sacrificed as a cost") { game.isInGraveyard(1, "Reckless Reveler") shouldBe true }
            game.resolveStack()

            withClue("Ornithopter is destroyed") { game.isInGraveyard(2, "Ornithopter") shouldBe true }
        }
    }
}
