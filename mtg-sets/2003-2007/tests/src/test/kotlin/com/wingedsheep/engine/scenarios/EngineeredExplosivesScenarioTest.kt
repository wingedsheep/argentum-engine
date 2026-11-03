package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.`5dn`.cards.EngineeredExplosives
import com.wingedsheep.mtg.sets.definitions.lea.cards.GrizzlyBears
import com.wingedsheep.mtg.sets.definitions.mrd.cards.MarchOfTheMachines
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engineered Explosives (5DN #118, reprinted in TRK) — {X} Artifact.
 *
 *   "Sunburst (This artifact enters with a charge counter on it for each color of mana spent to
 *    cast it.)
 *    {2}, Sacrifice this artifact: Destroy each nonland permanent with mana value equal to the
 *    number of charge counters on this artifact."
 */
class EngineeredExplosivesScenarioTest : FunSpec({

    val detonate = EngineeredExplosives.activatedAbilities[0].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EngineeredExplosives, GrizzlyBears, MarchOfTheMachines))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun counters(driver: GameTestDriver, id: EntityId, type: CounterType): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    test("X=2 paid with red and green enters with two charge counters; detonating destroys mana value 2") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val opp = driver.getOpponent(p)
        val bears = driver.putPermanentOnBattlefield(opp, "Grizzly Bears")
        val lions = driver.putPermanentOnBattlefield(opp, "Savannah Lions")
        val explosives = driver.putCardInHand(p, "Engineered Explosives")
        driver.giveMana(p, Color.RED, 1)
        driver.giveMana(p, Color.GREEN, 1)

        driver.castXSpell(p, explosives, xValue = 2).outcome shouldBe Outcome.Done
        driver.bothPass()
        counters(driver, explosives, CounterType.CHARGE) shouldBe 2

        val forests = List(2) { driver.putPermanentOnBattlefield(p, "Forest") }
        driver.submit(ActivateAbility(playerId = p, sourceId = explosives, abilityId = detonate))
            .outcome shouldBe Outcome.Done
        driver.bothPass()

        // The sacrifice is a cost: the count is the two counters it had as it last existed.
        driver.state.getBattlefield().contains(explosives) shouldBe false
        driver.state.getBattlefield().contains(bears) shouldBe false
        driver.state.getBattlefield().contains(lions) shouldBe true
        forests.all { it in driver.state.getBattlefield() } shouldBe true
    }

    test("X=2 paid with one color enters with one charge counter, not two") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        val explosives = driver.putCardInHand(p, "Engineered Explosives")
        driver.giveMana(p, Color.BLUE, 2)

        driver.castXSpell(p, explosives, xValue = 2).outcome shouldBe Outcome.Done
        driver.bothPass()

        counters(driver, explosives, CounterType.CHARGE) shouldBe 1
    }

    test("under March of the Machines it enters as a creature, so it gets +1/+1 counters instead") {
        val driver = createDriver()
        val p = driver.activePlayer!!
        driver.putPermanentOnBattlefield(p, "March of the Machines")
        val explosives = driver.putCardInHand(p, "Engineered Explosives")
        driver.giveMana(p, Color.RED, 1)

        driver.castXSpell(p, explosives, xValue = 1).outcome shouldBe Outcome.Done
        driver.bothPass()

        // A 0/0 artifact creature (mana value 0) that survives as a 1/1 on its +1/+1 counter.
        driver.state.getBattlefield().contains(explosives) shouldBe true
        counters(driver, explosives, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        counters(driver, explosives, CounterType.CHARGE) shouldBe 0
    }
})
