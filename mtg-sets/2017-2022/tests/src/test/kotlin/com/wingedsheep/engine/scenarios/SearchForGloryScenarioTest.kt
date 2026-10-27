package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Search for Glory (KHM #27).
 *
 *   Search your library for a snow permanent card, a legendary card, or a Saga card, reveal it,
 *   put it into your hand, then shuffle. You gain 1 life for each {S} spent to cast this spell.
 *
 * The search offers snow *permanent* cards (not a snow sorcery), legendary cards and Sagas; the
 * life gain counts the mana from snow sources spent on the cast, read when the spell resolves.
 */
class SearchForGloryScenarioTest : ScenarioTestBase() {

    private fun gloryBoard(vararg lands: String): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Search for Glory")
            .withCardInLibrary(1, "Hailstorm Valkyrie")
            .withCardInLibrary(1, "Sigrid, God-Favored")
            .withCardInLibrary(1, "Binding the Old Gods")
            .withCardInLibrary(1, "Search for Glory")
            .withCardInLibrary(1, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        lands.forEach { builder.withLandsOnBattlefield(1, it, 1) }
        return builder.build()
    }

    private fun TestGame.castAndFind(cardName: String) {
        castSpell(1, "Search for Glory").error shouldBe null
        resolveStack()
        val decision = getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
        withClue("snow permanent, legendary and Saga cards only — not the snow sorcery or a plain creature") {
            decision.options shouldContainExactlyInAnyOrder listOf(
                findCardsInLibrary(1, "Hailstorm Valkyrie").single(),
                findCardsInLibrary(1, "Sigrid, God-Favored").single(),
                findCardsInLibrary(1, "Binding the Old Gods").single(),
            )
        }
        selectCards(listOf(findCardsInLibrary(1, cardName).single())).error shouldBe null
        isInHand(1, cardName) shouldBe true
    }

    init {
        context("Search for Glory") {

            test("each snow mana spent gains 1 life") {
                val game = gloryBoard("Snow-Covered Plains", "Snow-Covered Plains", "Plains")
                game.castAndFind("Binding the Old Gods")
                withClue("two of the three mana came from snow sources") { game.getLifeTotal(1) shouldBe 22 }
            }

            test("no snow mana spent still searches but gains no life") {
                val game = gloryBoard("Plains", "Plains", "Plains")
                game.castAndFind("Sigrid, God-Favored")
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
