package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.FontOfProgress
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Font of Progress (ONE #51) — {U} Artifact.
 *
 * "This artifact enters with two oil counters on it.
 *  {3}, {T}: Target player mills X cards, where X is the number of oil counters on this artifact."
 */
class FontOfProgressScenarioTest : FunSpec({

    val abilityId = FontOfProgress.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FontOfProgress))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun castFont(driver: GameTestDriver): EntityId {
        val p1 = driver.player1
        val font = driver.putCardInHand(p1, "Font of Progress")
        driver.giveMana(p1, Color.BLUE, 1)
        driver.castSpell(p1, font).error shouldBe null
        driver.bothPass()
        return font
    }

    test("enters with two oil counters") {
        val driver = newDriver()
        val font = castFont(driver)
        oil(driver, font) shouldBe 2
    }

    test("target opponent mills two cards with the starting two oil counters") {
        val driver = newDriver()
        val p1 = driver.player1
        val p2 = driver.player2
        val font = castFont(driver)
        val gyBefore = driver.getGraveyard(p2).size

        driver.giveColorlessMana(p1, 3)
        driver.submitSuccess(
            ActivateAbility(p1, font, abilityId, targets = listOf(ChosenTarget.Player(p2)))
        )
        driver.state.getEntity(font)?.get<TappedComponent>() shouldNotBe null
        driver.bothPass()

        driver.getGraveyard(p2).size shouldBe gyBefore + 2
        driver.getGraveyard(p1).size shouldBe 0
        oil(driver, font) shouldBe 2
    }

    test("X tracks the current oil-counter count and can target yourself") {
        val driver = newDriver()
        val p1 = driver.player1
        val font = castFont(driver)
        driver.addComponent(font, CountersComponent(mapOf(CounterType.OIL to 5)))

        driver.giveColorlessMana(p1, 3)
        driver.submitSuccess(
            ActivateAbility(p1, font, abilityId, targets = listOf(ChosenTarget.Player(p1)))
        )
        driver.bothPass()

        driver.getGraveyard(p1).size shouldBe 5
    }

    test("can't be activated without three mana") {
        val driver = newDriver()
        val p1 = driver.player1
        val font = castFont(driver)

        driver.giveColorlessMana(p1, 2)
        driver.submitExpectFailure(
            ActivateAbility(p1, font, abilityId, targets = listOf(ChosenTarget.Player(driver.player2)))
        )
    }
})
