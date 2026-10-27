package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.answerForcedPayments
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ice.cards.MysticRemora
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mystic Remora — "Cumulative upkeep {1}. Whenever an opponent casts a noncreature spell, you may
 * draw a card unless that player pays {4}."
 *
 * The cumulative upkeep rules are pinned by `CumulativeUpkeepTest`; this checks the card's own
 * wiring: the upkeep bill, and the toll that only noncreature spells pay.
 */
class MysticRemoraScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(MysticRemora))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("an opponent's noncreature spell: they decline the {4}, the Remora's controller may draw") {
        val d = driver()
        val caster = d.activePlayer!!
        val owner = d.getOpponent(caster)
        d.putPermanentOnBattlefield(owner, "Mystic Remora")
        val handBefore = d.getHandSize(owner)

        val bolt = d.putCardInHand(caster, "Lightning Bolt")
        d.giveMana(caster, Color.RED, 1)
        d.castSpell(caster, bolt, targets = listOf(owner)).error shouldBe null
        d.stackSize shouldBe 2
        d.bothPass()

        // {4} is unaffordable, so the caster can only refuse; the Remora's controller chooses to draw.
        d.pendingDecision!!.playerId shouldBe caster
        d.answerForcedPayments()
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe owner
        d.submitYesNo(owner, true)
        d.getHandSize(owner) shouldBe handBefore + 1
    }

    test("an opponent's creature spell doesn't trigger it") {
        val d = driver()
        val caster = d.activePlayer!!
        val owner = d.getOpponent(caster)
        d.putPermanentOnBattlefield(owner, "Mystic Remora")

        val bears = d.putCardInHand(caster, "Grizzly Bears")
        d.giveMana(caster, Color.GREEN, 2)
        d.castSpell(caster, bears).error shouldBe null
        d.stackSize shouldBe 1
    }

    test("cumulative upkeep {1}: one age counter at its controller's upkeep, pay {1} to keep it") {
        val d = driver()
        val me = d.activePlayer!!
        val remora = d.putPermanentOnBattlefield(me, "Mystic Remora")

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.getOpponent(me))
        d.passPriorityUntil(Step.UPKEEP, me)
        d.giveColorlessMana(me, 1)
        d.bothPass()
        d.state.getEntity(remora)!!.get<CountersComponent>()!!.getCount(CounterType.AGE) shouldBe 1
        d.submitYesNo(me, true)
        (remora in d.state.getBattlefield()) shouldBe true
    }
})
