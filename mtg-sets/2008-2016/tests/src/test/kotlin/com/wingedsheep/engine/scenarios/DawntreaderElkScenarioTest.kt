package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dka.cards.DawntreaderElk
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DawntreaderElkScenarioTest : ScenarioTestBase() {
    init {
        test("{G}, sacrifice: fetches a basic land onto the battlefield tapped") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Dawntreader Elk")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val elk = game.findPermanent("Dawntreader Elk")!!

            val result = game.execute(
                ActivateAbility(game.player1Id, elk, DawntreaderElk.activatedAbilities.single().id)
            )
            withClue("${result.error}") { result.error shouldBe null }
            game.isInGraveyard(1, "Dawntreader Elk") shouldBe true
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(game.findCardsInLibrary(1, "Island"))
            game.resolveStack()

            val island = game.findPermanent("Island")!!
            game.state.getEntity(island)!!.has<TappedComponent>() shouldBe true
            game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
        }
    }
}
