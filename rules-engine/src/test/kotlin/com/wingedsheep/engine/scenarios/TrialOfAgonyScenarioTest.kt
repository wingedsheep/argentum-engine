package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Trial of Agony ({R} sorcery) — Duskmourn: House of Horror.
 *
 * "Choose two target creatures controlled by the same opponent. That player chooses one of those
 * creatures. Trial of Agony deals 5 damage to that creature, and the other can't block this turn."
 *
 * Same Gather → Select(opponent chooses) → split shape as Barrin's Spite, with damage + can't-block
 * as the split effects. Verifies the opponent makes the choice, the chosen creature takes 5 damage
 * (dies if its toughness ≤ 5), and the other creature is barred from blocking that turn.
 */
class TrialOfAgonyScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
    }

    test("opponent chooses one of two targets to take 5 damage; the other can't block") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = if (you == d.player1) d.player2 else d.player1
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Two creatures controlled by the same opponent.
        val a = d.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3
        val b = d.putCreatureOnBattlefield(opponent, "Centaur Courser") // 3/3

        val spell = d.putCardInHand(you, "Trial of Agony")
        d.giveMana(you, Color.RED, 1)
        d.castSpell(you, spell, targets = listOf(a, b))
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // The targets' controller (the opponent) chooses which one takes the damage.
        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.playerId shouldBe opponent
        pick.options.toSet() shouldBe setOf(a, b)

        // Opponent sacrifices nothing here — they pick `a` to take the 5 damage.
        d.submitDecision(opponent, CardsSelectedResponse(decisionId = pick.id, selectedCards = listOf(a)))
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // `a` took 5 damage — a 3/3 dies as a state-based action and is in the graveyard.
        d.getPermanents(opponent) shouldContainExactlyInAnyOrder listOf(b)

        // `b` (the other, undamaged) carries the can't-block-this-turn restriction.
        d.state.projectedState.cantBlock(b) shouldBe true
    }
})
