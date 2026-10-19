package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Greasefang, Okiba Boss (NEO #220).
 *
 * At the beginning of combat on your turn, return target Vehicle card from your graveyard to the
 * battlefield. It gains haste. Return it to its owner's hand at the beginning of your next end step.
 *
 * Pins the two halves that depend on how the engine runs the script: the targeted Vehicle (and only
 * it) comes back with haste and is bounced at your end step, and the bounce tracks that specific
 * object — a Vehicle that has since gone to the graveyard is a new object and stays there.
 */
class GreasefangOkibaBossScenarioTest : ScenarioTestBase() {

    init {
        context("Greasefang, Okiba Boss") {

            test("returns the targeted Vehicle with haste and bounces it at your end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Greasefang, Okiba Boss", summoningSickness = false)
                    .withCardInGraveyard(1, "Marshals' Pathcruiser")
                    .withCardInGraveyard(1, "High-Speed Hoverbike")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pathcruiser = game.findCardsInGraveyard(1, "Marshals' Pathcruiser").single()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                withClue("the trigger targets a Vehicle card in your graveyard") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                }
                game.selectTargets(listOf(pathcruiser))
                game.resolveStack()

                withClue("only the targeted Vehicle came back") {
                    game.isOnBattlefield("Marshals' Pathcruiser") shouldBe true
                    game.isInGraveyard(1, "High-Speed Hoverbike") shouldBe true
                }
                val returned = game.findPermanent("Marshals' Pathcruiser")!!
                withClue("the returned Vehicle gains haste") {
                    game.state.projectedState.hasKeyword(returned, Keyword.HASTE) shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("it returns to its owner's hand at the beginning of your end step") {
                    game.isOnBattlefield("Marshals' Pathcruiser") shouldBe false
                    game.isInHand(1, "Marshals' Pathcruiser") shouldBe true
                }
            }

            test("a Vehicle that left the battlefield before the end step is not returned to hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Greasefang, Okiba Boss", summoningSickness = false)
                    .withCardInGraveyard(1, "Marshals' Pathcruiser")
                    .withCardInGraveyard(1, "High-Speed Hoverbike")
                    .withCardInHand(1, "Shatter")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pathcruiser = game.findCardsInGraveyard(1, "Marshals' Pathcruiser").single()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.selectTargets(listOf(pathcruiser))
                game.resolveStack()
                game.isOnBattlefield("Marshals' Pathcruiser") shouldBe true

                val returned = game.findPermanent("Marshals' Pathcruiser")!!
                val cast = game.castSpell(1, "Shatter", returned)
                withClue("Shatter should be castable: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                game.isInGraveyard(1, "Marshals' Pathcruiser") shouldBe true

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the card in the graveyard is a new object; the delayed trigger leaves it alone") {
                    game.isInGraveyard(1, "Marshals' Pathcruiser") shouldBe true
                    game.isInHand(1, "Marshals' Pathcruiser") shouldBe false
                }
            }
        }
    }
}
