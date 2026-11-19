package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** Stormcaller of Keranos (BNG #112) — "Haste. {1}{U}: Scry 1." */
class StormcallerOfKeranosScenarioTest : ScenarioTestBase() {
    init {
        val scryId = cardRegistry.getCard("Stormcaller of Keranos")!!.script.activatedAbilities.single().id

        test("{1}{U} scries 1") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Stormcaller of Keranos")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val stormcaller = game.findPermanent("Stormcaller of Keranos")!!
            val top = game.state.getLibrary(game.player1Id).first()

            game.execute(ActivateAbility(game.player1Id, stormcaller, scryId)).error shouldBe null
            if (game.getPendingDecision() !is SelectCardsDecision && game.hasPendingDecision()) {
                game.submitManaSourcesAutoPay()
            }
            game.resolveStack()

            val scry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("scry 1 looks at the top card") { scry.options shouldBe listOf(top) }
            game.selectCards(listOf(top)).error shouldBe null
            game.resolveStack()

            withClue("the scried card went to the bottom") {
                game.state.getLibrary(game.player1Id).last() shouldBe top
            }
        }
    }
}
