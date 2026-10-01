package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.NornsWellspring
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Norn's Wellspring (ONE #24) — {1}{W} Artifact.
 *
 * "Whenever a creature you control dies, scry 1 and put an oil counter on this artifact.
 *  {1}, {T}, Remove two oil counters from this artifact: Draw a card."
 */
class NornsWellspringScenarioTest : FunSpec({

    val abilityId = NornsWellspring.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(NornsWellspring))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun killWithDoomBlade(driver: GameTestDriver, victim: EntityId) {
        val p1 = driver.player1
        val blade = driver.putCardInHand(p1, "Doom Blade")
        driver.giveMana(p1, Color.BLACK, 2)
        driver.castSpell(p1, blade, listOf(victim)).error shouldBe null
        driver.bothPass()
    }

    test("a creature you control dying scries 1 and adds an oil counter") {
        val driver = newDriver()
        val well = driver.putPermanentOnBattlefield(driver.player1, "Norn's Wellspring")
        val bear = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")

        killWithDoomBlade(driver, bear)
        // Trigger on the stack — resolve it.
        driver.bothPass()
        // Scry 1 surfaces a top/bottom decision.
        driver.pendingDecision shouldNotBe null
        while (driver.pendingDecision != null) driver.autoResolveDecision()

        oil(driver, well) shouldBe 1
    }

    test("an opponent's creature dying does not trigger") {
        val driver = newDriver()
        val well = driver.putPermanentOnBattlefield(driver.player1, "Norn's Wellspring")
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")

        killWithDoomBlade(driver, theirs)
        driver.pendingDecision shouldBe null
        driver.state.stack.isEmpty() shouldBe true
        oil(driver, well) shouldBe 0
    }

    test("{1}, {T}, remove two oil counters draws a card") {
        val driver = newDriver()
        val p1 = driver.player1
        val well = driver.putPermanentOnBattlefield(p1, "Norn's Wellspring")
        driver.addComponent(well, CountersComponent(mapOf(CounterType.OIL to 2)))
        val handBefore = driver.getHandSize(p1)

        driver.giveMana(p1, Color.WHITE, 1)
        driver.submitSuccess(ActivateAbility(playerId = p1, sourceId = well, abilityId = abilityId))
        oil(driver, well) shouldBe 0
        driver.state.getEntity(well)?.get<TappedComponent>() shouldNotBe null

        driver.bothPass()
        driver.getHandSize(p1) shouldBe handBefore + 1
    }

    test("can't activate with only one oil counter") {
        val driver = newDriver()
        val p1 = driver.player1
        val well = driver.putPermanentOnBattlefield(p1, "Norn's Wellspring")
        driver.addComponent(well, CountersComponent(mapOf(CounterType.OIL to 1)))

        driver.giveMana(p1, Color.WHITE, 1)
        driver.submitExpectFailure(ActivateAbility(playerId = p1, sourceId = well, abilityId = abilityId))
        oil(driver, well) shouldBe 1
        driver.state.getEntity(well)?.get<TappedComponent>() shouldBe null
    }
})
