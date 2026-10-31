package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lrw.cards.ChandraNalaar
import com.wingedsheep.mtg.sets.definitions.lrw.cards.JaceBeleren
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.Outcome
import io.kotest.matchers.shouldNotBe

class ChandraNalaarScenarioTest : FunSpec({
    val animatedWalker = card("Animated Planeswalker Test") {
        manaCost = "{3}{U}"
        typeLine = "Artifact Creature Planeswalker — Golem"
        startingLoyalty = 15
        power = 1
        toughness = 30
    }
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(ChandraNalaar, JaceBeleren, animatedWalker))
        it.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun putChandra(d: GameTestDriver, player: EntityId): EntityId =
        d.putPermanentOnBattlefield(player, "Chandra Nalaar").also {
            d.addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to 6)))
        }
    fun loyalty(d: GameTestDriver, id: EntityId) =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0
    val abilities = ChandraNalaar.script.activatedAbilities

    test("casting Chandra gives her six starting loyalty counters") {
        val d = driver()
        val me = d.activePlayer!!
        val chandra = d.putCardInHand(me, "Chandra Nalaar")
        d.giveMana(me, com.wingedsheep.sdk.core.Color.RED, 5)
        d.castSpell(me, chandra).outcome shouldBe Outcome.Done
        d.bothPass()
        loyalty(d, chandra) shouldBe 6
    }

    test("plus one damages only the targeted player and adds loyalty as a cost") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val chandra = putChandra(d, me)
        d.submitSuccess(ActivateAbility(me, chandra, abilities[0].id,
            targets = listOf(ChosenTarget.Player(opp))))
        loyalty(d, chandra) shouldBe 7
        d.bothPass()
        d.getLifeTotal(opp) shouldBe 19
        d.getLifeTotal(me) shouldBe 20
    }

    for (x in listOf(0, 2, 6)) {
        test("minus X pays and resolves with X=$x even when Chandra spends all loyalty") {
            val d = driver()
            val me = d.activePlayer!!
            val chandra = putChandra(d, me)
            val creature = d.putPermanentOnBattlefield(d.getOpponent(me), "Grizzly Bears")
            val result = d.submitSuccess(ActivateAbility(me, chandra, abilities[1].id,
                targets = listOf(ChosenTarget.Permanent(creature)), xValue = x))
            loyalty(d, chandra) shouldBe 6 - x
            result.events.filterIsInstance<CountersRemovedEvent>().sumOf { it.amount } shouldBe x
            d.bothPass()
            if (x == 0) {
                d.state.getEntity(creature)?.get<DamageComponent>()?.amount.orZero() shouldBe 0
            } else {
                d.getGraveyard(d.getOpponent(me)).contains(creature) shouldBe true
            }
        }
    }

    for (x in listOf(-1, 7)) {
        test("rejects an unaffordable or negative X=$x without paying loyalty") {
            val d = driver()
            val me = d.activePlayer!!
            val chandra = putChandra(d, me)
            val creature = d.putPermanentOnBattlefield(d.getOpponent(me), "Grizzly Bears")
            d.submit(ActivateAbility(me, chandra, abilities[1].id,
                targets = listOf(ChosenTarget.Permanent(creature)), xValue = x)).outcome shouldNotBe Outcome.Done
            loyalty(d, chandra) shouldBe 6
        }
    }

    test("minus X fizzles if its creature leaves while its loyalty cost stays paid") {
        val d = driver()
        val me = d.activePlayer!!
        val chandra = putChandra(d, me)
        val creature = d.putPermanentOnBattlefield(d.getOpponent(me), "Grizzly Bears")
        d.submitSuccess(ActivateAbility(me, chandra, abilities[1].id,
            targets = listOf(ChosenTarget.Permanent(creature)), xValue = 2))
        d.moveToGraveyard(creature)
        d.bothPass()
        loyalty(d, chandra) shouldBe 4
        d.getLifeTotal(me) shouldBe 20
        d.getLifeTotal(d.getOpponent(me)) shouldBe 20
    }

    for (targetPlaneswalker in listOf(false, true)) {
        test("ultimate damages the recipient and their creatures - planeswalker=$targetPlaneswalker") {
            val d = driver()
            val me = d.activePlayer!!
            val opp = d.getOpponent(me)
            val chandra = putChandra(d, me)
            d.addComponent(chandra, CountersComponent(mapOf(CounterType.LOYALTY to 8)))
            val mine = d.putPermanentOnBattlefield(me, "Grizzly Bears")
            val theirs = d.putPermanentOnBattlefield(opp, "Grizzly Bears")
            val jace = if (targetPlaneswalker) d.putPermanentOnBattlefield(opp, "Jace Beleren").also {
                d.addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to 15)))
            } else null
            val target = jace?.let { ChosenTarget.Permanent(it) } ?: ChosenTarget.Player(opp)
            d.submitSuccess(ActivateAbility(me, chandra, abilities[2].id, targets = listOf(target)))
            d.bothPass()
            d.getLifeTotal(opp) shouldBe if (targetPlaneswalker) 20 else 10
            if (jace != null) loyalty(d, jace) shouldBe 5
            d.getGraveyard(opp).contains(theirs) shouldBe true
            d.getGraveyard(me).contains(mine) shouldBe false
        }
    }
    test("ultimate does not damage an animated target planeswalker twice") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val chandra = putChandra(d, me)
        d.addComponent(chandra, CountersComponent(mapOf(CounterType.LOYALTY to 8)))
        val target = d.putPermanentOnBattlefield(opp, animatedWalker.name)
        d.addComponent(target, CountersComponent(mapOf(CounterType.LOYALTY to 15)))
        d.submitSuccess(ActivateAbility(me, chandra, abilities[2].id,
            targets = listOf(ChosenTarget.Permanent(target))))
        d.bothPass()
        loyalty(d, target) shouldBe 5
        d.getLifeTotal(opp) shouldBe 20
    }

})

private fun Int?.orZero() = this ?: 0
