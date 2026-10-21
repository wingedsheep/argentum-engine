package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.AttackersDeclaredEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * "Whenever a creature you control enters attacking" (`Triggers.a(filter).entersAttacking()`) and its
 * disjunction with `attacks()` (Arni Metalbrow).
 *
 * - An entrant matches only when it is actually attacking as it enters — a permanent put onto the
 *   battlefield tapped and attacking carries its `AttackingComponent` from the moment it enters
 *   (CR 508.4); an ordinary entry must not fail open.
 * - A creature that enters attacking never "attacked" (CR 508.4), so the `attacks()` atom stays silent.
 * - The `Triggers.or` disjunction expands per attacker exactly like the bare `attacks()` atom (one
 *   trigger per declared attacker, each with that attacker as the triggering entity, CR 603.2c).
 */
class EntersAttackingTriggerTest : FunSpec({
    val creatures = GameObjectFilter.Creature.youControl()
    val entersAttackingWatcher = card("Test Enters Attacking Watcher") {
        typeLine = "Creature"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.a(creatures).entersAttacking()
            effect = Effects.GainLife(1)
        }
    }
    val attacksOrEntersAttackingWatcher = card("Test Attacks Or Enters Attacking Watcher") {
        typeLine = "Creature"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.or(Triggers.a(creatures).attacks(), Triggers.a(creatures).entersAttacking())
            effect = Effects.GainLife(1)
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(entersAttackingWatcher, attacksOrEntersAttackingWatcher))
        it.initMirrorMatch(Deck.of("Mountain" to 40))
    }

    fun GameTestDriver.entry(id: EntityId) =
        ZoneChangeEvent(id, "Grizzly Bears", fromZone = Zone.HAND, toZone = Zone.BATTLEFIELD, ownerId = player1)

    fun GameTestDriver.markAttacking(id: EntityId) =
        replaceState(state.updateEntity(id) { it.with(AttackingComponent(player2, defendingPlayerId = player2)) })

    fun GameTestDriver.detect(watcher: EntityId, event: com.wingedsheep.engine.core.GameEvent) =
        services.triggerDetector.detectTriggers(state, listOf(event)).filter { it.sourceId == watcher }

    test("a creature entering attacking triggers entersAttacking") {
        val d = driver()
        val watcher = d.putCreatureOnBattlefield(d.player1, entersAttackingWatcher.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.markAttacking(bears)

        val triggers = d.detect(watcher, d.entry(bears))
        triggers.size shouldBe 1
        triggers.single().triggerContext.triggeringEntityId shouldBe bears
    }

    test("a creature entering without attacking does not trigger entersAttacking") {
        val d = driver()
        val watcher = d.putCreatureOnBattlefield(d.player1, entersAttackingWatcher.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")

        d.detect(watcher, d.entry(bears)).shouldBeEmpty()
    }

    test("the disjunction fires once per declared attacker, bound to that attacker") {
        val d = driver()
        val watcher = d.putCreatureOnBattlefield(d.player1, attacksOrEntersAttackingWatcher.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        listOf(watcher, bears).forEach { d.markAttacking(it) }

        val triggers = d.detect(watcher, AttackersDeclaredEvent(listOf(watcher, bears), attackingPlayerId = d.player1))
        triggers.map { it.triggerContext.triggeringEntityId } shouldContainExactlyInAnyOrder listOf(watcher, bears)
    }

    test("the disjunction fires once, through its entersAttacking atom, for a creature put in attacking") {
        val d = driver()
        val watcher = d.putCreatureOnBattlefield(d.player1, attacksOrEntersAttackingWatcher.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.markAttacking(bears)

        val triggers = d.detect(watcher, d.entry(bears))
        triggers.size shouldBe 1
        triggers.single().triggerContext.triggeringEntityId shouldBe bears
    }
})
