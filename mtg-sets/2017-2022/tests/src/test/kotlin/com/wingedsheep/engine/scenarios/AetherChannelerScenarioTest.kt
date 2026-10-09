package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dmu.cards.AetherChanneler
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class AetherChannelerScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + AetherChanneler)
        initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castChanneler(): EntityId {
        giveMana(player1, Color.BLUE, 3)
        val channeler = putCardInHand(player1, "Aether Channeler")
        castSpell(player1, channeler).error shouldBe null
        bothPass().error shouldBe null
        pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        return channeler
    }

    fun GameTestDriver.chooseMode(text: String) {
        val decision = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOfFirst { it.contains(text) }
        (index >= 0) shouldBe true
        submitDecision(player1, OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    test("Bird mode makes exactly one white flying Bird after the announced trigger resolves") {
        val d = driver()
        val channeler = d.castChanneler()
        d.chooseMode("Bird")
        d.getCreatures(d.player1) shouldBe listOf(channeler)
        d.bothPass().error shouldBe null
        val bird = d.getCreatures(d.player1).single { it != channeler }
        d.state.projectedState.getPower(bird) shouldBe 1
        d.state.projectedState.getToughness(bird) shouldBe 1
        d.state.projectedState.hasKeyword(bird, Keyword.FLYING) shouldBe true
        d.state.projectedState.hasSubtype(bird, "Bird") shouldBe true
        d.state.projectedState.getColors(bird) shouldBe setOf("WHITE")
    }

    test("bounce mode excludes itself and lands and returns an opponent's permanent") {
        val d = driver()
        val opponent = d.getOpponent(d.player1)
        val bear = d.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val island = d.putLandOnBattlefield(d.player1, "Island")
        val channeler = d.castChanneler()
        d.chooseMode("Return")
        (d.submitTargetSelection(d.player1, listOf(channeler)).error != null) shouldBe true
        (d.submitTargetSelection(d.player1, listOf(island)).error != null) shouldBe true
        d.submitTargetSelection(d.player1, listOf(bear)).error shouldBe null
        d.bothPass().error shouldBe null
        d.getHand(opponent).contains(bear) shouldBe true
        d.getPermanents(d.player1).contains(channeler) shouldBe true
    }

    test("draw mode survives the Channeler being destroyed in response") {
        val d = driver()
        val channeler = d.castChanneler()
        d.chooseMode("Draw")
        val before = d.getHandSize(d.player1)
        val top = d.putCardOnTopOfLibrary(d.player1, "Forest")
        val opponent = d.getOpponent(d.player1)
        val bolt = d.putCardInHand(opponent, "Lightning Bolt")
        d.giveMana(opponent, Color.RED)
        d.passPriority(d.player1).error shouldBe null
        d.castSpell(opponent, bolt, listOf(channeler)).error shouldBe null
        d.bothPass().error shouldBe null
        d.getGraveyard(d.player1).contains(channeler) shouldBe true
        d.bothPass().error shouldBe null
        d.getHandSize(d.player1) shouldBe before + 1
        d.getHand(d.player1).contains(top) shouldBe true
    }
})
