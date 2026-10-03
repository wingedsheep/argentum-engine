package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m20.cards.DreadPresence
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Dread Presence (M20) — Whenever a Swamp you control enters, choose one —
 * • You draw a card and you lose 1 life. • This creature deals 2 damage to any target and you gain
 * 2 life.
 */
class DreadPresenceScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + DreadPresence)
        initMirrorMatch(deck = Deck.of("Swamp" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.chooseMode(index: Int) {
        val decision = pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(decision.playerId, OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    test("a Swamp entering: draw a card and lose 1 life") {
        val d = setup()
        val you = d.activePlayer!!
        d.putCreatureOnBattlefield(you, "Dread Presence")
        val swamp = d.putCardInHand(you, "Swamp")
        val handBefore = d.getHand(you).size

        d.playLand(you, swamp).error shouldBe null
        d.chooseMode(0)
        d.bothPass()

        d.getHand(you).size shouldBe handBefore // -1 land played, +1 drawn
        d.getLifeTotal(you) shouldBe 19
    }

    test("a Swamp entering: 2 damage to any target and gain 2 life") {
        val d = setup()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putCreatureOnBattlefield(you, "Dread Presence")
        val swamp = d.putCardInHand(you, "Swamp")

        d.playLand(you, swamp).error shouldBe null
        d.chooseMode(1)
        d.submitTargetSelection(you, listOf(opponent)).error shouldBe null
        d.bothPass()

        d.getLifeTotal(opponent) shouldBe 18
        d.getLifeTotal(you) shouldBe 22
    }

    test("a non-Swamp land does not trigger") {
        val d = setup()
        val you = d.activePlayer!!
        d.putCreatureOnBattlefield(you, "Dread Presence")
        val forest = d.putCardInHand(you, "Forest")

        d.playLand(you, forest).error shouldBe null
        d.pendingDecision shouldBe null
        d.state.stack.isEmpty() shouldBe true
    }
})
