package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.rna.cards.RhythmOfTheWild
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rhythm of the Wild — "Creature spells you control can't be countered. Nontoken creatures you
 * control have riot."
 *
 * The effect-entry riot seam is pinned by `GrantedRiotEffectEntryTest`; this covers the card on the
 * cast path: an uncounterable creature spell that still asks counter-or-haste, and a noncreature
 * spell that stays counterable.
 */
class RhythmOfTheWildScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(RhythmOfTheWild))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("a creature spell you control can't be countered, and it enters with your riot choice") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, "Rhythm of the Wild")
        val bears = d.putCardInHand(me, "Grizzly Bears")
        val counterspell = d.putCardInHand(opp, "Counterspell")
        d.giveMana(me, Color.GREEN, 2)
        d.giveMana(opp, Color.BLUE, 2)

        d.castSpell(me, bears).error shouldBe null
        d.passPriority(me)
        d.submit(CastSpell(opp, counterspell, targets = listOf(ChosenTarget.Spell(bears)), paymentStrategy = PaymentStrategy.FromPool))
            .error shouldBe null
        d.bothPass() // Counterspell resolves; the Bears can't be countered
        d.getGraveyard(opp) shouldContain counterspell
        d.stackSize shouldBe 1

        d.bothPass() // the Bears resolve and ask counter-or-haste
        val pick = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        pick.playerId shouldBe me
        d.submitDecision(me, OptionChosenResponse(pick.id, 0)).error shouldBe null

        (bears in d.state.getBattlefield()) shouldBe true
        d.state.getEntity(bears)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
    }

    test("a noncreature spell you control can still be countered") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, "Rhythm of the Wild")
        val bolt = d.putCardInHand(me, "Lightning Bolt")
        val counterspell = d.putCardInHand(opp, "Counterspell")
        d.giveMana(me, Color.RED, 1)
        d.giveMana(opp, Color.BLUE, 2)

        d.castSpell(me, bolt, targets = listOf(opp)).error shouldBe null
        d.passPriority(me)
        d.submit(CastSpell(opp, counterspell, targets = listOf(ChosenTarget.Spell(bolt)), paymentStrategy = PaymentStrategy.FromPool))
            .error shouldBe null
        while (d.stackSize > 0) d.bothPass()

        d.getGraveyard(me) shouldContain bolt
        d.getLifeTotal(opp) shouldBe 20
    }
})
