package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hou.cards.DjeruWithEyesOpen
import com.wingedsheep.mtg.sets.definitions.lrw.cards.JaceBeleren
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Djeru, With Eyes Open — "If a source would deal damage to a planeswalker you control, prevent
 * 1 of that damage." The first prevention static scoped to planeswalkers: the damage that would
 * remove loyalty counters is reduced, and only for planeswalkers Djeru's controller controls.
 */
class DjeruWithEyesOpenScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(DjeruWithEyesOpen, JaceBeleren))
        it.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.putJace(player: EntityId): EntityId =
        putPermanentOnBattlefield(player, "Jace Beleren").also {
            addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to 4)))
        }

    fun GameTestDriver.loyalty(id: EntityId): Int? =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY)

    fun GameTestDriver.bolt(caster: EntityId, target: EntityId) {
        giveMana(caster, Color.RED, 1)
        val bolt = putCardInHand(caster, "Lightning Bolt")
        castSpellWithTargets(caster, bolt, listOf(ChosenTarget.Permanent(target)))
        bothPass()
    }

    test("damage to a planeswalker you control is reduced by 1") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, "Djeru, With Eyes Open")
        val jace = d.putJace(me)

        d.bolt(me, jace)

        d.loyalty(jace) shouldBe 2
    }

    test("an opponent's planeswalker is not protected") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.putCreatureOnBattlefield(me, "Djeru, With Eyes Open")
        val theirJace = d.putJace(opp)

        d.bolt(me, theirJace)

        d.loyalty(theirJace) shouldBe 1
    }

    test("without Djeru the planeswalker takes the full damage") {
        val d = driver()
        val me = d.activePlayer!!
        val jace = d.putJace(me)

        d.bolt(me, jace)

        d.loyalty(jace) shouldBe 1
    }
})
