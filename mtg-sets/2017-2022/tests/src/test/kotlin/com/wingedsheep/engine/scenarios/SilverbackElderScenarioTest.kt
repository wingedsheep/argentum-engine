package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.dmu.cards.SilverbackElder
import com.wingedsheep.mtg.sets.definitions.lea.cards.AlphaForest294
import com.wingedsheep.mtg.sets.definitions.lea.cards.GrizzlyBears
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class SilverbackElderScenarioTest : FunSpec({
    val artifact = card("Elder Test Artifact") { typeLine = "Artifact"; manaCost = "{0}" }
    fun board(): GameTestDriver = GameTestDriver().apply {
        registerCards(listOf(SilverbackElder, AlphaForest294, GrizzlyBears, artifact))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        putCreatureOnBattlefield(player1, SilverbackElder.name)
    }
    fun GameTestDriver.choose(mode: String) {
        val creature = putCardInHand(player1, GrizzlyBears.name)
        giveMana(player1, Color.GREEN, 2)
        castSpell(player1, creature).error shouldBe null
        // Modal cast-trigger choices may be collected as the trigger is stacked or resolves.
        if (state.pendingDecision == null) bothPass()
        val decision = state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOfFirst { it.contains(mode) }
        (index >= 0) shouldBe true
        submitDecision(decision.playerId, OptionChosenResponse(decision.id, index)).error shouldBe null
    }
    fun GameTestDriver.finish() {
        repeat(20) {
            if (state.stack.isEmpty() || state.pendingDecision != null) return
            bothPass()
        }
        error("Stack did not resolve")
    }

    test("creature cast gains four life before the creature resolves and ordinary entry does not trigger") {
        val game = board()
        game.putCreatureOnBattlefield(game.player1, GrizzlyBears.name)
        game.state.stack.size shouldBe 0
        game.choose("gain 4 life")
        // Resolve only the trigger if the mode was selected while stacking it.
        if (game.getLifeTotal(game.player1) == 20) game.bothPass()
        game.getLifeTotal(game.player1) shouldBe 24
        game.state.stack.size shouldBe 1
        game.finish()
        game.getLifeTotal(game.player1) shouldBe 24
    }
    test("artifact mode destroys its selected target") {
        val game = board()
        val target = game.putPermanentOnBattlefield(game.player2, artifact.name)
        game.choose("Destroy")
        val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        game.submitTargetSelection(decision.playerId, listOf(target)).error shouldBe null
        game.finish()
        game.getGraveyard(game.player2) shouldContain target
    }
    for (takeLand in listOf(true, false)) {
        test("land mode allows taking a tapped land or declining: take=$takeLand") {
            val game = board()
            val top = List(5) { game.putCardOnTopOfLibrary(game.player1, "Forest") }
            game.choose("top five")
            if (game.state.pendingDecision == null) game.bothPass()
            val choice = game.state.pendingDecision!!
            game.submitCardSelection(choice.playerId, if (takeLand) listOf(top.last()) else emptyList()).error shouldBe null
            game.finish()
            if (takeLand) {
                game.state.getBattlefield() shouldContain top.last()
                game.isTapped(top.last()) shouldBe true
            } else {
                game.state.getBattlefield() shouldNotContain top.last()
            }
            val remaining = if (takeLand) top.dropLast(1) else top
            game.state.getLibrary(game.player1).takeLast(remaining.size).toSet() shouldBe remaining.toSet()
        }
    }
    test("land mode with no lands keeps all five cards and moves them to library bottom") {
        val game = board()
        val top = List(5) { game.putCardOnTopOfLibrary(game.player1, GrizzlyBears.name) }
        game.choose("top five")
        game.finish()
        game.state.pendingDecision?.let {
            game.submitCardSelection(it.playerId, emptyList()).error shouldBe null
            game.finish()
        }
        game.state.getLibrary(game.player1).takeLast(5).toSet() shouldBe top.toSet()
    }
})
