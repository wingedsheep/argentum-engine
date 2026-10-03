package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sink into Stupor // Soporific Springs (MH3).
 *
 * Front: "Return target spell or nonland permanent an opponent controls to its owner's hand."
 * Back: "As this land enters, you may pay 3 life. If you don't, it enters tapped. {T}: Add {U}."
 */
class SinkIntoStuporScenarioTest : ScenarioTestBase() {

    init {
        context("Sink into Stupor — the instant front") {

            test("returns a nonland permanent an opponent controls to its owner's hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sink into Stupor")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Sink into Stupor", bears).error shouldBe null
                game.resolveStack()

                game.isInHand(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }

            test("can't target your own permanent or an opponent's land") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sink into Stupor")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ownBears = game.findPermanent("Grizzly Bears")!!
                val forest = game.findPermanent("Forest")!!
                withClue("own creature is not a legal target") {
                    game.castSpell(1, "Sink into Stupor", ownBears).error shouldNotBe null
                }
                withClue("an opponent's land is not a legal target") {
                    game.castSpell(1, "Sink into Stupor", forest).error shouldNotBe null
                }
            }

            test("returns an opponent's spell from the stack to its owner's hand; it does not resolve") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sink into Stupor")
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(1, "Sink into Stupor", "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.isInHand(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(1, "Sink into Stupor") shouldBe true
            }

            test("can't target your own spell") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sink into Stupor")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.castSpellTargetingStackSpell(1, "Sink into Stupor", "Lightning Bolt").error shouldNotBe null
            }
        }

        context("Soporific Springs — the land back") {

            fun landGame() = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Sink into Stupor")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("paying 3 life has it enter untapped") {
                val game = landGame()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(true).error shouldBe null

                val land = game.findPermanent("Soporific Springs")!!
                game.getLifeTotal(1) shouldBe 17
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe false
            }

            test("declining to pay has it enter tapped") {
                val game = landGame()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null
                game.answerYesNo(false).error shouldBe null

                val land = game.findPermanent("Soporific Springs")!!
                game.getLifeTotal(1) shouldBe 20
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
