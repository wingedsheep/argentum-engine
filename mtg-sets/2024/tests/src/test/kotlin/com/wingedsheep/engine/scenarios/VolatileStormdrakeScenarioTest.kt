package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.VolatileStormdrake
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class VolatileStormdrakeScenarioTest : FunSpec({
    val pinger = card("Test Pinger") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mana("{1}")
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }
    val drake = VolatileStormdrake.name

    fun GameTestDriver.controllerOf(id: EntityId) = state.projectedState.getController(id)
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(VolatileStormdrake, pinger))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.energy() = state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    /** Cast the Stormdrake, resolve it, and aim its enters trigger at [victim]; the trigger is left on the stack. */
    fun GameTestDriver.castDrake(victim: EntityId): EntityId {
        val spell = putCardInHand(player1, drake)
        giveMana(player1, Color.BLUE, 1)
        giveColorlessMana(player1, 1)
        castSpell(player1, spell).error shouldBe null
        bothPass()
        submitTargetSelection(player1, listOf(victim)).error shouldBe null
        return findPermanent(player1, drake)!!
    }

    test("exchanges control, gives four energy, and paying the mana value keeps the creature") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val source = d.castDrake(bear)
        d.bothPass()
        d.controllerOf(source) shouldBe d.player2
        d.controllerOf(bear) shouldBe d.player1
        d.energy() shouldBe 4
        (d.state.pendingDecision as YesNoDecision).yesText shouldBe "Pay 2 energy counters"
        d.submitYesNo(d.player1, true).error shouldBe null
        d.energy() shouldBe 2
        (bear in d.state.getBattlefield()) shouldBe true
        d.controllerOf(bear) shouldBe d.player1
    }

    test("declining the payment sacrifices the stolen creature") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val source = d.castDrake(bear)
        d.bothPass()
        d.submitYesNo(d.player1, false).error shouldBe null
        d.energy() shouldBe 4
        (bear in d.state.getBattlefield()) shouldBe false
        d.getGraveyardCardNames(d.player2).contains("Grizzly Bears") shouldBe true
        d.controllerOf(source) shouldBe d.player2
    }

    test("if the Stormdrake is gone when the trigger resolves, nothing is exchanged and no energy is gained") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val source = d.castDrake(bear)
        d.moveToGraveyard(source)
        d.bothPass()
        d.controllerOf(bear) shouldBe d.player2
        d.energy() shouldBe 0
        d.state.pendingDecision shouldBe null
    }

    test("hexproof from activated abilities: once it's theirs, your abilities can't target it but your spells can") {
        val d = driver()
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val source = d.castDrake(bear)
        d.bothPass()
        d.submitYesNo(d.player1, true).error shouldBe null
        d.controllerOf(source) shouldBe d.player2
        val relic = d.putPermanentOnBattlefield(d.player1, pinger.name)
        d.giveColorlessMana(d.player1, 1)
        d.submit(
            ActivateAbility(d.player1, relic, pinger.script.activatedAbilities.single().id, listOf(ChosenTarget.Permanent(source)))
        ).error shouldNotBe null
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, bolt, listOf(source)).error shouldBe null
        d.bothPass()
        (source in d.state.getBattlefield()) shouldBe false
        d.getGraveyardCardNames(d.player1).contains(drake) shouldBe true
    }
})
