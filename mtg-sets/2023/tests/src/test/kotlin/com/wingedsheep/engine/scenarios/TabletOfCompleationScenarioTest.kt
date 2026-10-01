package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.TabletOfCompleation
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tablet of Compleation (ONE #245): tap for an oil counter; tap for {C} at two or more oil
 * counters; {1},{T} draw at five or more. Each gate is tested on both sides of its threshold.
 */
class TabletOfCompleationScenarioTest : FunSpec({

    val addOil = TabletOfCompleation.activatedAbilities[0].id
    val addMana = TabletOfCompleation.activatedAbilities[1].id
    val draw = TabletOfCompleation.activatedAbilities[2].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TabletOfCompleation))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.oil(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun GameTestDriver.colorless(player: EntityId): Int =
        state.getEntity(player)?.get<ManaPoolComponent>()?.colorless ?: 0

    test("tapping puts an oil counter on it via the stack") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val tablet = driver.putPermanentOnBattlefield(player, "Tablet of Compleation")

        driver.submit(ActivateAbility(playerId = player, sourceId = tablet, abilityId = addOil)).error shouldBe null
        driver.bothPass()

        driver.oil(tablet) shouldBe 1
        driver.isTapped(tablet) shouldBe true
    }

    test("the mana ability needs two oil counters") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val tablet = driver.putPermanentOnBattlefield(player, "Tablet of Compleation")

        driver.addComponent(tablet, CountersComponent(mapOf(CounterType.OIL to 1)))
        val tooEarly = driver.submit(ActivateAbility(playerId = player, sourceId = tablet, abilityId = addMana))
        withClue("one oil counter is not two") {
            tooEarly.error shouldNotBe null
            driver.isTapped(tablet) shouldBe false
            driver.colorless(player) shouldBe 0
        }

        driver.addComponent(tablet, CountersComponent(mapOf(CounterType.OIL to 2)))
        driver.submit(ActivateAbility(playerId = player, sourceId = tablet, abilityId = addMana)).error shouldBe null
        withClue("mana ability resolves immediately") {
            driver.stackSize shouldBe 0
            driver.colorless(player) shouldBe 1
            driver.isTapped(tablet) shouldBe true
        }
    }

    test("the draw ability needs five oil counters and {1}") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val tablet = driver.putPermanentOnBattlefield(player, "Tablet of Compleation")
        driver.putPermanentOnBattlefield(player, "Island")

        driver.addComponent(tablet, CountersComponent(mapOf(CounterType.OIL to 4)))
        val tooEarly = driver.submit(
            ActivateAbility(playerId = player, sourceId = tablet, abilityId = draw, paymentStrategy = PaymentStrategy.AutoPay)
        )
        withClue("four oil counters is not five") {
            tooEarly.error shouldNotBe null
            driver.isTapped(tablet) shouldBe false
        }

        driver.addComponent(tablet, CountersComponent(mapOf(CounterType.OIL to 5)))
        val handBefore = driver.getHandSize(player)
        driver.submit(
            ActivateAbility(playerId = player, sourceId = tablet, abilityId = draw, paymentStrategy = PaymentStrategy.AutoPay)
        ).error shouldBe null
        driver.bothPass()

        driver.getHandSize(player) shouldBe handBefore + 1
        driver.isTapped(tablet) shouldBe true
        withClue("drawing doesn't spend oil counters") { driver.oil(tablet) shouldBe 5 }
    }
})
