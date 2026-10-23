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
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * An event-based delayed trigger watching one object ("when that creature dies") follows that
 * object only: the same card back on the battlefield after a zone change is a new object
 * (CR 400.7), and a watched target that resolves to nothing creates no trigger at all.
 */
class DelayedTriggerWatchedObjectTest : FunSpec({
    val watcher = card("Test Death Watcher") {
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            val creature = target(TargetFilter.Creature)
            effect = Effects.CreateDelayedTrigger(
                trigger = Triggers.self.dies(),
                watchedTarget = creature,
                fireOnce = true,
                expiry = DelayedTriggerExpiry.Never,
                effect = Effects.GainLife(DynamicAmount.Fixed(5)),
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
    val emptyWatcher = card("Test Empty Watcher") {
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.Pipeline {
                val creatures = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.Each, GameObjectFilter.Creature))
                val chosen = chooseExactly(1, from = creatures)
                run(Effects.CreateDelayedTrigger(
                    trigger = Triggers.self.dies(),
                    watchedTarget = chosen.asTarget(0),
                    fireOnce = true,
                    expiry = DelayedTriggerExpiry.Never,
                    effect = Effects.GainLife(DynamicAmount.Fixed(5)),
                ))
            }
        }
    }

    fun setup(): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver().also {
            it.registerCards(TestCards.all + listOf(watcher, bouncer, emptyWatcher))
            it.initMirrorMatch(Deck.of("Island" to 40))
            it.passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        return d to d.activePlayer!!
    }

    fun GameTestDriver.activate(player: EntityId, name: String, target: EntityId? = null) {
        val source = putPermanentOnBattlefield(player, name)
        val ability = cardRegistry.getCard(name)!!.script.activatedAbilities.single().id
        submit(ActivateAbility(player, source, ability, listOfNotNull(target?.let { ChosenTarget.Permanent(it) }))).outcome shouldBe Outcome.Done
        bothPass()
    }

    fun GameTestDriver.kill(player: EntityId, creature: EntityId) {
        val blade = putCardInHand(player, "Doom Blade")
        giveMana(player, Color.BLACK, 1)
        giveColorlessMana(player, 1)
        castSpell(player, blade, listOf(creature))
        bothPass()
    }

    test("the watched creature dying fires the trigger") {
        val (d, player) = setup()
        val courser = d.putPermanentOnBattlefield(player, "Centaur Courser")
        d.activate(player, watcher.name, courser)
        d.state.delayedTriggers.size shouldBe 1
        d.kill(player, courser)
        d.state.stack.size shouldBe 1
        d.bothPass()
        d.getLifeTotal(player) shouldBe 25
    }

    test("the same card back on the battlefield is a new object the trigger does not watch") {
        val (d, player) = setup()
        val courser = d.putPermanentOnBattlefield(player, "Centaur Courser")
        d.activate(player, watcher.name, courser)
        d.activate(player, bouncer.name, courser)
        (courser in d.getHand(player)) shouldBe true
        d.giveMana(player, Color.GREEN, 1)
        d.giveColorlessMana(player, 2)
        d.castSpell(player, courser, emptyList())
        d.bothPass()
        (courser in d.getPermanents(player)) shouldBe true
        d.kill(player, courser)
        d.state.stack.size shouldBe 0
        d.getLifeTotal(player) shouldBe 20
    }

    test("a watcher whose object changed zones is dropped at cleanup") {
        val (d, player) = setup()
        val courser = d.putPermanentOnBattlefield(player, "Centaur Courser")
        d.activate(player, watcher.name, courser)
        d.activate(player, bouncer.name, courser)
        d.state.delayedTriggers.size shouldBe 1
        d.passPriorityUntil(Step.UPKEEP)
        d.state.delayedTriggers shouldBe emptyList()
    }

    test("a watched target that resolves to nothing creates no trigger") {
        val (d, player) = setup()
        val before = d.state.delayedTriggers
        d.activate(player, emptyWatcher.name)
        d.state.delayedTriggers shouldBe before
    }
})
