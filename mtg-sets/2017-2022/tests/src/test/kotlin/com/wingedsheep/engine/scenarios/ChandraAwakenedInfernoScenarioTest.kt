package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lrw.cards.JaceBeleren
import com.wingedsheep.mtg.sets.definitions.m20.cards.ChandraAwakenedInferno
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Chandra, Awakened Inferno (M20 #127): the +2 emblem goes to each opponent and burns them on
 * their own upkeep from a colorless source, outliving Chandra; −3 spares Elementals; −X exiles a
 * creature or planeswalker it dealt damage to instead of letting it die.
 */
class ChandraAwakenedInfernoScenarioTest : FunSpec({
    val elemental = card("Test Elemental") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Elemental"
        power = 2
        toughness = 2
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(ChandraAwakenedInferno, JaceBeleren, elemental))
        it.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun putChandra(d: GameTestDriver, player: EntityId): EntityId =
        d.putPermanentOnBattlefield(player, "Chandra, Awakened Inferno").also {
            d.addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to 6)))
        }
    fun GameTestDriver.drain() {
        var passes = 0
        while (stackSize > 0 && passes++ < 20) bothPass()
    }
    val abilities = ChandraAwakenedInferno.script.activatedAbilities

    test("+2 gives the opponent a colorless emblem that burns them each upkeep, even after Chandra is gone") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val chandra = putChandra(d, me)
        d.submitSuccess(ActivateAbility(me, chandra, abilities[0].id))
        d.drain()

        val global = d.state.globalGrantedTriggeredAbilities.single()
        global.controllerId shouldBe opp
        d.state.getEntity(global.sourceId)!!.get<CardComponent>()!!.colors.shouldBeEmpty()

        d.moveToGraveyard(chandra)
        d.passPriorityUntil(Step.UPKEEP, opp)
        d.drain()
        d.getLifeTotal(opp) shouldBe 19

        d.passPriorityUntil(Step.UPKEEP, me)
        d.drain()
        d.getLifeTotal(me) shouldBe 20
        d.passPriorityUntil(Step.UPKEEP, opp)
        d.drain()
        d.getLifeTotal(opp) shouldBe 18
    }

    test("−3 deals 3 damage to each non-Elemental creature") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val chandra = putChandra(d, me)
        val bears = d.putPermanentOnBattlefield(opp, "Grizzly Bears")
        val fire = d.putPermanentOnBattlefield(opp, "Test Elemental")
        d.submitSuccess(ActivateAbility(me, chandra, abilities[1].id))
        d.drain()
        d.getGraveyard(opp) shouldContain bears
        d.findPermanent(opp, "Test Elemental") shouldBe fire
        (d.state.getEntity(fire)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
    }

    test("−X exiles the creature it dealt lethal damage to") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val chandra = putChandra(d, me)
        val bears = d.putPermanentOnBattlefield(opp, "Grizzly Bears")
        d.submitSuccess(ActivateAbility(me, chandra, abilities[2].id,
            targets = listOf(ChosenTarget.Permanent(bears)), xValue = 2))
        d.drain()
        d.getExileCardNames(opp) shouldContain "Grizzly Bears"
        d.getGraveyardCardNames(opp) shouldNotContain "Grizzly Bears"
    }

    test("−X exiles a planeswalker it reduces to zero loyalty") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val chandra = putChandra(d, me)
        val jace = d.putPermanentOnBattlefield(opp, "Jace Beleren").also {
            d.addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to 3)))
        }
        d.submitSuccess(ActivateAbility(me, chandra, abilities[2].id,
            targets = listOf(ChosenTarget.Permanent(jace)), xValue = 3))
        d.drain()
        d.getExileCardNames(opp) shouldContain "Jace Beleren"
        d.getGraveyardCardNames(opp) shouldNotContain "Jace Beleren"
    }
})
