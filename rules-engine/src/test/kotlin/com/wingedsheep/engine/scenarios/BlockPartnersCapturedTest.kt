package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.event.PendingTrigger
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * "Whenever [this / equipped] creature blocks or becomes blocked by one or more creatures, …
 * one of those creatures" (Godsend): the once-per-combat form of `BlocksOrBecomesBlockedByEvent`
 * fires a single trigger however many creatures the creature is paired with (Godsend's ruling),
 * and captures every matching partner in the trigger's captured collection so the effect can
 * choose among them at resolution. The per-partner form fires once per partner and captures none.
 */
class BlockPartnersCapturedTest : FunSpec({
    val watcher = card("Partner Watcher") {
        typeLine = "Creature"; power = 2; toughness = 2
        triggeredAbility { trigger = Triggers.self.blocksOrBecomesBlocked(); effect = Effects.GainLife(1) }
    }
    val perPartner = card("Per-Partner Watcher") {
        typeLine = "Creature"; power = 2; toughness = 2
        triggeredAbility {
            trigger = Triggers.self.blocksOrBecomesBlocked(GameObjectFilter.Creature)
            effect = Effects.GainLife(1)
        }
    }
    val sword = card("Partner Sword") {
        typeLine = "Artifact — Equipment"
        triggeredAbility { trigger = Triggers.attached.blocksOrBecomesBlocked(); effect = Effects.GainLife(1) }
    }
    val wall = card("Partner Wall") { typeLine = "Creature"; power = 0; toughness = 4 }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(watcher, perPartner, sword, wall))
        initMirrorMatch(Deck.of("Plains" to 40)); passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.attackWith(name: String): EntityId {
        val opp = getOpponent(activePlayer!!)
        return putCreatureOnBattlefield(activePlayer!!, name).also { addComponent(it, AttackingComponent(opp)) }
    }

    fun GameTestDriver.doubleBlock(attacker: EntityId): Pair<List<EntityId>, List<PendingTrigger>> {
        val opp = getOpponent(activePlayer!!)
        val blockers = List(2) { putCreatureOnBattlefield(opp, wall.name) }
        val declared = services.combatManager.declareBlockers(state, opp, blockers.associateWith { listOf(attacker) })
        declared.error shouldBe null
        replaceState(declared.state)
        return blockers to services.triggerDetector.detectTriggers(state, declared.events)
    }

    test("blocked by two creatures: one trigger capturing both partners") {
        val d = driver()
        val attacker = d.attackWith(watcher.name)
        val (blockers, triggers) = d.doubleBlock(attacker)
        val mine = triggers.filter { it.sourceId == attacker }
        mine shouldHaveSize 1
        mine.single().triggerContext.capturedEntityIds!! shouldContainExactlyInAnyOrder blockers
        mine.single().triggerContext.triggeringEntityId shouldBe blockers.first()
    }

    test("per-partner wording fires once per partner and captures nothing") {
        val d = driver()
        val attacker = d.attackWith(perPartner.name)
        val (blockers, triggers) = d.doubleBlock(attacker)
        val mine = triggers.filter { it.sourceId == attacker }
        mine.map { it.triggerContext.triggeringEntityId } shouldContainExactlyInAnyOrder blockers
        mine.forEach { it.triggerContext.capturedEntityIds shouldBe null }
    }

    test("an equipment's trigger captures its equipped creature's partners") {
        val d = driver()
        val attacker = d.attackWith(wall.name)
        val equipment = d.putPermanentOnBattlefield(d.activePlayer!!, sword.name)
        d.addComponent(equipment, AttachedToComponent(attacker))
        val (blockers, triggers) = d.doubleBlock(attacker)
        val mine = triggers.filter { it.sourceId == equipment }
        mine shouldHaveSize 1
        mine.single().triggerContext.capturedEntityIds!! shouldContainExactlyInAnyOrder blockers
    }

    test("a delayed once-per-combat watch captures every partner too") {
        val d = driver()
        val attacker = d.attackWith(wall.name)
        d.replaceState(d.state.copy(delayedTriggers = listOf(
            DelayedTriggeredAbility(
                id = "watch-partners", effect = Effects.GainLife(1),
                sourceId = EntityId.of("departed-source"), sourceName = "Departed source",
                controllerId = d.activePlayer!!, watchedEntityId = attacker,
                trigger = Triggers.self.blocksOrBecomesBlocked()
            )
        )))
        val (blockers, triggers) = d.doubleBlock(attacker)
        val mine = triggers.filter { it.ability.id == AbilityId("delayed_watch-partners") }
        mine shouldHaveSize 1
        mine.single().triggerContext.capturedEntityIds!! shouldContainExactlyInAnyOrder blockers
    }
})
