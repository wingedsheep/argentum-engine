package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Skyclave Aerialist // Skyclave Invader (MOM #78).
 * Transforming into the Invader looks at the top card: a land may go onto the battlefield,
 * otherwise (nonland, or a declined land) it goes to hand.
 */
class SkyclaveAerialistScenarioTest : ScenarioTestBase() {

    private fun transform(game: TestGame) {
        val id = game.findPermanent("Skyclave Aerialist")!!
        val abilityId = cardRegistry.getCard("Skyclave Aerialist")!!.activatedAbilities.first().id
        game.execute(ActivateAbility(playerId = game.player1Id, sourceId = id, abilityId = abilityId))
            .error shouldBe null
        if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
        game.resolveStack()
    }

    init {
        context("Skyclave Aerialist") {
            test("transforms into a 2/4 flyer and puts a nonland top card into hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Skyclave Aerialist")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                transform(game)

                val id = game.findPermanent("Skyclave Invader")!!
                game.state.getEntity(id)!!.get<CardComponent>()!!.name shouldBe "Skyclave Invader"
                game.state.projectedState.getToughness(id) shouldBe 4
                game.isInHand(1, "Grizzly Bears") shouldBe true
            }
        }
    }
}
