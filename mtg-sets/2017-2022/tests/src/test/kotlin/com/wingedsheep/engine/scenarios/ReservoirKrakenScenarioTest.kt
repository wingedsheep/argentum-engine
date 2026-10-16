package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.snc.cards.ReservoirKraken
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Reservoir Kraken (SNC) — {2}{U}{U} 6/6 Kraken, trample, ward {2}.
 *
 * "At the beginning of each combat, if this creature is untapped, any opponent may tap an untapped
 * creature they control. If they do, tap this creature and create a 1/1 blue Fish creature token
 * with 'This token can't be blocked.'"
 */
class ReservoirKrakenScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ReservoirKraken)
        driver.initMirrorMatch(deck = Deck.of("Island" to 30))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.fishTokens(playerId: EntityId): List<EntityId> =
        getCreatures(playerId).filter { state.projectedState.hasSubtype(it, "Fish") }

    test("an opponent taps a creature: the Kraken is tapped and its controller gets an unblockable Fish") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        val kraken = driver.putCreatureOnBattlefield(controller, "Reservoir Kraken")
        driver.putCreatureOnBattlefield(controller, "Centaur Courser") // never offered: only opponents may pay
        val oppCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        val tappedOppCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.tapPermanent(tappedOppCreature)

        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.stackSize shouldBe 1
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.playerId shouldBe opponent
        decision.options shouldContainExactlyInAnyOrder listOf(oppCreature)

        driver.submitCardSelection(opponent, listOf(oppCreature))

        driver.isTapped(oppCreature) shouldBe true
        driver.isTapped(kraken) shouldBe true
        val fish = driver.fishTokens(controller)
        fish shouldHaveSize 1
        driver.state.projectedState.getPower(fish.single()) shouldBe 1
        driver.state.projectedState.getToughness(fish.single()) shouldBe 1
        driver.fishTokens(opponent) shouldHaveSize 0
    }

    test("an opponent declines: nothing happens") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        val kraken = driver.putCreatureOnBattlefield(controller, "Reservoir Kraken")
        val oppCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.stackSize shouldBe 1
        driver.bothPass()

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().playerId shouldBe opponent
        driver.submitCardSelection(opponent, emptyList())

        driver.isTapped(oppCreature) shouldBe false
        driver.isTapped(kraken) shouldBe false
        driver.fishTokens(controller) shouldHaveSize 0
    }

    test("a tapped Kraken doesn't trigger") {
        val driver = createDriver()
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)
        val kraken = driver.putCreatureOnBattlefield(controller, "Reservoir Kraken")
        driver.putCreatureOnBattlefield(controller, "Centaur Courser")
        driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        driver.tapPermanent(kraken)

        driver.passPriorityUntil(Step.BEGIN_COMBAT)

        driver.stackSize shouldBe 0
        driver.pendingDecision shouldBe null
        driver.fishTokens(controller) shouldHaveSize 0
    }
})
