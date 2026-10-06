package com.wingedsheep.engine.event

import com.wingedsheep.engine.core.BlockersDeclaredEvent
import com.wingedsheep.engine.core.BlocksCreatedEvent
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class AttachedBecomesBlockedTest : FunSpec({
    fun driver(filter: GameObjectFilter? = GameObjectFilter.Creature): GameTestDriver {
        val observer = card("Block Observer") {
            typeLine = "Artifact — Equipment"
            triggeredAbility {
                trigger = Triggers.attached.becomesBlocked(by = filter)
                effect = Effects.DrawCards(1)
            }
        }
        return GameTestDriver().apply {
            registerCards(TestCards.all + observer)
            initMirrorMatch(deck = Deck.of("Forest" to 40))
        }
    }

    fun detector(driver: GameTestDriver): TriggerDetector {
        val predicates = PredicateEvaluator(cardRegistry = driver.cardRegistry)
        return TriggerDetector(driver.cardRegistry, conditionEvaluator = predicates.conditions, predicateEvaluator = predicates)
    }

    test("one trigger per blocker keeps the attachment source and controller") {
        val d = driver()
        val host = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val first = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val second = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        // Equipment can be controlled by a different player than the equipped creature.
        val equipment = d.putPermanentOnBattlefield(d.player2, "Block Observer")
        d.replaceState(d.state.updateEntity(equipment) { it.with(AttachedToComponent(host)) })
        val triggers = detector(d).detectTriggers(d.state, listOf(BlockersDeclaredEvent(mapOf(first to listOf(host), second to listOf(host)))))
        triggers shouldHaveSize 2
        triggers.map { it.sourceId }.toSet() shouldBe setOf(equipment)
        triggers.map { it.controllerId }.toSet() shouldBe setOf(d.player2)
        triggers.map { it.triggerContext.triggeringEntityId }.toSet() shouldBe setOf(first, second)
    }

    test("bare becomes blocked fires once for multiple blockers and not for a later blocker") {
        val d = driver(null)
        val host = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val first = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val second = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val equipment = d.putPermanentOnBattlefield(d.player1, "Block Observer")
        d.replaceState(d.state.updateEntity(equipment) { it.with(AttachedToComponent(host)) })
        val triggers = detector(d).detectTriggers(d.state, listOf(BlockersDeclaredEvent(mapOf(first to listOf(host), second to listOf(host)))))
        triggers shouldHaveSize 1
        triggers.single().triggerContext.triggeringEntityId shouldBe host
        detector(d).detectTriggers(d.state, listOf(BlocksCreatedEvent(
            blockers = mapOf(second to listOf(host)), newBlockers = setOf(second),
            newlyBlockedAttackers = emptySet(), previousBlockedCounts = emptyMap(), blockedCounts = mapOf(second to 1),
        ))) shouldHaveSize 0
    }

    test("a later blocker triggers even when the host is already blocked") {
        val d = driver()
        val host = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val blocker = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val equipment = d.putPermanentOnBattlefield(d.player1, "Block Observer")
        d.replaceState(d.state.updateEntity(equipment) { it.with(AttachedToComponent(host)) })
        val triggers = detector(d).detectTriggers(d.state, listOf(BlocksCreatedEvent(
            blockers = mapOf(blocker to listOf(host)), newBlockers = emptySet(),
            newlyBlockedAttackers = emptySet(), previousBlockedCounts = mapOf(blocker to 1), blockedCounts = mapOf(blocker to 2),
        )))
        triggers shouldHaveSize 1
        triggers.single().triggerContext.triggeringEntityId shouldBe blocker
    }

    test("no trigger for unattached equipment, another attacker, or the host blocking") {
        val d = driver()
        val host = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val other = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val blocker = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val equipment = d.putPermanentOnBattlefield(d.player1, "Block Observer")
        val event = BlockersDeclaredEvent(mapOf(blocker to listOf(host)))
        detector(d).detectTriggers(d.state, listOf(event)) shouldHaveSize 0
        d.replaceState(d.state.updateEntity(equipment) { it.with(AttachedToComponent(other)) })
        detector(d).detectTriggers(d.state, listOf(event)) shouldHaveSize 0
        d.replaceState(d.state.updateEntity(equipment) { it.with(AttachedToComponent(blocker)) })
        detector(d).detectTriggers(d.state, listOf(event)) shouldHaveSize 0
    }

    test("the blocker filter uses projected keywords and excludes nonmatching blockers") {
        val d = driver(GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
        val host = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val ground = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val flying = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val grant = d.services.effectExecutorRegistry.execute(
            d.state, Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self),
            EffectContext(sourceId = flying, controllerId = d.player2),
        )
        d.replaceState(grant.state)
        val equipment = d.putPermanentOnBattlefield(d.player1, "Block Observer")
        d.replaceState(d.state.updateEntity(equipment) { it.with(AttachedToComponent(host)) })
        val triggers = detector(d).detectTriggers(d.state, listOf(BlockersDeclaredEvent(mapOf(ground to listOf(host), flying to listOf(host)))))
        triggers shouldHaveSize 1
        triggers.single().triggerContext.triggeringEntityId shouldBe flying
    }
})
