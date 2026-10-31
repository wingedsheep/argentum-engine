package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Illuna, Apex of Wishes (IKO #190).
 *
 *   Mutate {3}{R/G}{U}{U}
 *   Flying, trample
 *   Whenever this creature mutates, exile cards from the top of your library until you exile a
 *   nonland permanent card. Put that card onto the battlefield or into your hand.
 *
 * Library (top first): Forest, Shock, Ornithopter, Island. The mutate trigger exiles Forest, Shock
 * and Ornithopter; Ornithopter goes to the battlefield or the hand by choice, while the land and
 * the sorcery stay in exile and Island stays in the library.
 */
class IllunaApexOfWishesScenarioTest : ScenarioTestBase() {

    private fun libraryNames(game: TestGame): List<String> =
        game.state.getZone(ZoneKey(game.player1Id, Zone.LIBRARY))
            .map { game.state.getEntity(it)?.get<CardComponent>()?.name ?: "?" }

    private fun setup(library: List<String> = listOf("Forest", "Shock", "Ornithopter", "Island")): TestGame {
        var builder = scenario()
            .withPlayers("P1", "P2")
            .withCardInHand(1, "Illuna, Apex of Wishes")
            .withLandsOnBattlefield(1, "Island", 4)
            .withLandsOnBattlefield(1, "Mountain", 3)
            .withLandsOnBattlefield(1, "Forest", 1)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        for (card in library) builder = builder.withCardInLibrary(1, card)
        repeat(3) { builder = builder.withCardInLibrary(2, "Forest") }
        return builder.build()
    }

    /** Mutate Illuna over the Bears and resolve up to the battlefield-or-hand choice. */
    private fun TestGame.mutateOverBears() {
        val bears = findPermanent("Grizzly Bears")!!
        val offer = getLegalActions(1).single {
            (it.action as? CastSpell)?.alternativeCostType == AlternativeCostType.MUTATE
        }
        execute((offer.action as CastSpell).copy(targets = listOf(ChosenTarget.Permanent(bears)))).error shouldBe null
        resolveStack()
        answerYesNo(true).error shouldBe null // over
        resolveStack()
    }

    init {
        context("Illuna, Apex of Wishes") {

            test("mutating exiles down to a nonland permanent card, which may go onto the battlefield") {
                val game = setup()
                game.mutateOverBears()

                withClue("the controller chooses battlefield or hand after seeing the card") {
                    (game.state.pendingDecision is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Ornithopter") shouldBe true
                withClue("the land and sorcery exiled along the way stay in exile") {
                    game.isInExile(1, "Forest") shouldBe true
                    game.isInExile(1, "Shock") shouldBe true
                }
                withClue("the walk stops at the first nonland permanent card") {
                    libraryNames(game) shouldBe listOf("Island")
                }
            }

            test("declining the battlefield puts the found card into the hand") {
                val game = setup()
                game.mutateOverBears()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Ornithopter") shouldBe true
                game.isOnBattlefield("Ornithopter") shouldBe false
                game.isInExile(1, "Forest") shouldBe true
                game.isInExile(1, "Shock") shouldBe true
            }

            test("with no nonland permanent card, the whole library is exiled and there is no choice") {
                val game = setup(library = listOf("Forest", "Shock"))
                game.mutateOverBears()

                withClue("nothing was found, so no destination choice is offered") {
                    game.hasPendingDecision() shouldBe false
                }
                game.isInExile(1, "Forest") shouldBe true
                game.isInExile(1, "Shock") shouldBe true
                libraryNames(game) shouldBe emptyList()
            }

            test("cast normally it doesn't mutate and exiles nothing") {
                val game = setup()
                game.castSpell(1, "Illuna, Apex of Wishes").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Illuna, Apex of Wishes") shouldBe true
                libraryNames(game) shouldBe listOf("Forest", "Shock", "Ornithopter", "Island")
            }
        }
    }
}
