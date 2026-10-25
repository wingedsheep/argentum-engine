package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * An event-based delayed trigger on the recipient form of "is dealt damage"
 * (`EventPattern.DamageReceivedEvent`): filter-scoped ("until end of turn, whenever a creature an
 * opponent controls is dealt damage, …" — Green Dragon), with the damaged permanent as the
 * triggering entity, and entity-scoped ("whenever that creature is dealt damage this turn").
 */
class DelayedDamageReceivedTriggerTest : FunSpec({
    val observer = card("Test Damage Observer") {
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.CreateDelayedTrigger(
                trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).isDealtDamage(),
                fireOnce = false,
                expiry = DelayedTriggerExpiry.EndOfTurn,
                effect = Effects.Destroy(EffectTarget.TriggeringEntity),
            )
        }
    }
    val watcher = card("Test Damage Watcher") {
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            val creature = target(TargetFilter.Creature)
            effect = Effects.CreateDelayedTrigger(
                trigger = Triggers.self.isDealtDamage(),
                watchedTarget = creature,
                fireOnce = false,
                expiry = DelayedTriggerExpiry.EndOfTurn,
                effect = Effects.GainLife(3),
            )
        }
    }
    // The same observer installed by an enters trigger, whose own triggering entity is the
    // creature itself: "it" in the delayed effect must still be the damaged creature.
    val breather = card("Test Breather") {
        manaCost = "{1}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.CreateDelayedTrigger(
                trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).isDealtDamage(),
                fireOnce = false,
                expiry = DelayedTriggerExpiry.EndOfTurn,
                effect = Effects.Destroy(EffectTarget.TriggeringEntity),
            )
        }
    }
    val bouncer = card("Test Bouncer") {
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            val creature = target(TargetFilter.Creature)
            effect = Effects.Move(creature, Zone.HAND)
        }
    }
    val pinger = card("Test Pinger") {
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            val any = target(Targets.Any)
            effect = Effects.DealDamage(1, any)
        }
    }

    fun setup(): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver().also {
            it.registerCards(TestCards.all + listOf(observer, watcher, breather, bouncer, pinger))
            it.initMirrorMatch(Deck.of("Island" to 40))
            it.passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        return d to d.activePlayer!!
    }

    fun GameTestDriver.activate(player: EntityId, name: String, target: EntityId? = null) {
        val source = putPermanentOnBattlefield(player, name)
        val ability = cardRegistry.getCard(name)!!.script.activatedAbilities.single().id
        val chosen = target?.let {
            if (it == player || it == getOpponent(player)) ChosenTarget.Player(it) else ChosenTarget.Permanent(it)
        }
        submit(ActivateAbility(player, source, ability, listOfNotNull(chosen))).outcome shouldBe Outcome.Done
        bothPass()
    }

    fun GameTestDriver.ping(player: EntityId, target: EntityId) {
        activate(player, pinger.name, target)
        // The delayed trigger, if it fired, sits on the stack now.
        if (state.stack.isNotEmpty()) bothPass()
    }

    test("a creature an opponent controls is destroyed when it is dealt damage") {
        val (d, player) = setup()
        val opponent = d.getOpponent(player)
        val force = d.putPermanentOnBattlefield(opponent, "Force of Nature")
        d.activate(player, observer.name)
        d.ping(player, force)
        (force in d.getPermanents(opponent)) shouldBe false
        (force in d.getGraveyard(opponent)) shouldBe true
    }

    test("installed by an enters trigger, it destroys the damaged creature, not the creature that entered") {
        val (d, player) = setup()
        val opponent = d.getOpponent(player)
        val force = d.putPermanentOnBattlefield(opponent, "Force of Nature")
        val artifact = d.putCardInHand(player, breather.name)
        d.giveColorlessMana(player, 1)
        d.castSpell(player, artifact).error shouldBe null
        d.bothPass()
        d.bothPass()
        d.state.delayedTriggers.size shouldBe 1
        d.ping(player, force)
        (force in d.getGraveyard(opponent)) shouldBe true
        (artifact in d.getPermanents(player)) shouldBe true
    }

    test("it fires for every matching creature, not just the first") {
        val (d, player) = setup()
        val opponent = d.getOpponent(player)
        val first = d.putPermanentOnBattlefield(opponent, "Force of Nature")
        val second = d.putPermanentOnBattlefield(opponent, "Centaur Courser")
        d.activate(player, observer.name)
        d.ping(player, first)
        d.ping(player, second)
        (first in d.getGraveyard(opponent)) shouldBe true
        (second in d.getGraveyard(opponent)) shouldBe true
    }

    test("the filter scopes the recipient: your own creatures and players don't trigger it") {
        val (d, player) = setup()
        val opponent = d.getOpponent(player)
        val mine = d.putPermanentOnBattlefield(player, "Force of Nature")
        d.activate(player, observer.name)
        d.ping(player, mine)
        d.ping(player, opponent)
        d.state.stack.size shouldBe 0
        (mine in d.getPermanents(player)) shouldBe true
        d.getLifeTotal(opponent) shouldBe 19
    }

    test("it expires at end of turn") {
        val (d, player) = setup()
        val opponent = d.getOpponent(player)
        d.activate(player, observer.name)
        d.state.delayedTriggers.size shouldBe 1
        d.passPriorityUntil(Step.UPKEEP)
        d.state.delayedTriggers shouldBe emptyList()
        val force = d.putPermanentOnBattlefield(opponent, "Force of Nature")
        d.ping(d.activePlayer!!, force)
        (force in d.getPermanents(opponent)) shouldBe true
    }

    test("an entity-scoped watcher fires only for its watched creature, each time it is dealt damage") {
        val (d, player) = setup()
        val watched = d.putPermanentOnBattlefield(player, "Force of Nature")
        val other = d.putPermanentOnBattlefield(player, "Centaur Courser")
        d.activate(player, watcher.name, watched)
        d.ping(player, other)
        d.getLifeTotal(player) shouldBe 20
        d.ping(player, watched)
        d.ping(player, watched)
        d.getLifeTotal(player) shouldBe 26
    }

    test("an entity-scoped watcher ignores the same card back on the battlefield as a new object") {
        val (d, player) = setup()
        val courser = d.putPermanentOnBattlefield(player, "Centaur Courser")
        d.activate(player, watcher.name, courser)
        d.activate(player, bouncer.name, courser)
        d.giveMana(player, Color.GREEN, 1)
        d.giveColorlessMana(player, 2)
        d.castSpell(player, courser).error shouldBe null
        d.bothPass()
        (courser in d.getPermanents(player)) shouldBe true
        d.ping(player, courser)
        d.getLifeTotal(player) shouldBe 20
    }
})
