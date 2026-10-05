package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Multiple Choice (STX #48) — {X}{U} Sorcery.
 *   If X is 1, scry 1, then draw a card.
 *   If X is 2, you may choose a player. They return a creature they control to its owner's hand.
 *   If X is 3, create a 4/4 blue and red Elemental creature token.
 *   If X is 4 or more, do all of the above.
 */
class MultipleChoiceScenarioTest : ScenarioTestBase() {

    init {
        // Library top → bottom: Mountain, Forest, Swamp.
        fun buildGame() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Multiple Choice")
            .withLandsOnBattlefield(1, "Island", 5)
            .withCardOnBattlefield(1, "Centaur Courser")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.scryTopToBottom() {
            val scry = getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            scry.options.size shouldBe 1
            selectCards(scry.options).error shouldBe null
        }

        fun TestGame.chooseOpponentThenBounceBears() {
            val choose = getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            choose.playerId shouldBe player1Id
            selectTargets(listOf(player2Id)).error shouldBe null
            val pick = getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            // The chosen player picks among the creatures *they* control.
            pick.playerId shouldBe player2Id
            pick.options.toSet() shouldBe setOf(findPermanent("Grizzly Bears")!!, findPermanent("Hill Giant")!!)
            selectCards(listOf(findPermanent("Grizzly Bears")!!)).error shouldBe null
        }

        test("X = 1 scries 1 then draws, and does nothing else") {
            val game = buildGame()
            game.castXSpell(1, "Multiple Choice", 1).error shouldBe null
            game.resolveStack()
            game.scryTopToBottom()
            game.resolveStack()

            game.isInHand(1, "Forest") shouldBe true
            game.isInHand(1, "Mountain") shouldBe false
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.findPermanent("Elemental Token").shouldBeNull()
            game.isInGraveyard(1, "Multiple Choice") shouldBe true
        }

        test("X = 2 lets you choose a player, who returns a creature of their choice") {
            val game = buildGame()
            game.castXSpell(1, "Multiple Choice", 2).error shouldBe null
            game.resolveStack()
            game.chooseOpponentThenBounceBears()
            game.resolveStack()

            game.isInHand(2, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.isOnBattlefield("Centaur Courser") shouldBe true
            game.handSize(1) shouldBe 0
            game.findPermanent("Elemental Token").shouldBeNull()
        }

        test("X = 2 — choosing yourself returns one of your own creatures") {
            val game = buildGame()
            game.castXSpell(1, "Multiple Choice", 2).error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(game.player1Id)).error shouldBe null
            game.resolveStack()

            // Centaur Courser is your only creature, so it returns without a further prompt.
            game.isInHand(1, "Centaur Courser") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
        }

        test("X = 2 — declining to choose a player returns nothing") {
            val game = buildGame()
            game.castXSpell(1, "Multiple Choice", 2).error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.skipTargets().error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeNull()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.isOnBattlefield("Centaur Courser") shouldBe true
        }

        test("X = 3 creates a 4/4 blue and red Elemental token only") {
            val game = buildGame()
            game.castXSpell(1, "Multiple Choice", 3).error shouldBe null
            game.resolveStack()

            val token = game.findPermanent("Elemental Token").shouldNotBeNull()
            val projected = game.state.projectedState
            projected.getPower(token) shouldBe 4
            projected.getToughness(token) shouldBe 4
            projected.hasSubtype(token, "Elemental") shouldBe true
            game.handSize(1) shouldBe 0
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("X = 4 does all of the above") {
            val game = buildGame()
            game.castXSpell(1, "Multiple Choice", 4).error shouldBe null
            game.resolveStack()
            game.scryTopToBottom()
            game.resolveStack()
            game.chooseOpponentThenBounceBears()
            game.resolveStack()

            game.isInHand(1, "Forest") shouldBe true
            game.isInHand(2, "Grizzly Bears") shouldBe true
            game.findPermanent("Elemental Token").shouldNotBeNull()
        }

        test("X = 0 has no effect") {
            val game = buildGame()
            game.castXSpell(1, "Multiple Choice", 0).error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeNull()
            game.handSize(1) shouldBe 0
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.findPermanent("Elemental Token").shouldBeNull()
            game.isInGraveyard(1, "Multiple Choice") shouldBe true
        }
    }
}
