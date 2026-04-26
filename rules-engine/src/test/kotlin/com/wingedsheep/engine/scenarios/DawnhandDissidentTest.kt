package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.support.setupP1
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.DawnhandDissident
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.DistributedCounterRemoval
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * End-to-end checks for Dawnhand Dissident's third ability:
 * "During your turn, you may cast creature spells from among cards exiled with
 *  this creature by removing three counters from among creatures you control
 *  in addition to paying their other costs."
 *
 * The first two abilities (Blight 1: Surveil, Blight 2: exile-from-graveyard) use
 * existing atomic primitives and are not re-tested here.
 */
class DawnhandDissidentTest : FunSpec({

    /**
     * Build a state where Player 1 controls Dawnhand Dissident (linked to an
     * exiled creature card) and a piggy-bank creature with [piggyCounters]
     * counters. Advances to [atStep] for Player 1.
     */
    fun setupLinkedExileScenario(
        piggyCounters: Int,
        piggyCounterType: CounterType = CounterType.PLUS_ONE_PLUS_ONE,
        atStep: Step = Step.PRECOMBAT_MAIN
    ): Triple<com.wingedsheep.engine.legalactions.support.EnumerationTestDriver, EntityId, EntityId> {
        val driver = setupP1(
            battlefield = listOf("Dawnhand Dissident", "Grizzly Bears", "Forest", "Forest", "Forest"),
            exile = listOf("Grizzly Bears"),
            extraSetCards = listOf(DawnhandDissident),
            atStep = atStep
        )
        val state = driver.game.state
        val p1 = driver.player1

        val dissident = state.getZone(ZoneKey(p1, Zone.BATTLEFIELD))
            .firstOrNull { state.getEntity(it)?.get<CardComponent>()?.name == "Dawnhand Dissident" }
            ?: error("Dawnhand Dissident not on battlefield")
        val piggy = state.getZone(ZoneKey(p1, Zone.BATTLEFIELD))
            .firstOrNull { state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }
            ?: error("Grizzly Bears not on battlefield")
        val exiledCreature = state.getZone(ZoneKey(p1, Zone.EXILE))
            .firstOrNull { state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }
            ?: error("Grizzly Bears not in exile")

        // Link the exiled card to the Dissident
        var newState = state.updateEntity(dissident) { c ->
            c.with(LinkedExileComponent(listOf(exiledCreature)))
        }
        // Put counters on the piggy creature
        if (piggyCounters > 0) {
            newState = newState.updateEntity(piggy) { c ->
                c.with(CountersComponent(mapOf(piggyCounterType to piggyCounters)))
            }
        }
        driver.game.replaceState(newState)

        return Triple(driver, dissident, exiledCreature)
    }

    test("linked-exile cast surfaces during your turn with sufficient counters") {
        val (driver, _, exiledCreature) = setupLinkedExileScenario(piggyCounters = 3)

        val actions = driver.enumerateFor(driver.player1)
        val exileCasts = actions.castActions().filter { (it.action as CastSpell).cardId == exiledCreature }
        exileCasts shouldHaveSize 1

        val cast = exileCasts.single()
        cast.sourceZone shouldBe "EXILE"
        cast.affordable shouldBe true
        cast.additionalCostInfo shouldNotBe null
        val info = cast.additionalCostInfo!!
        info.costType shouldBe "RemoveCountersFromYourCreatures"
        info.distributedCounterRemovalTotal shouldBe 3
        info.counterRemovalCreatures shouldHaveSize 1
    }

    test("linked-exile cast is unaffordable without enough counters") {
        val (driver, _, exiledCreature) = setupLinkedExileScenario(piggyCounters = 2)

        val actions = driver.enumerateFor(driver.player1)
        val exileCasts = actions.castActions().filter { (it.action as CastSpell).cardId == exiledCreature }
        exileCasts shouldHaveSize 1

        val cast = exileCasts.single()
        cast.sourceZone shouldBe "EXILE"
        cast.affordable shouldBe false
        cast.additionalCostInfo shouldNotBe null
        cast.additionalCostInfo!!.costType shouldBe "RemoveCountersFromYourCreatures"
    }

    test("casting from linked exile spends three counters from creatures you control") {
        val (driver, _, exiledCreature) = setupLinkedExileScenario(piggyCounters = 4)
        val p1 = driver.player1
        val piggy = driver.game.state.getZone(ZoneKey(p1, Zone.BATTLEFIELD))
            .first { driver.game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }

        val result = driver.game.submit(
            CastSpell(
                playerId = p1,
                cardId = exiledCreature,
                paymentStrategy = PaymentStrategy.AutoPay,
                additionalCostPayment = AdditionalCostPayment(
                    distributedCounterRemovals = listOf(
                        DistributedCounterRemoval(piggy, "+1/+1", 3)
                    )
                )
            )
        )
        result.isSuccess shouldBe true

        // Grizzly Bears should now have 1 +1/+1 counter remaining (4 - 3)
        val remaining = driver.game.state.getEntity(piggy)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE)
        remaining shouldBe 1
    }

    test("casting from linked exile fails if fewer than three counters are supplied") {
        val (driver, _, exiledCreature) = setupLinkedExileScenario(piggyCounters = 4)
        val p1 = driver.player1
        val piggy = driver.game.state.getZone(ZoneKey(p1, Zone.BATTLEFIELD))
            .first { driver.game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }

        val result = driver.game.submit(
            CastSpell(
                playerId = p1,
                cardId = exiledCreature,
                paymentStrategy = PaymentStrategy.AutoPay,
                additionalCostPayment = AdditionalCostPayment(
                    distributedCounterRemovals = listOf(
                        DistributedCounterRemoval(piggy, "+1/+1", 2)
                    )
                )
            )
        )
        result.isSuccess shouldBe false
    }

    test("linked-exile cast is unavailable for cards owned by opponent") {
        // Blight 2 can legally exile an opponent's graveyard card (strips their
        // deck), but "you may cast creature spells from among cards you own"
        // gates the reanimation to the granter's own cards only.
        val (driver, _, exiledCreature) = setupLinkedExileScenario(piggyCounters = 3)

        // Reassign ownership of the exiled card to P2 to simulate "exiled from
        // opponent's graveyard". The scenario helper places the card in P1's
        // exile but leaves default ownership — we override it explicitly here.
        val state = driver.game.state
        val newState = state.updateEntity(exiledCreature) { container ->
            val card = container.get<CardComponent>() ?: return@updateEntity container
            container.with(card.copy(ownerId = driver.player2))
        }
        driver.game.replaceState(newState)

        val actions = driver.enumerateFor(driver.player1)
        val exileCasts = actions.castActions().filter { (it.action as CastSpell).cardId == exiledCreature }
        exileCasts.shouldBeEmpty()
    }

    test("linked-exile cast pays counters distributed across two creatures (-1/-1)") {
        // User-reported bug: two creatures each carrying two -1/-1 counters
        // (4 total). Removing 3 counters split across the pair should pay the
        // additional cost. Hill Giants (3/3) survive the counters as 1/1s.
        val driver = setupP1(
            battlefield = listOf("Dawnhand Dissident", "Hill Giant", "Hill Giant", "Forest", "Forest", "Forest"),
            exile = listOf("Grizzly Bears"),
            extraSetCards = listOf(DawnhandDissident),
        )
        val state0 = driver.game.state
        val p1 = driver.player1

        val dissident = state0.getZone(ZoneKey(p1, Zone.BATTLEFIELD))
            .first { state0.getEntity(it)?.get<CardComponent>()?.name == "Dawnhand Dissident" }
        val bears = state0.getZone(ZoneKey(p1, Zone.BATTLEFIELD))
            .filter { state0.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant" }
        bears.size shouldBe 2
        val bear1 = bears[0]
        val bear2 = bears[1]
        val exiledCreature = state0.getZone(ZoneKey(p1, Zone.EXILE))
            .first { state0.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }

        var newState = state0.updateEntity(dissident) { c ->
            c.with(LinkedExileComponent(listOf(exiledCreature)))
        }
        newState = newState.updateEntity(bear1) { c ->
            c.with(CountersComponent(mapOf(CounterType.MINUS_ONE_MINUS_ONE to 2)))
        }
        newState = newState.updateEntity(bear2) { c ->
            c.with(CountersComponent(mapOf(CounterType.MINUS_ONE_MINUS_ONE to 2)))
        }
        driver.game.replaceState(newState)

        // 1) Engine surfaces the cast as affordable with 4 counters available.
        val actions = driver.enumerateFor(p1)
        val cast = actions.castActions().single { (it.action as CastSpell).cardId == exiledCreature }
        cast.affordable shouldBe true
        cast.additionalCostInfo!!.counterRemovalCreatures shouldHaveSize 2

        // 2) Casting succeeds with the typed distribution payload.
        val typedResult = driver.game.submit(
            CastSpell(
                playerId = p1,
                cardId = exiledCreature,
                paymentStrategy = PaymentStrategy.AutoPay,
                additionalCostPayment = AdditionalCostPayment(
                    distributedCounterRemovals = listOf(
                        DistributedCounterRemoval(bear1, "-1/-1", 2),
                        DistributedCounterRemoval(bear2, "-1/-1", 1),
                    )
                )
            )
        )
        typedResult.isSuccess shouldBe true
        driver.game.state.getEntity(bear1)?.get<CountersComponent>()
            ?.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 0
        driver.game.state.getEntity(bear2)?.get<CountersComponent>()
            ?.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 1
    }

    test("linked-exile cast picks counter type when a creature has multiple types") {
        // Multi-type pay: a 4/4 with both +1/+1 and stun counters lets the
        // player decide which to remove (Scryfall ruling on
        // RemoveCountersFromYourCreatures — controller chooses). Engine must
        // honour the typed entries verbatim, not auto-pick.
        val driver = setupP1(
            battlefield = listOf("Dawnhand Dissident", "Hill Giant", "Forest", "Forest", "Forest"),
            exile = listOf("Grizzly Bears"),
            extraSetCards = listOf(DawnhandDissident),
        )
        val state0 = driver.game.state
        val p1 = driver.player1

        val dissident = state0.getZone(ZoneKey(p1, Zone.BATTLEFIELD))
            .first { state0.getEntity(it)?.get<CardComponent>()?.name == "Dawnhand Dissident" }
        val giant = state0.getZone(ZoneKey(p1, Zone.BATTLEFIELD))
            .first { state0.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant" }
        val exiledCreature = state0.getZone(ZoneKey(p1, Zone.EXILE))
            .first { state0.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }

        var newState = state0.updateEntity(dissident) { c ->
            c.with(LinkedExileComponent(listOf(exiledCreature)))
        }
        newState = newState.updateEntity(giant) { c ->
            c.with(CountersComponent(mapOf(
                CounterType.PLUS_ONE_PLUS_ONE to 2,
                CounterType.STUN to 2,
            )))
        }
        driver.game.replaceState(newState)

        // Engine surfaces a per-type breakdown so the UI can offer +/- per type.
        val actions = driver.enumerateFor(p1)
        val cast = actions.castActions().single { (it.action as CastSpell).cardId == exiledCreature }
        val info = cast.additionalCostInfo!!.counterRemovalCreatures.single()
        info.availableCounters shouldBe 4
        info.availableCountersByType.keys shouldBe setOf("+1/+1", "stun")
        info.availableCountersByType["+1/+1"] shouldBe 2
        info.availableCountersByType["stun"] shouldBe 2

        // Player chooses to remove 1 +1/+1 and 2 stun (preserves the +1/+1 buff
        // they care about). Engine honours the typed entries.
        val result = driver.game.submit(
            CastSpell(
                playerId = p1,
                cardId = exiledCreature,
                paymentStrategy = PaymentStrategy.AutoPay,
                additionalCostPayment = AdditionalCostPayment(
                    distributedCounterRemovals = listOf(
                        DistributedCounterRemoval(giant, "+1/+1", 1),
                        DistributedCounterRemoval(giant, "stun", 2),
                    )
                )
            )
        )
        result.isSuccess shouldBe true
        val countersAfter = driver.game.state.getEntity(giant)?.get<CountersComponent>()
        countersAfter?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        countersAfter?.getCount(CounterType.STUN) shouldBe 0
    }

    test("linked-exile cast rejects legacy counterRemovals payload (typed-only)") {
        // CastSpell flow no longer honours the legacy `counterRemovals: Map<EntityId, Int>`
        // payload — clients must send the typed `distributedCounterRemovals`. This guards
        // the engine against silently auto-picking counter types on the player's behalf.
        val driver = setupP1(
            battlefield = listOf("Dawnhand Dissident", "Hill Giant", "Hill Giant", "Forest", "Forest", "Forest"),
            exile = listOf("Grizzly Bears"),
            extraSetCards = listOf(DawnhandDissident),
        )
        val state0 = driver.game.state
        val p1 = driver.player1

        val dissident = state0.getZone(ZoneKey(p1, Zone.BATTLEFIELD))
            .first { state0.getEntity(it)?.get<CardComponent>()?.name == "Dawnhand Dissident" }
        val giants = state0.getZone(ZoneKey(p1, Zone.BATTLEFIELD))
            .filter { state0.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant" }
        val exiledCreature = state0.getZone(ZoneKey(p1, Zone.EXILE))
            .first { state0.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }

        var newState = state0.updateEntity(dissident) { c ->
            c.with(LinkedExileComponent(listOf(exiledCreature)))
        }
        for (g in giants) {
            newState = newState.updateEntity(g) { c ->
                c.with(CountersComponent(mapOf(CounterType.MINUS_ONE_MINUS_ONE to 2)))
            }
        }
        driver.game.replaceState(newState)

        val result = driver.game.submit(
            CastSpell(
                playerId = p1,
                cardId = exiledCreature,
                paymentStrategy = PaymentStrategy.AutoPay,
                additionalCostPayment = AdditionalCostPayment(
                    counterRemovals = mapOf(giants[0] to 2, giants[1] to 1)
                )
            )
        )
        result.isSuccess shouldBe false
    }

    test("linked-exile cast is unavailable on opponent's turn") {
        // Set up on P1's turn. Advance until P2 is the active player.
        val (driver, _, exiledCreature) = setupLinkedExileScenario(piggyCounters = 5)
        val p2 = driver.player2

        // Repeatedly pass priority until the active player flips to P2.
        var guard = 0
        while (driver.game.state.activePlayerId != p2 && guard < 200) {
            driver.game.passPriorityUntil(Step.CLEANUP)
            // Cleanup is followed by next turn's Untap for the other player.
            driver.game.passPriorityUntil(Step.PRECOMBAT_MAIN)
            guard++
        }
        driver.game.state.activePlayerId shouldBe p2

        // Now it's Player 2's turn; Player 1's Dawnhand Dissident cannot use its
        // during-your-turn permission to cast the linked-exile card.
        val actions = driver.enumerateFor(driver.player1)
        val exileCasts = actions.castActions().filter { (it.action as CastSpell).cardId == exiledCreature }
        exileCasts.shouldBeEmpty()
    }
})
