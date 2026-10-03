package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Boggart Trawler // Boggart Bog (MH3).
 *
 * Front: "When this creature enters, exile target player's graveyard." Back: "As this land enters,
 * you may pay 3 life. If you don't, it enters tapped. {T}: Add {B}."
 */
class BoggartTrawlerScenarioTest : ScenarioTestBase() {

    private fun landGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Boggart Trawler")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Boggart Trawler — enters trigger") {

            test("exiles only the targeted player's graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Boggart Trawler")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInGraveyard(2, "Lightning Bolt")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Boggart Trawler").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val decision = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision; got ${game.state.pendingDecision}")
                game.submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(game.player2Id))))
                    .error shouldBe null
                game.resolveStack()

                withClue("opponent's graveyard is exiled") { game.graveyardSize(2) shouldBe 0 }
                withClue("our own graveyard is untouched") { game.graveyardSize(1) shouldBe 1 }
                game.state.getExile(game.player2Id).size shouldBe 2
                (game.findPermanent("Boggart Trawler") != null) shouldBe true
            }
        }

        context("Boggart Bog — the land back") {

            test("paying 3 life has it enter untapped") {
                val game = landGame()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(true).error shouldBe null

                val land = game.findPermanent("Boggart Bog")!!
                game.getLifeTotal(1) shouldBe 17
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe false
            }

            test("declining to pay has it enter tapped") {
                val game = landGame()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(false).error shouldBe null

                val land = game.findPermanent("Boggart Bog")!!
                game.getLifeTotal(1) shouldBe 20
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
