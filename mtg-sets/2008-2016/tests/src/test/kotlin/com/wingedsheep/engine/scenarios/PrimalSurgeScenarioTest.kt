package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Primal Surge (AVR #189) — {8}{G}{G} Sorcery.
 *
 *   Exile the top card of your library. If it's a permanent card, you may put it onto the
 *   battlefield. If you do, repeat this process.
 *
 * The loop continues only while the exiled card actually entered: a nonpermanent card, a decline,
 * or an Aura with nothing to enchant (it stays in exile, CR 303.4g) ends the spell.
 */
class PrimalSurgeScenarioTest : ScenarioTestBase() {

    /**
     * Answer each put-onto-the-battlefield prompt: accept while [accept] says so (choosing the one
     * offered card), otherwise decline. Returns how many prompts were seen.
     */
    private fun driveSurge(game: TestGame, accept: (Int) -> Boolean = { true }): Int {
        var prompts = 0
        var guard = 0
        while (game.hasPendingDecision() && guard++ < 40) {
            val decision = game.getPendingDecision()
            if (decision is ChooseTargetsDecision) {
                // An entering Aura's enchant choice — take the first legal host.
                game.selectTargets(listOf(decision.legalTargets[0]!!.first()))
                continue
            }
            if (decision !is SelectCardsDecision) break
            if (accept(prompts) && decision.options.isNotEmpty()) {
                game.selectCards(listOf(decision.options.first()))
            } else {
                game.skipSelection()
            }
            prompts++
        }
        game.resolveStack()
        return prompts
    }

    private fun surgeGame(vararg library: String): TestGame {
        var builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Primal Surge")
            .withLandsOnBattlefield(1, "Forest", 10)
        // First added is the top of the library.
        for (name in library) builder = builder.withCardInLibrary(1, name)
        return builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    init {
        context("Primal Surge") {

            test("keeps putting permanent cards onto the battlefield until it exiles a nonpermanent card") {
                val game = surgeGame("Centaur Courser", "Forest", "Lightning Bolt", "Savannah Lions")

                game.castSpell(1, "Primal Surge").error shouldBe null
                game.resolveStack()
                val prompts = driveSurge(game)

                withClue("one prompt each for the Courser and the Forest; the Bolt is never offered") {
                    prompts shouldBe 2
                }
                withClue("both permanent cards entered") {
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                    game.findPermanents("Forest").size shouldBe 11
                }
                withClue("the Bolt was exiled and ended the process") {
                    game.isInExile(1, "Lightning Bolt") shouldBe true
                }
                withClue("the card under the Bolt was never touched") {
                    game.isOnBattlefield("Savannah Lions") shouldBe false
                    game.isInExile(1, "Savannah Lions") shouldBe false
                    game.librarySize(1) shouldBe 1
                }
                game.isInGraveyard(1, "Primal Surge") shouldBe true
            }

            test("declining a permanent card leaves it in exile and ends the process") {
                val game = surgeGame("Centaur Courser", "Savannah Lions")

                game.castSpell(1, "Primal Surge").error shouldBe null
                game.resolveStack()
                val prompts = driveSurge(game) { false }

                prompts shouldBe 1
                withClue("the declined Courser stays exiled") {
                    game.isInExile(1, "Centaur Courser") shouldBe true
                    game.isOnBattlefield("Centaur Courser") shouldBe false
                }
                withClue("no repeat: the Lions remain in the library") {
                    game.librarySize(1) shouldBe 1
                    game.isInExile(1, "Savannah Lions") shouldBe false
                }
            }

            test("an Aura with nothing to enchant stays in exile and ends the process") {
                val game = surgeGame("Pacifism", "Savannah Lions")

                game.castSpell(1, "Primal Surge").error shouldBe null
                game.resolveStack()
                driveSurge(game)

                withClue("no creature to enchant: Pacifism couldn't enter") {
                    game.isInExile(1, "Pacifism") shouldBe true
                    game.isOnBattlefield("Pacifism") shouldBe false
                }
                withClue("Pacifism wasn't put onto the battlefield, so the process doesn't repeat") {
                    game.librarySize(1) shouldBe 1
                    game.isOnBattlefield("Savannah Lions") shouldBe false
                }
            }

            test("an Aura that enters attached to a creature continues the process") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Primal Surge")
                    .withLandsOnBattlefield(1, "Forest", 10)
                    .withCardOnBattlefield(1, "Centaur Courser")
                    // First added is the top of the library.
                    .withCardInLibrary(1, "Pacifism")
                    .withCardInLibrary(1, "Savannah Lions")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val courser = game.findPermanent("Centaur Courser")!!

                game.castSpell(1, "Primal Surge").error shouldBe null
                game.resolveStack()
                val prompts = driveSurge(game)

                withClue("Pacifism entered attached to the Courser") {
                    val pacifism = game.findPermanent("Pacifism")
                    (pacifism != null) shouldBe true
                    game.state.getEntity(pacifism!!)?.get<AttachedToComponent>()?.targetId shouldBe courser
                }
                withClue("the Aura counted as put onto the battlefield, so the Lions were exiled and put too") {
                    prompts shouldBe 2
                    game.isOnBattlefield("Savannah Lions") shouldBe true
                }
                withClue("the Bolt ended the process") {
                    game.isInExile(1, "Lightning Bolt") shouldBe true
                    game.librarySize(1) shouldBe 0
                }
            }

            test("an empty library ends the process at once") {
                val game = surgeGame()

                game.castSpell(1, "Primal Surge").error shouldBe null
                game.resolveStack()
                driveSurge(game) shouldBe 0

                game.isInGraveyard(1, "Primal Surge") shouldBe true
            }
        }
    }
}
