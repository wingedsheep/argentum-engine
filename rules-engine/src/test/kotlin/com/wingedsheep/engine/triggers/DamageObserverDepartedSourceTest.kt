package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A source-filtered damage observer ("whenever a creature you control deals damage to a creature")
 * reads a source that died to the same combat damage as it last existed (CR 603.10) — including a
 * token, which has ceased to exist by the time triggers are detected — and a source-relative
 * filter (`notSourceItself()`) is read against the observing permanent.
 */
class DamageObserverDepartedSourceTest : FunSpec({

    // "Whenever another creature you control deals damage to a creature, you gain 1 life."
    val observer = card("Test Departed Source Observer") {
        manaCost = "{1}"
        typeLine = "Creature — Spirit"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl().notSourceItself())
                .dealsDamage(Recipient.AnyCreature)
            effect = Effects.GainLife(1)
        }
    }

    val trader = card("Test Trader") {
        manaCost = "{1}"
        typeLine = "Creature — Soldier"
        power = 1
        toughness = 1
    }

    fun createDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(observer, trader))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.attackIntoBlocker(attacker: EntityId, blocker: EntityId) {
        val active = activePlayer!!
        val defender = getOpponent(active)
        removeSummoningSickness(attacker)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(active, listOf(attacker), defender).outcome shouldBe Outcome.Done
        bothPass()
        declareBlockers(defender, mapOf(blocker to listOf(attacker))).outcome shouldBe Outcome.Done
        passPriorityUntil(Step.END_COMBAT)
    }

    test("a token that trades in combat still matches the source filter") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        d.putCreatureOnBattlefield(me, "Test Departed Source Observer")
        val token = d.putCreatureOnBattlefield(me, "Test Trader")
        d.replaceState(d.state.updateEntity(token) { it.with(TokenComponent) })
        val blocker = d.putCreatureOnBattlefield(them, "Test Trader")

        d.attackIntoBlocker(token, blocker)

        d.state.getEntity(token) shouldBe null
        d.getLifeTotal(me) shouldBe 21
    }

    test("a nontoken that trades in combat matches too") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        d.putCreatureOnBattlefield(me, "Test Departed Source Observer")
        val attacker = d.putCreatureOnBattlefield(me, "Test Trader")
        val blocker = d.putCreatureOnBattlefield(them, "Test Trader")

        d.attackIntoBlocker(attacker, blocker)

        d.getLifeTotal(me) shouldBe 21
    }

    test("the observer's own damage is excluded by notSourceItself") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val self = d.putCreatureOnBattlefield(me, "Test Departed Source Observer")
        val blocker = d.putCreatureOnBattlefield(them, "Test Trader")

        d.attackIntoBlocker(self, blocker)

        d.getLifeTotal(me) shouldBe 20
    }
})
