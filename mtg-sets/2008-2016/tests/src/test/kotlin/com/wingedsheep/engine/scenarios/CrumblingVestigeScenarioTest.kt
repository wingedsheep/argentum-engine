package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Crumbling Vestige — Land. "This land enters tapped. When this land enters, add one mana of any
 * color. {T}: Add {C}."
 */
class CrumblingVestigeScenarioTest : ScenarioTestBase() {
    init {
        test("enters tapped and its enters trigger adds one mana of the chosen color") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Crumbling Vestige")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val vestige = game.findCardsInHand(1, "Crumbling Vestige").single()

            game.execute(PlayLand(game.player1Id, vestige)).error shouldBe null
            withClue("this land enters tapped") {
                game.state.getEntity(vestige)?.has<TappedComponent>() shouldBe true
            }

            game.resolveStack()
            val decision = game.getPendingDecision() as? ChooseColorDecision
                ?: error("expected a color choice, got ${game.getPendingDecision()}")
            game.submitDecision(ColorChosenResponse(decision.id, Color.GREEN)).error shouldBe null
            game.resolveStack()

            val pool = game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!
            pool.green shouldBe 1
            pool.colorless shouldBe 0
        }

        test("{T}: Add {C}") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Crumbling Vestige")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val vestige = game.findPermanent("Crumbling Vestige")!!
            val manaId = cardRegistry.getCard("Crumbling Vestige")!!.script.activatedAbilities.single().id

            game.execute(ActivateAbility(game.player1Id, vestige, manaId)).error shouldBe null
            withClue("a mana ability doesn't use the stack") { game.state.stack.size shouldBe 0 }
            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.colorless shouldBe 1
        }
    }
}
