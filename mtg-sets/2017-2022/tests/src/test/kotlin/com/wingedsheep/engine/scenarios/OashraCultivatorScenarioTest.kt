package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Oashra Cultivator (AKH #177) — "{2}{G}, {T}, Sacrifice this creature: Search your library for a
 * basic land card, put it onto the battlefield tapped, then shuffle."
 */
class OashraCultivatorScenarioTest : ScenarioTestBase() {
    init {
        test("sacrificing it fetches a basic land onto the battlefield tapped") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Oashra Cultivator")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cultivator = game.findPermanent("Oashra Cultivator")!!
            val abilityId = cardRegistry.getCard("Oashra Cultivator")!!.activatedAbilities.first().id
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = cultivator, abilityId = abilityId)
            ).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()

            withClue("sacrificed as a cost") { game.isInGraveyard(1, "Oashra Cultivator") shouldBe true }
            game.resolveStack()

            val island = game.findCardsInLibrary(1, "Island").single()
            game.selectCards(listOf(island)).error shouldBe null
            game.resolveStack()

            val islandOnField = game.findPermanent("Island")!!
            withClue("the basic land enters tapped") {
                game.state.getEntity(islandOnField)!!
                    .has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe true
            }
            withClue("the nonland stays in the library") {
                game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
            }
        }
    }
}
