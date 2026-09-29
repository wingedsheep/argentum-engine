package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DepthDefilerScenarioTest : ScenarioTestBase() {
    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Depth Defiler")
        .withCardInHand(1, "Counterspell")
        .withCardInHand(1, "Unsummon")
        .withLandsOnBattlefield(1, "Island", 7)
        .withLandsOnBattlefield(1, "Snow-Covered Wastes", 1)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

    private fun TestGame.cast(kicked: Boolean) {
        execute(CastSpell(player1Id, findCardsInHand(1, "Depth Defiler").single(),
            declaredCostSlot = if (kicked) ChoiceSlot.KICKED else null)).error shouldBe null
    }
    private fun TestGame.mode(text: String) {
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOfFirst { it.contains(text) }
        check(index >= 0) { decision.toString() }
        submitDecision(OptionChosenResponse(decision.id, index)).error shouldBe null
    }
    init {
        test("unkicked cast chooses exactly one mode before resolution") {
            val game = board()
            game.cast(false)
            game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>().options.size shouldBe 2
            game.mode("Return target creature")
            game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            game.state.stack.mapNotNull { game.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>() }
                .single().chosenModes shouldBe listOf(0)
            game.resolveStack()
            game.isInHand(2, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Depth Defiler") shouldBe true
            game.findCardsInHand(1, "Forest").size shouldBe 0
        }
        test("kicked cast requires both modes and retains them when its creature spell is countered") {
            val game = board()
            game.cast(true)
            game.mode("Return target creature")
            game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>().options.size shouldBe 1
            game.mode("draws two cards")
            game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            game.selectTargets(listOf(game.player1Id)).error shouldBe null
            game.state.stack.mapNotNull { game.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>() }
                .single().chosenModes shouldBe listOf(0, 1)
            game.castSpellTargetingStackSpell(1, "Counterspell", "Depth Defiler").error shouldBe null
            game.resolveStack()
            game.isInHand(2, "Grizzly Bears") shouldBe true
            game.findCardsInHand(1, "Forest").size shouldBe 2
            game.selectCards(game.findCardsInHand(1, "Forest").take(1)).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Depth Defiler") shouldBe true
            game.findCardsInHand(1, "Forest").size shouldBe 1
        }
        test("an illegal creature target leaves the player mode resolving") {
            val game = board()
            game.cast(true)
            game.mode("Return target creature")
            game.mode("draws two cards")
            val bear = game.findPermanent("Grizzly Bears")!!
            game.selectTargets(listOf(bear)).error shouldBe null
            game.selectTargets(listOf(game.player1Id)).error shouldBe null
            game.castSpell(1, "Unsummon", bear).error shouldBe null
            game.resolveStack()
            game.findCardsInHand(1, "Forest").size shouldBe 2
            game.selectCards(game.findCardsInHand(1, "Forest").take(1)).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Depth Defiler") shouldBe true
            game.isInHand(2, "Grizzly Bears") shouldBe true
        }
        test("choosing the player mode first still resolves the creature mode before drawing and discarding") {
            val game = board()
            game.cast(true)
            game.mode("draws two cards")
            game.mode("Return target creature")
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            game.resolveStack()
            game.isInHand(2, "Grizzly Bears") shouldBe true
            game.selectCards(game.findCardsInHand(2, "Grizzly Bears")).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
        }
    }
}
