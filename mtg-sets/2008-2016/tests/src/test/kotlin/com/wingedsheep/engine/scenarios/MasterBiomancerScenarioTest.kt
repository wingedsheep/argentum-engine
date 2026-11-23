package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gtc.cards.MasterBiomancer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Master Biomancer — "Each other creature you control enters with a number of additional +1/+1
 * counters on it equal to this creature's power and as a Mutant in addition to its other types."
 *
 * The count is the Biomancer's power as the creature enters (ruling 2013-01-24), pumps included.
 */
class MasterBiomancerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MasterBiomancer))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun plusOnes(driver: GameTestDriver, entity: EntityId): Int =
        driver.state.getEntity(entity)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun castBears(driver: GameTestDriver, player: EntityId): EntityId {
        val bear = driver.putCardInHand(player, "Grizzly Bears")
        driver.giveMana(player, Color.GREEN, 2)
        driver.castSpell(player, bear)
        driver.bothPass()
        return driver.getCreatures(player).first {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
        }
    }

    test("another creature enters with counters equal to its power and as a Mutant") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(me, "Master Biomancer")

        val bears = castBears(driver, me)

        plusOnes(driver, bears) shouldBe 2
        driver.state.projectedState.hasSubtype(bears, "Mutant") shouldBe true
        driver.state.projectedState.hasSubtype(bears, "Bear") shouldBe true
    }

    test("the count reads the Biomancer's current power") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val biomancer = driver.putCreatureOnBattlefield(me, "Master Biomancer")
        val growth = driver.putCardInHand(me, "Giant Growth")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, growth, listOf(biomancer))
        driver.bothPass()

        plusOnes(driver, castBears(driver, me)) shouldBe 5
    }

    test("the Biomancer itself enters without counters and is not a Mutant") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val biomancer = driver.putCardInHand(me, "Master Biomancer")
        driver.giveMana(me, Color.GREEN, 2)
        driver.giveMana(me, Color.BLUE, 2)
        driver.castSpell(me, biomancer)
        driver.bothPass()

        plusOnes(driver, biomancer) shouldBe 0
        driver.state.projectedState.hasSubtype(biomancer, "Mutant") shouldBe false
    }

    test("an opponent's creature is unaffected") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.putCreatureOnBattlefield(driver.getOpponent(me), "Master Biomancer")

        val bears = castBears(driver, me)

        plusOnes(driver, bears) shouldBe 0
        driver.state.projectedState.hasSubtype(bears, "Mutant") shouldBe false
    }
})
