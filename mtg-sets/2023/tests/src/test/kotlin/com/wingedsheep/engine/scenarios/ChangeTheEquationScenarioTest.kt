package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.ChangeTheEquation
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Change the Equation (MOM #50) — mode 0 counters a spell with mana value 2 or less, mode 1 a
 * red or green spell with mana value 6 or less. Both are targeting limits, so an out-of-range
 * spell must be rejected at cast time.
 */
class ChangeTheEquationScenarioTest : FunSpec({

    fun setup(spellName: String, color: Color): Triple<GameTestDriver, EntityId, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ChangeTheEquation)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.giveMana(me, color, 12)
        val card = d.putCardInHand(me, spellName)
        d.castSpell(me, card).error shouldBe null
        d.passPriority(me)
        return Triple(d, me, opp)
    }

    fun GameTestDriver.change(opp: EntityId, mode: Int): ExecutionResult {
        val target = ChosenTarget.Spell(getTopOfStack()!!)
        giveMana(opp, Color.BLUE, 2)
        val c = putCardInHand(opp, "Change the Equation")
        return submit(
            CastSpell(
                playerId = opp,
                cardId = c,
                targets = listOf(target),
                chosenModes = listOf(mode),
                modeTargetsOrdered = listOf(listOf(target)),
            )
        )
    }

    test("mode 0 counters a red spell with mana value 1") {
        val (d, me, opp) = setup("Goblin Guide", Color.RED)
        d.change(opp, 0).error shouldBe null
        d.bothPass()
        d.getGraveyardCardNames(me).contains("Goblin Guide") shouldBe true
    }

    test("mode 0 rejects a spell with mana value 3") {
        val (d, _, opp) = setup("Centaur Courser", Color.GREEN)
        (d.change(opp, 0).error != null) shouldBe true
    }

    test("mode 1 counters a green spell with mana value 3") {
        val (d, me, opp) = setup("Centaur Courser", Color.GREEN)
        d.change(opp, 1).error shouldBe null
        d.bothPass()
        d.getGraveyardCardNames(me).contains("Centaur Courser") shouldBe true
    }

    test("mode 1 rejects a green spell with mana value above 6") {
        val (d, _, opp) = setup("Ghalta, Primal Hunger", Color.GREEN)
        (d.change(opp, 1).error != null) shouldBe true
    }

    test("mode 1 rejects a spell that is neither red nor green") {
        val (d, _, opp) = setup("Black Creature", Color.BLACK)
        (d.change(opp, 1).error != null) shouldBe true
    }
})
