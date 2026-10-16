package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.snc.cards.ExoticPets
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Exotic Pets (SNC) — {1}{W}{U} Instant.
 *
 * "Create two 1/1 blue Fish creature tokens with 'This token can't be blocked.' Then for each kind
 * of counter among creatures you control, put a counter of that kind on either of those tokens."
 */
class ExoticPetsScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ExoticPets)
        driver.initMirrorMatch(deck = Deck.of("Island" to 30))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.fish(playerId: EntityId): List<EntityId> =
        getCreatures(playerId).filter { state.projectedState.hasSubtype(it, "Fish") }

    fun GameTestDriver.counters(id: EntityId): Map<CounterType, Int> =
        state.getEntity(id)?.get<CountersComponent>()?.counters.orEmpty().filterValues { it > 0 }

    fun GameTestDriver.castPets(playerId: EntityId) {
        giveMana(playerId, Color.WHITE)
        giveMana(playerId, Color.BLUE)
        giveMana(playerId, Color.BLUE)
        castSpell(playerId, putCardInHand(playerId, "Exotic Pets"))
        bothPass()
    }

    test("one counter per kind among your creatures, each on the Fish you pick") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val a = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val b = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        driver.addComponent(a, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2, CounterType.SHIELD to 1)))
        // A second creature with the same kind adds no extra counter: kinds count, not counters.
        driver.addComponent(b, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
        // An opponent's creature's counters aren't "among creatures you control".
        val theirs = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.addComponent(theirs, CountersComponent(mapOf(CounterType.STUN to 1)))

        driver.castPets(me)

        val fish = driver.fish(me)
        fish shouldHaveSize 2
        val (first, second) = fish

        val plusOne = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        plusOne.playerId shouldBe me
        plusOne.prompt shouldBe "Choose a Fish to get a +1/+1 counter"
        plusOne.options shouldContainExactlyInAnyOrder fish
        driver.submitCardSelection(me, listOf(first))

        val shield = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        shield.prompt shouldBe "Choose a Fish to get a shield counter"
        driver.submitCardSelection(me, listOf(first))

        driver.pendingDecision shouldBe null
        driver.counters(first) shouldBe mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1, CounterType.SHIELD to 1)
        driver.counters(second) shouldBe emptyMap()
        driver.state.projectedState.getPower(first) shouldBe 2
    }

    test("the counters can be split between the two Fish") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val a = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        driver.addComponent(a, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1, CounterType.SHIELD to 1)))

        driver.castPets(me)
        val (first, second) = driver.fish(me)

        driver.submitCardSelection(me, listOf(first))
        driver.submitCardSelection(me, listOf(second))

        driver.counters(first) shouldBe mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)
        driver.counters(second) shouldBe mapOf(CounterType.SHIELD to 1)
    }

    test("no counters among your creatures: just two unblockable Fish, no prompt") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Centaur Courser")

        driver.castPets(me)

        driver.pendingDecision shouldBe null
        val fish = driver.fish(me)
        fish shouldHaveSize 2
        fish.forEach {
            driver.counters(it) shouldBe emptyMap()
            driver.state.projectedState.getPower(it) shouldBe 1
        }
    }
})
