package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.GlistenerSeer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Glistener Seer (ONE #54) — {U} 0/3 Creature — Phyrexian Advisor.
 *
 * "This creature enters with three oil counters on it.
 *  {T}, Remove an oil counter from this creature: Scry 1."
 *
 * Proof card for [CounterType.OIL]: the counter is placed by an enters-with replacement and spent
 * as part of an activation cost, and the ability can't be activated once the oil runs out.
 */
class GlistenerSeerScenarioTest : FunSpec({

    val abilityId = GlistenerSeer.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GlistenerSeer))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun castSeer(driver: GameTestDriver): EntityId {
        val player = driver.player1
        val seer = driver.putCardInHand(player, "Glistener Seer")
        driver.giveMana(player, Color.BLUE, 1)
        driver.castSpell(player, seer).error shouldBe null
        driver.bothPass()
        return seer
    }

    test("enters with three oil counters") {
        val driver = newDriver()
        val seer = castSeer(driver)
        oil(driver, seer) shouldBe 3
    }

    test("tapping and removing an oil counter scries 1") {
        val driver = newDriver()
        val seer = castSeer(driver)
        driver.removeSummoningSickness(seer)

        driver.submitSuccess(ActivateAbility(playerId = driver.player1, sourceId = seer, abilityId = abilityId))
        oil(driver, seer) shouldBe 2
        driver.state.getEntity(seer)?.get<TappedComponent>() shouldNotBe null

        driver.bothPass()
        // Scry 1 surfaces the controller's bottom/top choice.
        driver.pendingDecision shouldNotBe null
        while (driver.pendingDecision != null) driver.autoResolveDecision()
        oil(driver, seer) shouldBe 2
    }

    test("can't be activated without an oil counter") {
        val driver = newDriver()
        val seer = driver.putCreatureOnBattlefield(driver.player1, "Glistener Seer")
        driver.removeSummoningSickness(seer)
        oil(driver, seer) shouldBe 0

        driver.submitExpectFailure(ActivateAbility(playerId = driver.player1, sourceId = seer, abilityId = abilityId))
        driver.state.getEntity(seer)?.get<TappedComponent>() shouldBe null
    }
})
