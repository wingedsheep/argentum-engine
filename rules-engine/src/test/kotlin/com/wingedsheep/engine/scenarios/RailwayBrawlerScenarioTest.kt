package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.RailwayBrawler
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Railway Brawler — {3}{G}{G} 5/5 Creature — Rhino Warrior
 *
 * Reach, trample
 * "Whenever another creature you control enters, put X +1/+1 counters on it, where X is its power."
 * Plot {3}{G}
 *
 * Verifies the enters-trigger reads the entering creature's power and stacks that many +1/+1
 * counters on the entering creature itself (via EffectTarget.TriggeringEntity).
 */
class RailwayBrawlerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RailwayBrawler))
        return driver
    }

    fun plusCounters(driver: GameTestDriver, entity: EntityId): Int =
        driver.state.getEntity(entity)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("entering 2/1 creature gets two +1/+1 counters (X = its power)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(me, "Railway Brawler")

        // Cast Goblin Guide (2/1). Its ETB triggers Railway Brawler: put 2 counters on the Guide.
        val guide = driver.putCardInHand(me, "Goblin Guide")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, guide)
        driver.bothPass() // resolve Goblin Guide -> Railway Brawler trigger on stack
        driver.bothPass() // resolve trigger

        val guideEntity = driver.findPermanent(me, "Goblin Guide")!!
        plusCounters(driver, guideEntity) shouldBe 2
        // 2/1 base + 2 counters = 4/3
        driver.state.projectedState.getPower(guideEntity) shouldBe 4
        driver.state.projectedState.getToughness(guideEntity) shouldBe 3
    }

    test("entering 5/5 creature gets five +1/+1 counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putCreatureOnBattlefield(me, "Railway Brawler")

        val fon = driver.putCardInHand(me, "Force of Nature")
        driver.giveMana(me, Color.GREEN, 5)
        driver.castSpell(me, fon)
        driver.bothPass()
        driver.bothPass()

        val fonEntity = driver.findPermanent(me, "Force of Nature")!!
        plusCounters(driver, fonEntity) shouldBe 5
    }

    test("Railway Brawler does not put counters on itself (OTHER binding)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val me = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // Cast Railway Brawler itself — its own entry must NOT trigger its OTHER-bound ability.
        val brawler = driver.putCardInHand(me, "Railway Brawler")
        driver.giveMana(me, Color.GREEN, 2)
        driver.giveColorlessMana(me, 3)
        driver.castSpell(me, brawler)
        driver.bothPass()
        driver.passPriorityUntil(Step.END)

        val brawlerEntity = driver.findPermanent(me, "Railway Brawler")!!
        plusCounters(driver, brawlerEntity) shouldBe 0
    }
})
