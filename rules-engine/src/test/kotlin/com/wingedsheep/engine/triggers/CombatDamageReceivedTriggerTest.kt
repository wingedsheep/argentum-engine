package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `Triggers.<subject>.isDealtDamage(damageType = …)`: "whenever this is dealt combat damage"
 * (Wall of Essence) keeps only combat damage, and still folds a combat damage step's simultaneous
 * damage into one trigger carrying the total (CR 603.2c); the noncombat form ignores combat damage.
 */
class CombatDamageReceivedTriggerTest : FunSpec({

    // "Whenever this creature is dealt combat damage, you gain that much life."
    val combatSponge = card("Test Combat Sponge") {
        manaCost = "{1}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 8
        triggeredAbility {
            trigger = Triggers.self.isDealtDamage(damageType = DamageType.Combat)
            effect = Effects.GainLife(DynamicAmounts.triggerDamageAmount())
        }
    }

    // "Whenever this creature is dealt noncombat damage, you gain that much life."
    val noncombatSponge = card("Test Noncombat Sponge") {
        manaCost = "{1}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 8
        triggeredAbility {
            trigger = Triggers.self.isDealtDamage(damageType = DamageType.NonCombat)
            effect = Effects.GainLife(DynamicAmounts.triggerDamageAmount())
        }
    }

    // "Whenever a source deals combat damage to this creature, you gain 1 life."
    val perSourceCombat = card("Test Per Source Combat Wall") {
        manaCost = "{1}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 8
        triggeredAbility {
            trigger = Triggers.self.isDealtDamage(by = GameObjectFilter.Any, damageType = DamageType.Combat)
            effect = Effects.GainLife(1)
        }
    }

    // "Whenever enchanted creature is dealt combat damage, you gain that much life."
    val combatLink = card("Test Combat Link") {
        manaCost = "{1}"
        typeLine = "Enchantment — Aura"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
        triggeredAbility {
            trigger = Triggers.attached.isDealtDamage(damageType = DamageType.Combat)
            effect = Effects.GainLife(DynamicAmounts.triggerDamageAmount())
        }
    }

    val bear = card("Test Bear") {
        manaCost = "{1}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    val ping = card("Test Damage Ping") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            val t = target(Targets.Any)
            effect = Effects.DealDamage(2, t)
        }
    }

    fun createDriver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(combatSponge, noncombatSponge, perSourceCombat, combatLink, bear, ping))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** [attacker] attacks; each of [blockers] blocks it; then combat runs out with triggers resolved. */
    fun GameTestDriver.attackIntoBlockers(attacker: EntityId, blockers: List<EntityId>) {
        val active = activePlayer!!
        val defender = getOpponent(active)
        (blockers + attacker).forEach(::removeSummoningSickness)
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(active, listOf(attacker), defender).outcome shouldBe Outcome.Done
        bothPass()
        declareBlockers(defender, blockers.associateWith { listOf(attacker) }).outcome shouldBe Outcome.Done
        passPriorityUntil(Step.END_COMBAT)
    }

    fun GameTestDriver.pingAndResolve(caster: EntityId, target: EntityId) {
        val id = putCardInHand(caster, "Test Damage Ping")
        giveMana(caster, Color.RED, 1)
        castSpell(caster, id, listOf(target)).outcome shouldBe Outcome.Done
        var guard = 0
        while (guard++ < 10 && state.stack.isNotEmpty()) bothPass()
    }

    test("combat damage from two blockers is one trigger carrying the total") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val sponge = d.putCreatureOnBattlefield(me, "Test Combat Sponge")
        val b1 = d.putCreatureOnBattlefield(them, "Test Bear")
        val b2 = d.putCreatureOnBattlefield(them, "Test Bear")

        d.attackIntoBlockers(sponge, listOf(b1, b2))

        d.getLifeTotal(me) shouldBe 24
    }

    test("noncombat damage doesn't trigger the combat form") {
        val d = createDriver()
        val me = d.activePlayer!!
        val sponge = d.putCreatureOnBattlefield(me, "Test Combat Sponge")

        d.pingAndResolve(me, sponge)

        d.getLifeTotal(me) shouldBe 20
    }

    test("the noncombat form triggers on a burn spell and ignores combat damage") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val sponge = d.putCreatureOnBattlefield(me, "Test Noncombat Sponge")
        val blocker = d.putCreatureOnBattlefield(them, "Test Bear")

        d.attackIntoBlockers(sponge, listOf(blocker))
        d.getLifeTotal(me) shouldBe 20

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.pingAndResolve(me, sponge)
        d.getLifeTotal(me) shouldBe 22
    }

    test("the per-source combat form triggers once per blocker and not on a burn spell") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val wall = d.putCreatureOnBattlefield(me, "Test Per Source Combat Wall")
        val b1 = d.putCreatureOnBattlefield(them, "Test Bear")
        val b2 = d.putCreatureOnBattlefield(them, "Test Bear")

        d.attackIntoBlockers(wall, listOf(b1, b2))
        d.getLifeTotal(me) shouldBe 22

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.pingAndResolve(me, wall)
        d.getLifeTotal(me) shouldBe 22
    }

    test("the attached combat form reads the host's combat damage only") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val host = d.putCreatureOnBattlefield(me, "Test Combat Sponge")
        val aura = d.putPermanentOnBattlefield(me, "Test Combat Link")
        d.replaceState(d.state.updateEntity(aura) { it.with(AttachedToComponent(host)) }
            .updateEntity(host) { it.with(AttachmentsComponent(listOf(aura))) })

        d.pingAndResolve(me, host)
        d.getLifeTotal(me) shouldBe 20

        val blocker = d.putCreatureOnBattlefield(them, "Test Bear")
        d.attackIntoBlockers(host, listOf(blocker))
        // The host's own trigger and the Aura's each gain 2.
        d.getLifeTotal(me) shouldBe 24
    }
})
