package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.MeldwebStrider
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Meldweb Strider (ONE #60) — {4}{U} Artifact — Vehicle 5/5.
 *
 * "Vigilance
 *  This Vehicle enters with an oil counter on it.
 *  Remove an oil counter from this Vehicle: It becomes an artifact creature until end of turn.
 *  Crew 3"
 */
class MeldwebStriderScenarioTest : FunSpec({

    val animateAbilityId = MeldwebStrider.activatedAbilities.single().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MeldwebStrider))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun castStrider(driver: GameTestDriver): EntityId {
        val me = driver.player1
        val card = driver.putCardInHand(me, "Meldweb Strider")
        driver.giveMana(me, Color.BLUE, 1)
        driver.giveColorlessMana(me, 4)
        driver.castSpell(me, card).error shouldBe null
        driver.bothPass()
        driver.state.getBattlefield().contains(card) shouldBe true
        return card
    }

    test("enters with one oil counter and is not a creature") {
        val driver = newDriver()
        val strider = castStrider(driver)
        oil(driver, strider) shouldBe 1
        driver.state.projectedState.isCreature(strider) shouldBe false
    }

    test("removing the oil counter animates it into a 5/5 vigilant artifact creature for the turn") {
        val driver = newDriver()
        val strider = castStrider(driver)

        driver.submitSuccess(
            ActivateAbility(playerId = driver.player1, sourceId = strider, abilityId = animateAbilityId)
        )
        oil(driver, strider) shouldBe 0
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.isCreature(strider) shouldBe true
        projected.hasType(strider, "ARTIFACT") shouldBe true
        projected.getPower(strider) shouldBe 5
        projected.getToughness(strider) shouldBe 5
        projected.hasKeyword(strider, Keyword.VIGILANCE) shouldBe true

        // Only one oil counter: the ability can't be activated a second time.
        driver.submitExpectFailure(
            ActivateAbility(playerId = driver.player1, sourceId = strider, abilityId = animateAbilityId)
        )

        // The animation ends at end of turn.
        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.projectedState.isCreature(strider) shouldBe false
    }
})
