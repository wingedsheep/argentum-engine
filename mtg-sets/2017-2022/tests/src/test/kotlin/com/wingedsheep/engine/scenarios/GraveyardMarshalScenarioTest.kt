package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe

/**
 * Graveyard Marshal (M19 #99) — "{2}{B}, Exile a creature card from your graveyard: Create a tapped
 * 2/2 black Zombie creature token."
 */
class GraveyardMarshalScenarioTest : ScenarioTestBase() {
    init {
        test("exiling a creature card from your graveyard makes a tapped 2/2 Zombie") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Graveyard Marshal")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val marshal = game.findPermanent("Graveyard Marshal")!!
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val abilityId = cardRegistry.getCard("Graveyard Marshal")!!.script.activatedAbilities[0].id
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = marshal,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(exiledCards = listOf(bears)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Grizzly Bears") shouldBe true
            val zombie = game.findPermanent("Zombie Token")!!
            game.state.projectedState.getPower(zombie) shouldBe 2
            game.state.projectedState.getToughness(zombie) shouldBe 2
            game.state.getEntity(zombie)!!.has<TappedComponent>() shouldBe true
        }
    }
}
