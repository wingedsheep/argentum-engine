package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ecological Appreciation (STX #128) — {X}{2}{G} Sorcery.
 *
 * "Search your library and graveyard for up to four creature cards with different names that
 *  each have mana value X or less and reveal them. An opponent chooses two of those cards.
 *  Shuffle the chosen cards into your library and put the rest onto the battlefield. Exile
 *  Ecological Appreciation."
 *
 * Covers the two-zone search held to mana value X, the opponent (not the caster) making the
 * split, the chosen cards going to the library and the rest to the battlefield, the spell
 * exiling itself, and the ruling that finding two or fewer puts nothing onto the battlefield.
 */
class EcologicalAppreciationScenarioTest : ScenarioTestBase() {

    init {
        test("an opponent sends two of four found creatures to the library; the rest enter the battlefield") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Ecological Appreciation")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Centaur Courser")
                .withCardInLibrary(1, "Force of Nature")
                .withCardInGraveyard(1, "Savannah Lions")
                .withCardInGraveyard(1, "Llanowar Elves")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            val courser = game.findCardsInLibrary(1, "Centaur Courser").single()
            val force = game.findCardsInLibrary(1, "Force of Nature").single()
            val lions = game.findCardsInGraveyard(1, "Savannah Lions").single()
            val elves = game.findCardsInGraveyard(1, "Llanowar Elves").single()

            game.castXSpell(1, "Ecological Appreciation", xValue = 3).error shouldBe null
            game.resolveStack()

            withClue("the caster searches") {
                game.getPendingDecision()?.playerId shouldBe game.player1Id
            }
            game.selectCards(listOf(bears, courser, lions, elves)).error shouldBe null

            withClue("the opponent makes the split") {
                game.getPendingDecision()?.playerId shouldBe game.player2Id
            }
            game.selectCards(listOf(bears, lions)).error shouldBe null

            withClue("the chosen cards are shuffled into the caster's library") {
                (bears in game.state.getLibrary(game.player1Id)) shouldBe true
                (lions in game.state.getLibrary(game.player1Id)) shouldBe true
            }
            withClue("the rest enter the battlefield") {
                game.isOnBattlefield("Centaur Courser") shouldBe true
                game.isOnBattlefield("Llanowar Elves") shouldBe true
                game.state.projectedState.getController(courser) shouldBe game.player1Id
                game.state.projectedState.getController(elves) shouldBe game.player1Id
            }
            withClue("Force of Nature (mana value 6) is above X = 3 and stays in the library") {
                (force in game.state.getLibrary(game.player1Id)) shouldBe true
            }
            withClue("Ecological Appreciation exiles itself") {
                game.isInExile(1, "Ecological Appreciation") shouldBe true
                game.isInGraveyard(1, "Ecological Appreciation") shouldBe false
            }
        }

        test("finding only two creatures puts none onto the battlefield") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Ecological Appreciation")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Savannah Lions")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            val lions = game.findCardsInGraveyard(1, "Savannah Lions").single()

            game.castXSpell(1, "Ecological Appreciation", xValue = 2).error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(bears, lions)).error shouldBe null

            withClue("both found cards go to the library (2021-04-16 ruling)") {
                (bears in game.state.getLibrary(game.player1Id)) shouldBe true
                (lions in game.state.getLibrary(game.player1Id)) shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isOnBattlefield("Savannah Lions") shouldBe false
            }
        }
    }
}
