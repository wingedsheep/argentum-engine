package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.BladedAmbassador
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bladed Ambassador (ONE #5) — {1}{W} 3/1 Creature — Phyrexian Soldier.
 *
 * "This creature enters with an oil counter on it.
 *  {1}, Remove an oil counter from this creature: This creature gains indestructible until end of turn."
 */
class BladedAmbassadorScenarioTest : FunSpec({

    val abilityId = BladedAmbassador.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BladedAmbassador))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun castAmbassador(driver: GameTestDriver): EntityId {
        val player = driver.player1
        val ambassador = driver.putCardInHand(player, "Bladed Ambassador")
        driver.giveMana(player, Color.WHITE, 2)
        driver.castSpell(player, ambassador).error shouldBe null
        driver.bothPass()
        return ambassador
    }

    fun doomBlade(driver: GameTestDriver, target: EntityId) {
        val player = driver.player1
        val blade = driver.putCardInHand(player, "Doom Blade")
        driver.giveMana(player, Color.BLACK, 2)
        driver.castSpell(player, blade, listOf(target)).error shouldBe null
        driver.bothPass()
    }

    fun activate(driver: GameTestDriver, ambassador: EntityId) = ActivateAbility(
        playerId = driver.player1,
        sourceId = ambassador,
        abilityId = abilityId,
        paymentStrategy = PaymentStrategy.FromPool
    )

    test("enters with one oil counter") {
        val driver = newDriver()
        val ambassador = castAmbassador(driver)
        oil(driver, ambassador) shouldBe 1
        driver.state.projectedState.hasKeyword(ambassador, Keyword.INDESTRUCTIBLE) shouldBe false
    }

    test("paying {1} and removing the oil counter makes it indestructible and it survives destruction") {
        val driver = newDriver()
        val ambassador = castAmbassador(driver)

        driver.giveColorlessMana(driver.player1, 1)
        driver.submitSuccess(activate(driver, ambassador))
        oil(driver, ambassador) shouldBe 0
        driver.bothPass()
        driver.state.projectedState.hasKeyword(ambassador, Keyword.INDESTRUCTIBLE) shouldBe true

        doomBlade(driver, ambassador)
        driver.findPermanent(driver.player1, "Bladed Ambassador") shouldBe ambassador

        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.projectedState.hasKeyword(ambassador, Keyword.INDESTRUCTIBLE) shouldBe false
    }

    test("control: without activating, it is destroyed") {
        val driver = newDriver()
        val ambassador = castAmbassador(driver)

        doomBlade(driver, ambassador)
        driver.findPermanent(driver.player1, "Bladed Ambassador") shouldBe null
        driver.getGraveyardCardNames(driver.player1).contains("Bladed Ambassador") shouldBe true
    }

    test("can't be activated a second time once the oil counter is spent") {
        val driver = newDriver()
        val ambassador = castAmbassador(driver)

        driver.giveColorlessMana(driver.player1, 2)
        driver.submitSuccess(activate(driver, ambassador))
        driver.bothPass()
        driver.submitExpectFailure(activate(driver, ambassador))
        oil(driver, ambassador) shouldBe 0
    }
})
