package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.AncestralRecall
import com.wingedsheep.mtg.sets.definitions.lea.cards.Fork
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Fork (LEA) — "Copy target instant or sorcery spell, except that the copy is red. You may choose
 * new targets for the copy."
 */
class ForkScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + Fork + AncestralRecall)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingPlayer = 0, startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Cast Fork at [spell] and let it resolve, stopping at the copy's new-targets question. */
    fun GameTestDriver.fork(spell: EntityId) {
        val fork = putCardInHand(player1, "Fork")
        giveMana(player1, Color.RED, 2)
        castSpellWithTargets(player1, fork, listOf(ChosenTarget.Spell(spell))).error shouldBe null
        var guard = 0
        while (state.pendingDecision !is ChooseTargetsDecision && guard++ < 10) bothPass()
        (state.pendingDecision is ChooseTargetsDecision) shouldBe true
    }

    fun GameTestDriver.copyOnStack() = state.stack.single { state.getEntity(it)?.has<CopyOfComponent>() == true }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (stackSize > 0 && guard++ < 20) bothPass()
    }

    test("the copy is red, and its target can be changed") {
        val d = driver()
        val recall = d.putCardInHand(d.player1, "Ancestral Recall")
        d.giveMana(d.player1, Color.BLUE, 1)
        d.castSpellWithTargets(d.player1, recall, listOf(ChosenTarget.Player(d.player2))).error shouldBe null
        d.fork(recall)

        d.submitTargetSelection(d.player1, listOf(d.player1)).error shouldBe null
        val copy = d.copyOnStack()
        withClue("the copy is red rather than the blue its mana cost would make it") {
            d.state.getEntity(copy)!!.get<CardComponent>()!!.colors shouldBe setOf(Color.RED)
        }
        val myHand = d.getHand(d.player1).size
        val theirHand = d.getHand(d.player2).size
        d.resolveAll()
        withClue("the copy's new target draws three, and the original still draws its three") {
            d.getHand(d.player1).size shouldBe myHand + 3
            d.getHand(d.player2).size shouldBe theirHand + 3
        }
    }

    test("choosing no new target keeps the original one") {
        val d = driver()
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpellWithTargets(d.player1, bolt, listOf(ChosenTarget.Player(d.player2))).error shouldBe null
        d.fork(bolt)

        d.submitTargetSelection(d.player1, emptyList()).error shouldBe null
        d.state.getEntity(d.copyOnStack())!!.get<TargetsComponent>()!!.targets shouldBe
            listOf(ChosenTarget.Player(d.player2))
        d.resolveAll()
        d.getLifeTotal(d.player2) shouldBe 14
    }
})
