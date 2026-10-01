package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.PredationSteward
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Predation Steward (ONE #180) — {1}{G} 2/2 Creature — Phyrexian Elf Warrior.
 *
 * "This creature enters with two oil counters on it.
 *  {2}{G}, {T}, Remove an oil counter from this creature: Target creature gets +2/+2 until end of turn.
 *  Activate only as a sorcery."
 */
class PredationStewardScenarioTest : FunSpec({

    val abilityId = PredationSteward.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(PredationSteward))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun castSteward(driver: GameTestDriver): EntityId {
        val p1 = driver.player1
        val steward = driver.putCardInHand(p1, "Predation Steward")
        driver.giveMana(p1, Color.GREEN, 2)
        driver.castSpell(p1, steward).error shouldBe null
        driver.bothPass()
        return steward
    }

    fun activate(driver: GameTestDriver, steward: EntityId, target: EntityId) =
        ActivateAbility(
            playerId = driver.player1,
            sourceId = steward,
            abilityId = abilityId,
            targets = listOf(ChosenTarget.Permanent(target)),
        )

    test("enters with two oil counters") {
        val driver = newDriver()
        val steward = castSteward(driver)
        oil(driver, steward) shouldBe 2
    }

    test("activating removes an oil counter, taps it, and gives target creature +2/+2 until end of turn") {
        val driver = newDriver()
        val p1 = driver.player1
        val steward = castSteward(driver)
        val bears = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        driver.removeSummoningSickness(steward)

        driver.giveMana(p1, Color.GREEN, 3)
        driver.submitSuccess(activate(driver, steward, bears))
        oil(driver, steward) shouldBe 1
        driver.state.getEntity(steward)?.get<TappedComponent>() shouldNotBe null

        driver.bothPass()
        driver.state.projectedState.getPower(bears) shouldBe 4
        driver.state.projectedState.getToughness(bears) shouldBe 4

        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.projectedState.getPower(bears) shouldBe 2
        driver.state.projectedState.getToughness(bears) shouldBe 2
    }

    test("can't be activated without an oil counter") {
        val driver = newDriver()
        val p1 = driver.player1
        val steward = driver.putCreatureOnBattlefield(p1, "Predation Steward")
        driver.removeSummoningSickness(steward)
        oil(driver, steward) shouldBe 0

        driver.giveMana(p1, Color.GREEN, 3)
        driver.submitExpectFailure(activate(driver, steward, steward))
    }

    test("activate only as a sorcery") {
        val driver = newDriver()
        val p1 = driver.player1
        val steward = castSteward(driver)
        driver.removeSummoningSickness(steward)

        val bears = driver.putCardInHand(p1, "Grizzly Bears")
        driver.giveMana(p1, Color.GREEN, 2)
        driver.castSpell(p1, bears).error shouldBe null
        driver.giveMana(p1, Color.GREEN, 3)
        driver.submitExpectFailure(activate(driver, steward, steward))
        oil(driver, steward) shouldBe 2
    }
})
