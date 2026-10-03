package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fell the Profane // Fell Mire (MH3).
 *
 * Front: "Destroy target creature or planeswalker." Back: "As this land enters, you may pay 3 life.
 * If you don't, it enters tapped. {T}: Add {B}."
 */
class FellTheProfaneScenarioTest : ScenarioTestBase() {

    private fun castGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Fell the Profane")
        .withLandsOnBattlefield(1, "Swamp", 4)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardOnBattlefield(2, "Jace Beleren")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun landGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Fell the Profane")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Fell the Profane — the instant front") {

            test("destroys target creature") {
                val game = castGame()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Fell the Profane", bears).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isOnBattlefield("Jace Beleren") shouldBe true
            }

            test("destroys target planeswalker") {
                val game = castGame()
                val jace = game.findPermanent("Jace Beleren")!!
                game.state = game.state.updateEntity(jace) {
                    it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3))
                }
                game.castSpell(1, "Fell the Profane", jace).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Jace Beleren") shouldBe false
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("can't target a land") {
                val game = castGame()
                val swamp = game.findPermanent("Swamp")!!
                game.castSpell(1, "Fell the Profane", swamp).error shouldNotBe null
            }
        }

        context("Fell Mire — the land back") {

            test("paying 3 life has it enter untapped") {
                val game = landGame()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(true).error shouldBe null

                val land = game.findPermanent("Fell Mire")!!
                game.getLifeTotal(1) shouldBe 17
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe false
            }

            test("declining to pay has it enter tapped") {
                val game = landGame()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(false).error shouldBe null

                val land = game.findPermanent("Fell Mire")!!
                game.getLifeTotal(1) shouldBe 20
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
