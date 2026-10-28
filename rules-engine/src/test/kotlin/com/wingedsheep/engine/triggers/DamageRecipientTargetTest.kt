package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Engine coverage for `EffectTarget.DamageRecipient` — "that creature / that planeswalker" in a
 * deals-damage trigger, the object that was dealt the damage.
 *
 * A deals-damage observer with a source filter ("whenever a creature you control deals combat
 * damage to …") binds the triggering entity to the damage's *source*, so the recipient needs its
 * own reference; on a self-bound deals-damage trigger it names the same object as the triggering
 * entity. Either way the attacker that dealt the damage is left alone.
 */
class DamageRecipientTargetTest : FunSpec({

    val attacker = card("Recipient Probe Attacker") {
        manaCost = "{1}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    val wall = card("Recipient Probe Wall") {
        manaCost = "{1}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 5
    }

    val observer = card("Recipient Probe Observer") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.AnyCreature)
            effect = Effects.Destroy(EffectTarget.DamageRecipient)
        }
    }

    val selfStriker = card("Recipient Probe Striker") {
        manaCost = "{1}"
        typeLine = "Creature — Snake"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.self.dealsCombatDamage(Recipient.AnyCreature)
            effect = Effects.Destroy(EffectTarget.DamageRecipient)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(attacker, wall, observer, selfStriker))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.attackIntoWall(attackerName: String) {
        val active = activePlayer!!
        val defender = getOpponent(active)
        val striker = putCreatureOnBattlefield(active, attackerName)
        val blocker = putCreatureOnBattlefield(defender, "Recipient Probe Wall")
        removeSummoningSickness(striker)
        removeSummoningSickness(blocker)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(active, listOf(striker), defender).error shouldBe null
        bothPass()
        declareBlockers(defender, mapOf(blocker to listOf(striker))).error shouldBe null
        passPriorityUntil(Step.END_COMBAT)
    }

    test("an observer with a source filter destroys the damaged creature, not the damage source") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        driver.putPermanentOnBattlefield(active, "Recipient Probe Observer")

        driver.attackIntoWall("Recipient Probe Attacker")

        val defender = driver.getOpponent(active)
        driver.findPermanent(defender, "Recipient Probe Wall") shouldBe null
        driver.findPermanent(active, "Recipient Probe Attacker") shouldNotBe null
    }

    test("a self-bound deals-damage trigger names the same recipient as the triggering entity") {
        val driver = createDriver()
        val active = driver.activePlayer!!

        driver.attackIntoWall("Recipient Probe Striker")

        val defender = driver.getOpponent(active)
        driver.findPermanent(defender, "Recipient Probe Wall") shouldBe null
        driver.findPermanent(active, "Recipient Probe Striker") shouldNotBe null
    }

    test("without the trigger the wall survives the 2 damage") {
        val driver = createDriver()
        val active = driver.activePlayer!!

        driver.attackIntoWall("Recipient Probe Attacker")

        driver.findPermanent(driver.getOpponent(active), "Recipient Probe Wall") shouldNotBe null
    }
})
