package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.ExertedComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.StewardOfSolidarity
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Steward of Solidarity (HOU) — {T}, Exert: create a 1/1 white Warrior token with vigilance.
 */
class StewardOfSolidarityScenarioTest : ScenarioTestBase() {
    init {
        test("{T}, Exert: creates a 1/1 vigilant Warrior and exerts the Steward") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Steward of Solidarity")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val steward = game.findPermanent("Steward of Solidarity")!!

            game.execute(
                ActivateAbility(game.player1Id, steward, StewardOfSolidarity.activatedAbilities.single().id)
            ).error shouldBe null
            game.resolveStack()

            val token = game.findAllPermanents("Warrior Token").single()
            val projected = game.state.projectedState
            projected.getPower(token) shouldBe 1
            projected.getToughness(token) shouldBe 1
            projected.hasKeyword(token, Keyword.VIGILANCE) shouldBe true
            game.state.getEntity(steward)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(steward)?.has<ExertedComponent>() shouldBe true
        }
    }
}
