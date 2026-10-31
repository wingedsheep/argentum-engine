package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * An event-based delayed trigger over an `EventPattern.AnyOf` ("when you next cast an instant
 * spell … or activate a … ability this turn" — Repeated Reverberation): every atom is matched
 * with its own scoping, a one-shot trigger is consumed by whichever atom fires first, and an
 * event matching no atom leaves it armed.
 */
class DelayedAnyOfTriggerTest : FunSpec({
    fun watcher(name: String, fireOnce: Boolean) = card(name) {
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.CreateDelayedTrigger(
                trigger = Triggers.or(
                    Triggers.you.casts(GameObjectFilter.Instant),
                    Triggers.you.activatesAbility(of = GameObjectFilter.Creature)
                ),
                fireOnce = fireOnce,
                expiry = DelayedTriggerExpiry.EndOfTurn,
                effect = Effects.GainLife(1),
            )
        }
    }
    val oneShot = watcher("Test AnyOf Once", fireOnce = true)
    val repeating = watcher("Test AnyOf Repeating", fireOnce = false)
    val pumper = card("Test AnyOf Pumper") {
        manaCost = "{1}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Mana("{0}")
            effect = Effects.DrawCards(1)
        }
    }

    fun setup(): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver().also {
            it.registerCards(TestCards.all + listOf(oneShot, repeating, pumper))
            it.initMirrorMatch(Deck.of("Mountain" to 40))
            it.passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        return d to d.activePlayer!!
    }

    fun GameTestDriver.arm(player: EntityId, name: String) {
        val source = putPermanentOnBattlefield(player, name)
        val ability = cardRegistry.getCard(name)!!.script.activatedAbilities.single().id
        submit(ActivateAbility(player, source, ability)).outcome shouldBe Outcome.Done
        bothPass()
    }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    fun GameTestDriver.bolt(player: EntityId) {
        val bolt = putCardInHand(player, "Lightning Bolt")
        giveMana(player, Color.RED, 1)
        castSpell(player, bolt, listOf(getOpponent(player))).error shouldBe null
        resolveAll()
    }

    fun GameTestDriver.activatePumper(player: EntityId, pumperId: EntityId) {
        val ability = cardRegistry.getCard(pumper.name)!!.script.activatedAbilities.single().id
        submit(ActivateAbility(player, pumperId, ability)).error shouldBe null
        resolveAll()
    }

    test("the instant-cast atom fires it, and a one-shot trigger is spent") {
        val (d, player) = setup()
        d.arm(player, oneShot.name)
        d.state.delayedTriggers.size shouldBe 1
        d.bolt(player)
        d.getLifeTotal(player) shouldBe 21
        d.state.delayedTriggers.size shouldBe 0
        d.bolt(player)
        d.getLifeTotal(player) shouldBe 21
    }

    test("the ability-activation atom fires it too") {
        val (d, player) = setup()
        val creature = d.putPermanentOnBattlefield(player, pumper.name)
        d.arm(player, oneShot.name)
        d.activatePumper(player, creature)
        d.getLifeTotal(player) shouldBe 21
        d.state.delayedTriggers.size shouldBe 0
    }

    test("an event no atom matches leaves it armed") {
        val (d, player) = setup()
        d.arm(player, oneShot.name)
        // A creature spell isn't an instant, and the watcher's own {T} ability isn't a creature's.
        val bears = d.putCardInHand(player, "Centaur Courser")
        d.giveMana(player, Color.GREEN, 3)
        d.castSpell(player, bears).error shouldBe null
        d.resolveAll()
        d.getLifeTotal(player) shouldBe 20
        d.state.delayedTriggers.size shouldBe 1
    }

    test("a repeating any-of trigger fires once per matching event, through either atom") {
        val (d, player) = setup()
        val creature = d.putPermanentOnBattlefield(player, pumper.name)
        d.arm(player, repeating.name)
        d.bolt(player)
        d.activatePumper(player, creature)
        d.bolt(player)
        d.getLifeTotal(player) shouldBe 23
        d.state.delayedTriggers.size shouldBe 1
    }
})
