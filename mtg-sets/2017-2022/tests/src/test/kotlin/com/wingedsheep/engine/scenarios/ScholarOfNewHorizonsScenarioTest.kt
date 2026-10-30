package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.brc.cards.ScholarOfNewHorizons
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.DistributedCounterRemoval
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scholar of New Horizons (BRC #6).
 *
 * "{T}, Remove a counter from a permanent you control: Search your library for a Plains card and
 * reveal it. If an opponent controls more lands than you, you may put that card onto the
 * battlefield tapped. If you don't put the card onto the battlefield, put it into your hand. Then
 * shuffle."
 *
 * Proven:
 *  - the cost takes a counter of any kind from another permanent you control;
 *  - behind on lands, the found Plains may go onto the battlefield tapped;
 *  - behind on lands, declining puts it into hand (2022-10-14 ruling);
 *  - not behind on lands, there is no battlefield offer — the Plains goes straight to hand.
 */
class ScholarOfNewHorizonsScenarioTest : FunSpec({

    val abilityId = ScholarOfNewHorizons.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ScholarOfNewHorizons)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    fun GameTestDriver.giveCounters(id: EntityId, type: CounterType, count: Int) {
        replaceState(state.updateEntity(id) { c -> c.with(CountersComponent(mapOf(type to count))) })
    }

    fun counters(driver: GameTestDriver, id: EntityId, type: CounterType): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.counters?.get(type) ?: 0

    /** Board: the Scholar (no counters) and a Savannah Lions holding one charge counter. */
    fun GameTestDriver.setUp(opponentLands: Int): Triple<EntityId, EntityId, EntityId> {
        val you = activePlayer!!
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        val scholar = putCreatureOnBattlefield(you, "Scholar of New Horizons")
        removeSummoningSickness(scholar)
        val lions = putCreatureOnBattlefield(you, "Savannah Lions")
        giveCounters(lions, CounterType.CHARGE, 1)
        repeat(opponentLands) { putLandOnBattlefield(getOpponent(you), "Plains") }
        return Triple(you, scholar, lions)
    }

    fun GameTestDriver.activateFrom(you: EntityId, scholar: EntityId, from: EntityId) {
        val result = submit(
            ActivateAbility(
                playerId = you,
                sourceId = scholar,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(
                    distributedCounterRemovals = listOf(DistributedCounterRemoval(from, CounterType.CHARGE.printed, 1))
                )
            )
        )
        result.outcome shouldBe Outcome.Done
        bothPass()
    }

    /** Answer the library search by picking one Plains; returns it. */
    fun GameTestDriver.findPlains(you: EntityId): EntityId {
        val search = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        val plains = search.options.first()
        submitCardSelection(you, listOf(plains))
        return plains
    }

    test("behind on lands: the found Plains may go onto the battlefield tapped") {
        val driver = newDriver()
        val (you, scholar, lions) = driver.setUp(opponentLands = 2)
        val handBefore = driver.getHandSize(you)

        driver.activateFrom(you, scholar, lions)
        counters(driver, lions, CounterType.CHARGE) shouldBe 0

        val plains = driver.findPlains(you)
        val offer = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        offer.options shouldBe listOf(plains)
        driver.submitCardSelection(you, listOf(plains))

        driver.getLands(you) shouldContain plains
        driver.isTapped(plains) shouldBe true
        driver.getHandSize(you) shouldBe handBefore
    }

    test("behind on lands: declining the offer puts the Plains into hand") {
        val driver = newDriver()
        val (you, scholar, lions) = driver.setUp(opponentLands = 2)

        driver.activateFrom(you, scholar, lions)
        val plains = driver.findPlains(you)
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(you, emptyList())

        driver.getHand(you) shouldContain plains
        driver.getLands(you) shouldNotContain plains
    }

    test("not behind on lands: no battlefield offer, the Plains goes to hand") {
        val driver = newDriver()
        val (you, scholar, lions) = driver.setUp(opponentLands = 0)

        driver.activateFrom(you, scholar, lions)
        val plains = driver.findPlains(you)

        driver.pendingDecision shouldBe null
        driver.getHand(you) shouldContain plains
        driver.getLands(you) shouldNotContain plains
    }
})
