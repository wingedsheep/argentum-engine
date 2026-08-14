package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Spider-Woman, Stunning Savior (SPM #152) — {1}{W/U} Legendary Creature —
 * Spider Human Hero, 2/2.
 *
 *   Flying
 *   Venom Blast — Artifacts and creatures your opponents control enter tapped.
 *
 * Exercises the global [com.wingedsheep.sdk.scripting.PermanentsEnterTapped] replacement widened
 * from creatures (Authority of the Consuls) to *artifacts and creatures*, gated to permanents an
 * opponent of Spider-Woman's controller controls. Verifies the opponent's creature AND non-creature
 * artifact both enter tapped, while the controller's own creature and artifact enter untapped.
 */
class SpiderWomanStunningSaviorScenarioTest : ScenarioTestBase() {

    init {
        context("Spider-Woman, Stunning Savior") {

            test("is a 2/2 with flying") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Spider-Woman, Stunning Savior")
                    .withActivePlayer(1)
                    .build()

                val spiderWoman = game.findPermanent("Spider-Woman, Stunning Savior")!!
                withClue("2/2 with flying") {
                    game.state.projectedState.getPower(spiderWoman) shouldBe 2
                    game.state.projectedState.getToughness(spiderWoman) shouldBe 2
                    game.state.projectedState.hasKeyword(spiderWoman, Keyword.FLYING) shouldBe true
                }
            }

            test("artifacts and creatures an opponent controls enter tapped") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Spider-Woman, Stunning Savior")
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Feldon's Cane")
                    .withLandsOnBattlefield(2, "Forest", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsCast = game.castSpell(2, "Grizzly Bears")
                withClue("Opponent casting Grizzly Bears should succeed: ${bearsCast.error}") {
                    bearsCast.error shouldBe null
                }
                game.resolveStack()

                val caneCast = game.castSpell(2, "Feldon's Cane")
                withClue("Opponent casting Feldon's Cane should succeed: ${caneCast.error}") {
                    caneCast.error shouldBe null
                }
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                val cane = game.findPermanent("Feldon's Cane")!!
                withClue("Opponent's creature enters tapped") {
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                }
                withClue("Opponent's non-creature artifact enters tapped") {
                    game.state.getEntity(cane)!!.has<TappedComponent>() shouldBe true
                }
            }

            test("your own artifacts and creatures enter untapped") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Spider-Woman, Stunning Savior")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Feldon's Cane")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsCast = game.castSpell(1, "Grizzly Bears")
                withClue("Casting your own Grizzly Bears should succeed: ${bearsCast.error}") {
                    bearsCast.error shouldBe null
                }
                game.resolveStack()

                val caneCast = game.castSpell(1, "Feldon's Cane")
                withClue("Casting your own Feldon's Cane should succeed: ${caneCast.error}") {
                    caneCast.error shouldBe null
                }
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                val cane = game.findPermanent("Feldon's Cane")!!
                withClue("Your own creature enters untapped") {
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
                }
                withClue("Your own artifact enters untapped") {
                    game.state.getEntity(cane)!!.has<TappedComponent>() shouldBe false
                }
            }
        }
    }
}
