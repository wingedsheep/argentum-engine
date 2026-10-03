package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Disciple of Freyalise // Garden of Freyalise (MH3).
 *
 * Front: "When this creature enters, you may sacrifice another creature. If you do, you gain X life
 * and draw X cards, where X is that creature's power." Back: "As this land enters, you may pay 3
 * life. If you don't, it enters tapped. {T}: Add {G}."
 */
class DiscipleOfFreyaliseScenarioTest : ScenarioTestBase() {

    private fun castDisciple() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Disciple of Freyalise")
        .withLandsOnBattlefield(1, "Forest", 6)
        .withCardOnBattlefield(1, "Craw Wurm") // power 6, unlike Disciple's own 3
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun landGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Disciple of Freyalise")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Disciple of Freyalise — enters trigger") {

            test("sacrificing a 6-power creature gains 6 life and draws 6 cards") {
                val game = castDisciple()
                val wurm = game.findPermanent("Craw Wurm")!!
                game.castSpell(1, "Disciple of Freyalise").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("only another creature you control is offered") {
                    decision.options shouldContainExactlyInAnyOrder listOf(wurm)
                }
                game.selectCards(listOf(wurm)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Craw Wurm") shouldBe true
                game.getLifeTotal(1) shouldBe 26
                game.state.getHand(game.player1Id).size shouldBe 6
                game.isOnBattlefield("Disciple of Freyalise") shouldBe true
            }

            test("declining sacrifices nothing, gains no life, draws nothing") {
                val game = castDisciple()
                game.castSpell(1, "Disciple of Freyalise").error shouldBe null
                game.resolveStack()
                game.skipSelection().error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Craw Wurm") shouldBe true
                game.getLifeTotal(1) shouldBe 20
                game.state.getHand(game.player1Id).size shouldBe 0
            }
        }

        context("Garden of Freyalise — the land back") {

            test("paying 3 life has it enter untapped") {
                val game = landGame()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(true).error shouldBe null

                val land = game.findPermanent("Garden of Freyalise")!!
                game.getLifeTotal(1) shouldBe 17
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe false
            }

            test("declining to pay has it enter tapped") {
                val game = landGame()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(false).error shouldBe null

                val land = game.findPermanent("Garden of Freyalise")!!
                game.getLifeTotal(1) shouldBe 20
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
