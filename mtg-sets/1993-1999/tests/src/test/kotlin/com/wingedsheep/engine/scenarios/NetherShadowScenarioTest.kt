package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe

class NetherShadowScenarioTest : ScenarioTestBase() {
    init {
        fun board(above: Int = 3, below: Int = 0, lands: Int = 0) = scenario().withPlayers("Owner", "Opponent").apply {
            repeat(below) { withCardInGraveyard(1, "Grizzly Bears") }
            withCardInGraveyard(1, "Nether Shadow")
            repeat(above) { withCardInGraveyard(1, "Grizzly Bears") }
            repeat(lands) { withCardInGraveyard(1, "Swamp") }
        }.withCardInLibrary(1, "Swamp").withCardInLibrary(2, "Forest")
            .withActivePlayer(2).withPriorityPlayer(2).inPhase(Phase.ENDING, Step.END).build()
        fun upkeep(game: TestGame) {
            game.passPriority().error shouldBe null
            game.passPriority().error shouldBe null
            game.state.activePlayerId shouldBe game.player1Id
            game.state.step shouldBe Step.UPKEEP
        }
        test("three creatures above allow optional return with haste") {
            val game = board(); upkeep(game)
            game.state.stack.size shouldBe 1
            game.resolveStack()
            (game.state.pendingDecision as YesNoDecision).playerId shouldBe game.player1Id
            game.answerYesNo(true).error shouldBe null
            game.isOnBattlefield("Nether Shadow") shouldBe true
            game.isInGraveyard(1, "Nether Shadow") shouldBe false
            game.state.projectedState.hasKeyword(game.findPermanent("Nether Shadow")!!, Keyword.HASTE) shouldBe true
            game.state.stack.size shouldBe 0
        }
        test("return can be declined") {
            val game = board(); upkeep(game); game.resolveStack()
            game.answerYesNo(false).error shouldBe null
            game.isInGraveyard(1, "Nether Shadow") shouldBe true
        }
        test("creatures below and noncreatures above do not meet the threshold") {
            val game = board(above = 2, below = 3, lands = 5)
            // Two creatures above are insufficient regardless of cards below.
            upkeep(game)
            game.state.stack.size shouldBe 0
            game.isInGraveyard(1, "Nether Shadow") shouldBe true
        }
        test("condition is rechecked when a creature above leaves in response") {
            val game = board(); upkeep(game)
            val bear = game.findCardsInGraveyard(1, "Grizzly Bears").first()
            game.state = zones.moveToZone(game.state, bear, Zone.EXILE).state
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            game.isInGraveyard(1, "Nether Shadow") shouldBe true
        }
        test("source leaving and re-entering cannot be returned by its old ability") {
            val game = board(); upkeep(game)
            val shadow = game.findCardsInGraveyard(1, "Nether Shadow").single()
            game.state = zones.moveToZone(game.state, shadow, Zone.EXILE).state
            game.state = zones.moveToZone(game.state, shadow, Zone.GRAVEYARD).state
            // Put three more creatures above the new object, making the position alone legal.
            for (bear in game.findCardsInGraveyard(1, "Grizzly Bears")) {
                game.state = zones.moveToZone(game.state, bear, Zone.EXILE).state
                game.state = zones.moveToZone(game.state, bear, Zone.GRAVEYARD).state
            }
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            game.isInGraveyard(1, "Nether Shadow") shouldBe true
        }
        test("battlefield Shadow does not trigger") {
            val game = scenario().withPlayers("Owner", "Opponent").withCardOnBattlefield(1, "Nether Shadow")
                .withCardInGraveyard(1, "Grizzly Bears").withCardInGraveyard(1, "Grizzly Bears").withCardInGraveyard(1, "Grizzly Bears")
                .withCardInLibrary(1, "Swamp").withCardInLibrary(2, "Forest")
                .withActivePlayer(2).withPriorityPlayer(2).inPhase(Phase.ENDING, Step.END).build()
            upkeep(game); game.state.stack.size shouldBe 0
        }
        test("opponents upkeep does not trigger a graveyard Shadow") {
            val game = board()
            game.state = game.state.copy(activePlayerId = game.player1Id, priorityPlayerId = game.player1Id)
            game.passPriority().error shouldBe null
            game.passPriority().error shouldBe null
            game.state.activePlayerId shouldBe game.player2Id
            game.state.step shouldBe Step.UPKEEP
            game.state.stack.size shouldBe 0
        }
        test("simultaneous destruction is owner-ordered before Wrath reaches the graveyard") {
            val game = scenario().withPlayers("Owner", "Opponent")
                .withCardInHand(1, "Wrath of God").withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(1, "Nether Shadow").withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Llanowar Elves").withCardOnBattlefield(1, "Hill Giant")
                .withCardInLibrary(1, "Plains").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).withPriorityPlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.state = game.state.copy(preserveGraveyardOrder = true)
            val shadow = game.findPermanent("Nether Shadow")!!
            game.castSpell(1, "Wrath of God").error shouldBe null
            game.resolveStack()
            val question = game.state.pendingDecision as OrderObjectsDecision
            val topFirst = question.objects.filter { it != shadow } + shadow
            game.submitDecision(OrderedResponse(question.id, topFirst)).error shouldBe null
            game.state.getGraveyard(game.player1Id).first() shouldBe shadow
            game.findCardsInGraveyard(1, "Wrath of God").single() shouldBe game.state.getGraveyard(game.player1Id).last()
        }
        test("four Shadows return only the one that met the condition at upkeep") {
            val game = scenario().withPlayers("Owner", "Opponent")
                .withCardInGraveyard(1, "Nether Shadow").withCardInGraveyard(1, "Nether Shadow")
                .withCardInGraveyard(1, "Nether Shadow").withCardInGraveyard(1, "Nether Shadow")
                .withCardInLibrary(1, "Swamp").withCardInLibrary(2, "Forest")
                .withActivePlayer(2).withPriorityPlayer(2).inPhase(Phase.ENDING, Step.END).build()
            upkeep(game)
            game.state.stack.size shouldBe 1
            game.resolveStack()
            game.answerYesNo(true).error shouldBe null
            game.findCardsInGraveyard(1, "Nether Shadow").size shouldBe 3
            game.state.stack.size shouldBe 0
            game.state.pendingDecision shouldBe null
        }
    }
}
