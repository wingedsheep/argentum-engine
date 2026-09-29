package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.CountersRemovedFromYourPermanentsThisTurnComponent
import com.wingedsheep.engine.state.components.player.PermanentsWithCountersPutIntoGraveyardThisTurnComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Turn history of counters leaving permanents: [CounterHistory] credits removals at the settle
 * boundary, and the zone-transition funnel records the counters on permanents put into graveyards.
 * Read by `CounterRemovedFromPermanentYouControlledThisTurn` and
 * `PermanentWithCounterPutIntoGraveyardThisTurn` (Churning Reservoir).
 */
class CounterHistoryTest : FunSpec({

    // "{0}, Remove an oil counter from this artifact, Sacrifice it: You gain 1 life." — the counter
    // leaves and the permanent leaves in the same payment.
    val oilFlask = card("Oil Flask") {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Composite(Costs.RemoveCounterFromSelf(CounterType.OIL, 1), Costs.SacrificeSelf)
            effect = Effects.GainLife(1)
        }
    }

    // "Exile target permanent." — leaves the battlefield, but not to a graveyard.
    val banish = card("Banish Permanent") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Permanent)
            effect = Effects.Exile(t)
        }
    }

    fun newDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(oilFlask, banish))
        d.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.giveCounters(id: EntityId, type: CounterType, amount: Int) {
        replaceState(state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        })
    }

    fun GameTestDriver.removed(player: EntityId): Set<CounterType> =
        state.getEntity(player)?.get<CountersRemovedFromYourPermanentsThisTurnComponent>()?.kinds ?: emptySet()

    fun GameTestDriver.graveyarded(player: EntityId): Set<CounterType> =
        state.getEntity(player)?.get<PermanentsWithCountersPutIntoGraveyardThisTurnComponent>()?.kinds ?: emptySet()

    test("a removal is credited to the permanent's controller, not the player who removed it") {
        val d = newDriver()
        val theirs = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val recorded = CounterHistory.recordRemovals(
            d.state, listOf(CountersRemovedEvent(theirs, CounterType.OIL, 1, "Grizzly Bears"))
        )
        recorded.getEntity(d.player2)?.get<CountersRemovedFromYourPermanentsThisTurnComponent>()?.kinds shouldBe
            setOf(CounterType.OIL)
        recorded.getEntity(d.player1)?.get<CountersRemovedFromYourPermanentsThisTurnComponent>() shouldBe null
    }

    test("counters leaving a player or a card off the battlefield aren't removed from a permanent") {
        val d = newDriver()
        val inHand = d.putCardInHand(d.player1, "Grizzly Bears")
        val recorded = CounterHistory.recordRemovals(
            d.state,
            listOf(
                CountersRemovedEvent(d.player1, CounterType.POISON, 1, ""),
                CountersRemovedEvent(inHand, CounterType.TIME, 1, "Grizzly Bears"),
                CountersRemovedEvent(inHand, CounterType.OIL, 0, "Grizzly Bears"),
            )
        )
        d.state.turnOrder.forEach { player ->
            recorded.getEntity(player)?.get<CountersRemovedFromYourPermanentsThisTurnComponent>() shouldBe null
        }
    }

    test("a permanent that left in the same action is credited to its last-known controller") {
        val d = newDriver()
        val gone = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val recorded = CounterHistory.recordRemovals(
            d.state,
            listOf(
                CountersRemovedEvent(gone, CounterType.OIL, 1, "Grizzly Bears"),
                ZoneChangeEvent(
                    gone, "Grizzly Bears", Zone.BATTLEFIELD, Zone.GRAVEYARD, d.player1,
                    lastKnown = EntitySnapshot(entityId = gone, controllerId = d.player2)
                ),
            )
        )
        recorded.getEntity(d.player2)?.get<CountersRemovedFromYourPermanentsThisTurnComponent>()?.kinds shouldBe
            setOf(CounterType.OIL)
    }

    test("removing a counter and sacrificing as one cost records both halves") {
        val d = newDriver()
        val flask = d.putPermanentOnBattlefield(d.player1, "Oil Flask")
        d.giveCounters(flask, CounterType.OIL, 1)

        d.submitSuccess(ActivateAbility(playerId = d.player1, sourceId = flask, abilityId = oilFlask.activatedAbilities.first().id))
        d.assertInGraveyard(d.player1, "Oil Flask")

        d.removed(d.player1) shouldBe setOf(CounterType.OIL)
        // The counter was already gone as it hit the graveyard, so it doesn't count as "with an oil
        // counter on it".
        d.graveyarded(d.player1) shouldBe emptySet()
    }

    test("a permanent put into a graveyard records the kinds it had, credited to its controller") {
        val d = newDriver()
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.giveCounters(bears, CounterType.OIL, 2)
        d.giveCounters(bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, com.wingedsheep.sdk.core.Color.RED, 1)
        d.castSpellWithTargets(d.player1, bolt, listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(bears)))
        d.bothPass()
        d.assertInGraveyard(d.player2, "Grizzly Bears")

        d.graveyarded(d.player2) shouldContainExactlyInAnyOrder setOf(CounterType.OIL, CounterType.PLUS_ONE_PLUS_ONE)
        d.graveyarded(d.player1) shouldBe emptySet()
    }

    test("a permanent exiled with counters isn't put into a graveyard") {
        val d = newDriver()
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.giveCounters(bears, CounterType.OIL, 1)

        val spell = d.putCardInHand(d.player1, "Banish Permanent")
        d.castSpellWithTargets(d.player1, spell, listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(bears)))
        d.bothPass()

        d.graveyarded(d.player2) shouldBe emptySet()
    }

    test("+1/+1 and -1/-1 annihilation (CR 704.5q) is a removal: it emits events and is recorded") {
        val d = newDriver()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.giveCounters(bears, CounterType.PLUS_ONE_PLUS_ONE, 2)
        d.giveCounters(bears, CounterType.MINUS_ONE_MINUS_ONE, 1)

        // Any settled action runs state-based actions.
        val flask = d.putPermanentOnBattlefield(d.player1, "Oil Flask")
        d.giveCounters(flask, CounterType.OIL, 1)
        val result = d.submitSuccess(ActivateAbility(playerId = d.player1, sourceId = flask, abilityId = oilFlask.activatedAbilities.first().id))

        val counters = d.state.getEntity(bears)?.get<CountersComponent>()
        counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        counters?.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 0
        result.events.filterIsInstance<CountersRemovedEvent>().filter { it.entityId == bears }
            .map { it.counterType to it.amount } shouldContainExactlyInAnyOrder
            listOf(CounterType.PLUS_ONE_PLUS_ONE to 1, CounterType.MINUS_ONE_MINUS_ONE to 1)
        d.removed(d.player1) shouldContainExactlyInAnyOrder
            setOf(CounterType.OIL, CounterType.PLUS_ONE_PLUS_ONE, CounterType.MINUS_ONE_MINUS_ONE)
    }

    test("the history is cleared at end of turn") {
        val d = newDriver()
        val flask = d.putPermanentOnBattlefield(d.player1, "Oil Flask")
        d.giveCounters(flask, CounterType.OIL, 1)
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.giveCounters(bears, CounterType.OIL, 1)
        d.submitSuccess(ActivateAbility(playerId = d.player1, sourceId = flask, abilityId = oilFlask.activatedAbilities.first().id))
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, com.wingedsheep.sdk.core.Color.RED, 1)
        d.castSpellWithTargets(d.player1, bolt, listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(bears)))
        d.bothPass()
        d.removed(d.player1) shouldBe setOf(CounterType.OIL)
        d.graveyarded(d.player2) shouldBe setOf(CounterType.OIL)

        d.passPriorityUntil(Step.UPKEEP)
        d.removed(d.player1) shouldBe emptySet()
        d.graveyarded(d.player2) shouldBe emptySet()
    }
})
