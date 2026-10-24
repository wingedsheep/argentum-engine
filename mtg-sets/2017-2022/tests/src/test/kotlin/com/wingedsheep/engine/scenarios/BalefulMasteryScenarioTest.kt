package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.stx.cards.BalefulMastery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Baleful Mastery (STX #64): "You may pay {1}{B} rather than pay this spell's mana cost. If the
 * {1}{B} cost was paid, an opponent draws a card. Exile target creature or planeswalker."
 */
class BalefulMasteryScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + BalefulMastery)
        it.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("cast for {1}{B}: exiles the creature and the opponent draws a card") {
        val d = createDriver()
        val me = d.player1
        val opponent = d.player2
        val courser = d.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val mastery = d.putCardInHand(me, "Baleful Mastery")
        d.giveMana(me, Color.BLACK, 2)
        val opponentHand = d.getHand(opponent).size

        d.submit(CastSpell(me, mastery, targets = listOf(ChosenTarget.Permanent(courser)),
            useAlternativeCost = true, alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE)).error shouldBe null
        d.bothPass()

        d.findPermanent(opponent, "Centaur Courser") shouldBe null
        d.getExile(opponent).contains(courser) shouldBe true
        d.getHand(opponent).size shouldBe opponentHand + 1
    }

    test("cast for {3}{B}: exiles the creature and nobody draws") {
        val d = createDriver()
        val me = d.player1
        val opponent = d.player2
        val courser = d.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val mastery = d.putCardInHand(me, "Baleful Mastery")
        d.giveMana(me, Color.BLACK, 4)
        val opponentHand = d.getHand(opponent).size
        val myHand = d.getHand(me).size

        d.castSpellWithTargets(me, mastery, listOf(ChosenTarget.Permanent(courser))).error shouldBe null
        d.bothPass()

        d.getExile(opponent).contains(courser) shouldBe true
        d.getHand(opponent).size shouldBe opponentHand
        d.getHand(me).size shouldBe myHand - 1
    }
})
