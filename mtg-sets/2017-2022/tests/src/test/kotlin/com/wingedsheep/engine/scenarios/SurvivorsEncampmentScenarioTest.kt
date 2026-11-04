package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.SurvivorsEncampment
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe

/**
 * Survivors' Encampment (HOU) — {T}: Add {C}; {T}, Tap an untapped creature you control: one mana of any color.
 */
class SurvivorsEncampmentScenarioTest : ScenarioTestBase() {
    private val colorlessId = SurvivorsEncampment.activatedAbilities[0].id
    private val anyColorId = SurvivorsEncampment.activatedAbilities[1].id

    init {
        test("{T}: adds {C}") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Survivors' Encampment")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val encampment = game.findPermanent("Survivors' Encampment")!!
            game.execute(ActivateAbility(game.player1Id, encampment, colorlessId)).error shouldBe null
            game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.colorless shouldBe 1
        }

        test("{T}, tap an untapped creature: adds one mana of the chosen color") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Survivors' Encampment")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val encampment = game.findPermanent("Survivors' Encampment")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = encampment,
                    abilityId = anyColorId,
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(bears)),
                    manaColorChoice = Color.BLUE
                )
            ).error shouldBe null
            game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.blue shouldBe 1
            game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
            game.state.getEntity(encampment)?.has<TappedComponent>() shouldBe true
        }
    }
}
