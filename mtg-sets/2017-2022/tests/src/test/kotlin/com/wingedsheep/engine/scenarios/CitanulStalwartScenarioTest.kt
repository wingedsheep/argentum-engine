package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe

/**
 * Citanul Stalwart (BRO #175) — {T}, Tap an untapped artifact or creature you control: Add one
 * mana of any color.
 */
class CitanulStalwartScenarioTest : ScenarioTestBase() {

    init {
        test("tapping itself and an untapped artifact adds one mana of the chosen color") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Citanul Stalwart", summoningSickness = false)
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val stalwart = game.findPermanent("Citanul Stalwart")!!
            val thopter = game.findPermanent("Ornithopter")!!
            val abilityId = cardRegistry.getCard("Citanul Stalwart")!!.script.activatedAbilities[0].id

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = stalwart,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(tappedPermanents = listOf(thopter)),
                    manaColorChoice = Color.RED,
                )
            ).error shouldBe null

            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.getAmount(Color.RED) shouldBe 1
            game.state.getEntity(stalwart)!!.has<TappedComponent>() shouldBe true
            game.state.getEntity(thopter)!!.has<TappedComponent>() shouldBe true
        }
    }
}
