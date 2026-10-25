package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.enteredBattlefieldTapped
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.core.untapOrConsumeStun
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * "Whenever a permanent you control enters tapped" (`Triggers.a(filter).entersTapped()`, Amulet of
 * Vigor) and the tap-state predicates on zone-change triggers generally.
 *
 * - An arrival matches by its status **as it entered** (CR 110.5b), frozen by the first tap/untap
 *   transition of the visit: a permanent that enters untapped and is then tapped didn't enter tapped
 *   (Amulet of Vigor's ruling), and one that entered tapped and was untapped since still did.
 * - The status belongs to the battlefield object: one that leaves and returns is a new object
 *   (CR 400.7) and its earlier visit's answer is gone.
 * - A departure reads the tap state it left with ("whenever a tapped creature dies").
 */
class EntersTappedTriggerTest : FunSpec({
    val entersTappedWatcher = card("Test Enters Tapped Watcher") {
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Permanent.youControl()).entersTapped()
            effect = Effects.GainLife(1)
        }
    }
    val tappedDiesWatcher = card("Test Tapped Dies Watcher") {
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.a(GameObjectFilter.Creature.tapped()).dies()
            effect = Effects.GainLife(1)
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(entersTappedWatcher, tappedDiesWatcher))
        it.initMirrorMatch(Deck.of("Mountain" to 40))
    }

    fun GameTestDriver.entry(id: EntityId, owner: EntityId = player1) =
        ZoneChangeEvent(id, "Grizzly Bears", fromZone = Zone.HAND, toZone = Zone.BATTLEFIELD, ownerId = owner)

    fun GameTestDriver.enterTapped(id: EntityId) =
        replaceState(state.updateEntity(id) { it.with(TappedComponent) })

    fun GameTestDriver.detect(watcher: EntityId, event: GameEvent) =
        services.triggerDetector.detectTriggers(state, listOf(event)).filter { it.sourceId == watcher }

    test("a permanent entering tapped triggers entersTapped, bound to that permanent") {
        val d = driver()
        val watcher = d.putPermanentOnBattlefield(d.player1, entersTappedWatcher.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.enterTapped(bears)

        val triggers = d.detect(watcher, d.entry(bears))
        triggers.size shouldBe 1
        triggers.single().triggerContext.triggeringEntityId shouldBe bears
    }

    test("a permanent entering untapped does not trigger entersTapped") {
        val d = driver()
        val watcher = d.putPermanentOnBattlefield(d.player1, entersTappedWatcher.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")

        d.detect(watcher, d.entry(bears)).shouldBeEmpty()
    }

    test("entering untapped and becoming tapped afterwards is not entering tapped") {
        val d = driver()
        val watcher = d.putPermanentOnBattlefield(d.player1, entersTappedWatcher.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.replaceState(tap(d.state, bears).state)

        d.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
        d.detect(watcher, d.entry(bears)).shouldBeEmpty()
    }

    test("entering tapped still counts after it is untapped, and a later retap does not change it") {
        val d = driver()
        val watcher = d.putPermanentOnBattlefield(d.player1, entersTappedWatcher.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.enterTapped(bears)
        d.replaceState(untapOrConsumeStun(d.state, bears).first)
        d.detect(watcher, d.entry(bears)).size shouldBe 1

        d.replaceState(tap(d.state, bears).state)
        d.replaceState(untapOrConsumeStun(d.state, bears).first)
        enteredBattlefieldTapped(d.state.getEntity(bears)!!) shouldBe true
    }

    test("an opponent's permanent entering tapped does not trigger a you-control watcher") {
        val d = driver()
        val watcher = d.putPermanentOnBattlefield(d.player1, entersTappedWatcher.name)
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.enterTapped(bears)

        d.detect(watcher, d.entry(bears, owner = d.player2)).shouldBeEmpty()
    }

    test("the entry tap state belongs to the battlefield object and is cleared as it leaves") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.replaceState(tap(d.state, bears).state)
        enteredBattlefieldTapped(d.state.getEntity(bears)!!) shouldBe false

        val stripped = ZoneMovementUtils.stripBattlefieldComponents(d.state.getEntity(bears)!!)
            .with(TappedComponent)
        enteredBattlefieldTapped(stripped) shouldBe true
    }

    test("a dies trigger filtered on tapped reads the tap state the creature left with") {
        val d = driver()
        val watcher = d.putPermanentOnBattlefield(d.player1, tappedDiesWatcher.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        fun death(wasTapped: Boolean) = ZoneChangeEvent(
            bears, "Grizzly Bears", fromZone = Zone.BATTLEFIELD, toZone = Zone.GRAVEYARD, ownerId = d.player1,
            lastKnown = EntitySnapshot(
                entityId = bears, controllerId = d.player1, wasTapped = wasTapped,
                typeLine = TypeLine.parse("Creature — Bear"),
            ),
        )
        d.moveToGraveyard(bears)

        d.services.triggerDetector.detectTriggers(d.state, listOf(death(wasTapped = true)))
            .filter { it.sourceId == watcher }.size shouldBe 1
        d.services.triggerDetector.detectTriggers(d.state, listOf(death(wasTapped = false)))
            .filter { it.sourceId == watcher }.shouldBeEmpty()
    }
})
