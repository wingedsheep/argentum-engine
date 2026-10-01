package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.TamiyosImmobilizer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tamiyo's Immobilizer (ONE #69) — {3}{U} Artifact.
 *
 * "This artifact enters with four oil counters on it.
 *  {T}, Remove an oil counter from this artifact: Tap target artifact or creature."
 */
class TamiyosImmobilizerScenarioTest : FunSpec({

    val abilityId = TamiyosImmobilizer.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TamiyosImmobilizer))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun tapped(driver: GameTestDriver, id: EntityId): Boolean =
        driver.state.getEntity(id)?.get<TappedComponent>() != null

    fun castImmobilizer(driver: GameTestDriver): EntityId {
        val player = driver.player1
        val card = driver.putCardInHand(player, "Tamiyo's Immobilizer")
        driver.giveMana(player, Color.BLUE, 4)
        driver.castSpell(player, card).error shouldBe null
        driver.bothPass()
        return card
    }

    test("enters with four oil counters") {
        val driver = newDriver()
        val imm = castImmobilizer(driver)
        oil(driver, imm) shouldBe 4
    }

    test("taps target creature, spending an oil counter") {
        val driver = newDriver()
        val imm = castImmobilizer(driver)
        val bear = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        driver.submitSuccess(
            ActivateAbility(driver.player1, imm, abilityId, targets = listOf(ChosenTarget.Permanent(bear)))
        )
        oil(driver, imm) shouldBe 3
        tapped(driver, imm) shouldBe true
        tapped(driver, bear) shouldBe false

        driver.bothPass()
        tapped(driver, bear) shouldBe true
    }

    test("taps target artifact") {
        val driver = newDriver()
        val imm = castImmobilizer(driver)
        val otherArtifact = driver.putPermanentOnBattlefield(driver.player2, "Tamiyo's Immobilizer")

        driver.submitSuccess(
            ActivateAbility(driver.player1, imm, abilityId, targets = listOf(ChosenTarget.Permanent(otherArtifact)))
        )
        driver.bothPass()
        tapped(driver, otherArtifact) shouldBe true
    }

    test("can't target a land") {
        val driver = newDriver()
        val imm = castImmobilizer(driver)
        val land = driver.putPermanentOnBattlefield(driver.player2, "Island")

        driver.submitExpectFailure(
            ActivateAbility(driver.player1, imm, abilityId, targets = listOf(ChosenTarget.Permanent(land)))
        )
        oil(driver, imm) shouldBe 4
    }

    test("can't be activated without an oil counter") {
        val driver = newDriver()
        val imm = driver.putPermanentOnBattlefield(driver.player1, "Tamiyo's Immobilizer")
        val bear = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        oil(driver, imm) shouldBe 0

        driver.submitExpectFailure(
            ActivateAbility(driver.player1, imm, abilityId, targets = listOf(ChosenTarget.Permanent(bear)))
        )
        tapped(driver, imm) shouldBe false
        tapped(driver, bear) shouldNotBe true
    }
})
