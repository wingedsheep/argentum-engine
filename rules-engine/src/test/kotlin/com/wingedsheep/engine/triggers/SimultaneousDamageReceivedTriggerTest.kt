package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Whenever this is dealt damage" triggers once per damage event (CR 603.2c), and all combat
 * damage in a combat damage step is dealt simultaneously (CR 510.2) — so two blockers' damage is
 * one trigger carrying the total (Fungusaur, Boros Reckoner, Pain for All rulings). "Whenever a
 * source deals damage to this" is the per-source wording and still triggers once per source
 * (Nested Ghoul's ruling).
 */
class SimultaneousDamageReceivedTriggerTest : FunSpec({

    // "Whenever this creature is dealt 3 or more damage, you gain that much life" (Innocent
    // Bystander's gate). Two 2-power blockers fire it only if their damage is one event, so every
    // life total below tells one 4-damage trigger apart from two 2-damage ones. A 0-power attacker,
    // so combat never asks how to divide its damage among blockers.
    val threeOrMore = Conditions.CompareAmounts(DynamicAmounts.triggerDamageAmount(), ComparisonOperator.GTE, 3)

    val sponge = card("Test Damage Sponge") {
        manaCost = "{1}"
        typeLine = "Creature — Sponge"
        power = 0
        toughness = 8
        triggeredAbility {
            trigger = Triggers.self.isDealtDamage()
            triggerRestriction = threeOrMore
            effect = Effects.GainLife(DynamicAmounts.triggerDamageAmount())
        }
    }

    val brittleSponge = card("Test Brittle Sponge") {
        manaCost = "{1}"
        typeLine = "Creature — Sponge"
        power = 0
        toughness = 3
        triggeredAbility {
            trigger = Triggers.self.isDealtDamage()
            triggerRestriction = threeOrMore
            effect = Effects.GainLife(DynamicAmounts.triggerDamageAmount())
        }
    }

    // "Whenever a source deals damage to this creature, you gain 1 life."
    val perSource = card("Test Per Source Wall") {
        manaCost = "{1}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 8
        triggeredAbility {
            trigger = Triggers.self.isDealtDamage(by = GameObjectFilter.Any)
            effect = Effects.GainLife(1)
        }
    }

    // "Whenever enchanted creature is dealt 3 or more damage, you gain that much life."
    val painLink = card("Test Pain Link") {
        manaCost = "{1}"
        typeLine = "Enchantment — Aura"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
        triggeredAbility {
            trigger = Triggers.attached.isDealtDamage()
            triggerRestriction = threeOrMore
            effect = Effects.GainLife(DynamicAmounts.triggerDamageAmount())
        }
    }

    val bear = card("Test Bear") {
        manaCost = "{1}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    val striker = card("Test First Striker") {
        manaCost = "{1}"
        typeLine = "Creature — Soldier"
        power = 2
        toughness = 2
        keywords(Keyword.FIRST_STRIKE)
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
        registerCards(TestCards.all + listOf(sponge, brittleSponge, perSource, painLink, bear, striker, ping))
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

    fun GameTestDriver.attach(aura: EntityId, host: EntityId) {
        replaceState(state.updateEntity(aura) { it.with(AttachedToComponent(host)) }
            .updateEntity(host) { it.with(AttachmentsComponent(listOf(aura))) })
    }

    test("two blockers' simultaneous combat damage is one trigger carrying the total") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Damage Sponge")
        val b1 = d.putCreatureOnBattlefield(them, "Test Bear")
        val b2 = d.putCreatureOnBattlefield(them, "Test Bear")

        d.attackIntoBlockers(attacker, listOf(b1, b2))

        // One trigger for 4; two triggers for 2 each would both miss the gate.
        d.getLifeTotal(me) shouldBe 24
    }

    test("the folded trigger is one stack object") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Damage Sponge")
        val b1 = d.putCreatureOnBattlefield(them, "Test Bear")
        val b2 = d.putCreatureOnBattlefield(them, "Test Bear")
        listOf(attacker, b1, b2).forEach(d::removeSummoningSickness)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(attacker), them)
        d.bothPass()
        d.declareBlockers(them, mapOf(b1 to listOf(attacker), b2 to listOf(attacker)))

        var guard = 0
        while (guard++ < 20 && d.state.stack.isEmpty() && d.state.step != Step.END_COMBAT) d.bothPass()

        d.state.stack.size shouldBe 1
    }

    test("separate damage events trigger separately") {
        val d = createDriver()
        val me = d.activePlayer!!
        val sponge = d.putCreatureOnBattlefield(me, "Test Damage Sponge")

        d.pingAndResolve(me, sponge)
        d.pingAndResolve(me, sponge)

        // 2 and then 2 is never "3 or more damage" — the fold is for simultaneous damage only.
        d.getLifeTotal(me) shouldBe 20
    }

    test("a creature killed by the simultaneous damage still triggers once with the total") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Brittle Sponge")
        val b1 = d.putCreatureOnBattlefield(them, "Test Bear")
        val b2 = d.putCreatureOnBattlefield(them, "Test Bear")

        d.attackIntoBlockers(attacker, listOf(b1, b2))

        d.state.getBattlefield().contains(attacker) shouldBe false
        d.getLifeTotal(me) shouldBe 24
    }

    test("first-strike and regular combat damage are separate events") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Damage Sponge")
        val b1 = d.putCreatureOnBattlefield(them, "Test First Striker")
        val b2 = d.putCreatureOnBattlefield(them, "Test Bear")

        d.attackIntoBlockers(attacker, listOf(b1, b2))

        // First strike makes its own combat damage step (CR 510.4): 2, then 2.
        d.getLifeTotal(me) shouldBe 20
    }

    test("the per-source wording triggers once for each source") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Per Source Wall")
        val b1 = d.putCreatureOnBattlefield(them, "Test Bear")
        val b2 = d.putCreatureOnBattlefield(them, "Test Bear")

        d.attackIntoBlockers(attacker, listOf(b1, b2))

        d.getLifeTotal(me) shouldBe 22
    }

    test("enchanted creature being dealt simultaneous damage triggers the Aura once") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val attacker = d.putCreatureOnBattlefield(me, "Test Damage Sponge")
        // The sponge's own trigger also gains 4; the Aura's gains another 4.
        d.attach(d.putPermanentOnBattlefield(me, "Test Pain Link"), attacker)
        val b1 = d.putCreatureOnBattlefield(them, "Test Bear")
        val b2 = d.putCreatureOnBattlefield(them, "Test Bear")

        d.attackIntoBlockers(attacker, listOf(b1, b2))

        d.getLifeTotal(me) shouldBe 28
    }

    test("two such creatures each fold their own damage") {
        val d = createDriver()
        val me = d.activePlayer!!
        val them = d.getOpponent(me)
        val a1 = d.putCreatureOnBattlefield(me, "Test Damage Sponge")
        val a2 = d.putCreatureOnBattlefield(me, "Test Damage Sponge")
        val b1 = d.putCreatureOnBattlefield(them, "Test Bear")
        val b2 = d.putCreatureOnBattlefield(them, "Test Bear")
        val b3 = d.putCreatureOnBattlefield(them, "Test Bear")
        listOf(a1, a2, b1, b2, b3).forEach(d::removeSummoningSickness)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(a1, a2), them)
        d.bothPass()
        d.declareBlockers(them, mapOf(b1 to listOf(a1), b2 to listOf(a1), b3 to listOf(a2)))
        d.passPriorityUntil(Step.END_COMBAT)

        // a1's two blockers fold to 4; a2's lone 2 doesn't reach the gate, and isn't added to a1's.
        d.getLifeTotal(me) shouldBe 24
    }

    test("the source filter is the per-source wording, and only the SELF subject takes it") {
        (Triggers.self.isDealtDamage().event as EventPattern.DamageReceivedEvent).source shouldBe null
        Triggers.self.isDealtDamage().event.description shouldBe "this is dealt damage"
        Triggers.self.isDealtDamage(by = GameObjectFilter.Any).event.description shouldBe
            "a source deals damage to this"
        shouldThrow<IllegalArgumentException> {
            Triggers.attached.isDealtDamage(by = GameObjectFilter.Creature)
        }
    }
})
