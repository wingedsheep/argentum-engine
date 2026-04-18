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
                        DistributedCounterRemoval(piggy, CounterType.PLUS_ONE_PLUS_ONE, 3)
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
                        DistributedCounterRemoval(piggy, CounterType.PLUS_ONE_PLUS_ONE, 2)
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
