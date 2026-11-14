package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Swift Reckoning (ORI #35).
 *
 * "Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, you may
 * cast this spell as though it had flash. Destroy target tapped creature."
 *
 * The spell-mastery count is read as the cast begins (the card's `conditionalFlash`): with two
 * instant/sorcery cards in its controller's graveyard it is offered on the opponent's turn; with only
 * one — or with non-instant/sorcery cards padding the graveyard — it keeps sorcery timing. Cast from
 * the graveyard, Swift Reckoning doesn't count itself: it needs two *other* instant/sorcery cards there.
 */
class SwiftReckoningScenarioTest : ScenarioTestBase() {

    private fun game(graveyard: List<String>) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Swift Reckoning")
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
        .apply { graveyard.forEach { withCardInGraveyard(1, it) } }
        .withActivePlayer(2)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /**
     * Opponent's turn; Swift Reckoning sits in the graveyard and Mission Briefing (an instant that
     * lands in the graveyard too) gives it "you may cast it this turn".
     */
    private fun graveyardGame(others: List<String>): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Mission Briefing")
            .withLandsOnBattlefield(1, "Island", 2)
            .withLandsOnBattlefield(1, "Plains", 2)
            .withCardInGraveyard(1, "Swift Reckoning")
            .apply { others.forEach { withCardInGraveyard(1, it) } }
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(1, "Plains")
            .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
            .withActivePlayer(2)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Mission Briefing").error shouldBe null
        game.resolveStack()
        (game.getPendingDecision() is SelectCardsDecision) shouldBe true
        game.selectCards(emptyList()).error shouldBe null
        if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()
        if (game.getPendingDecision() is SelectCardsDecision) {
            game.selectCards(game.findCardsInGraveyard(1, "Swift Reckoning")).error shouldBe null
        }
        game.resolveStack()
        game.isInGraveyard(1, "Mission Briefing") shouldBe true
        // The active opponent gets priority after resolution; they pass it back.
        game.state.priorityPlayerId shouldBe game.player2Id
        game.passPriority()
        game.state.priorityPlayerId shouldBe game.player1Id
        return game
    }

    private fun TestGame.reckoningOffer() =
        getLegalActions(1).firstOrNull { it.actionType == "CastSpell" && it.description.contains("Swift Reckoning") }

    init {
        context("Swift Reckoning") {

            test("with spell mastery it is cast on the opponent's turn and destroys the tapped creature") {
                val game = game(listOf("Lightning Bolt", "Divination"))
                game.reckoningOffer().shouldNotBeNull()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                game.castSpell(1, "Swift Reckoning", bears).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }

            test("with only one instant or sorcery in the graveyard it keeps sorcery timing") {
                val game = game(listOf("Lightning Bolt"))
                game.reckoningOffer().shouldBeNull()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                game.castSpell(1, "Swift Reckoning", bears).error shouldNotBe null
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("non-instant, non-sorcery cards do not count toward spell mastery") {
                val game = game(listOf("Lightning Bolt", "Grizzly Bears", "Plains"))
                game.reckoningOffer().shouldBeNull()
            }

            test("cast from the graveyard it does not count itself toward spell mastery") {
                // Graveyard: Swift Reckoning + Mission Briefing — only one *other* instant/sorcery.
                val game = graveyardGame(emptyList())
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                game.castSpellFromGraveyard(1, "Swift Reckoning", bears).error shouldBe
                    "You can only cast sorcery-speed spells during your main phase with an empty stack"
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("cast from the graveyard with two other instants or sorceries there it has flash") {
                val game = graveyardGame(listOf("Lightning Bolt"))
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                game.castSpellFromGraveyard(1, "Swift Reckoning", bears).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }
        }
    }
}
