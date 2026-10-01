package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.AxiomEngraver
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Axiom Engraver (ONE #120) — {1}{R} 1/3 Creature — Phyrexian Wizard.
 *
 * Enters with two oil counters; {T}, remove an oil counter, discard a card: draw a card.
 */
class AxiomEngraverScenarioTest : FunSpec({

    val lootId = AxiomEngraver.activatedAbilities[0].id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AxiomEngraver))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun castEngraver(driver: GameTestDriver): EntityId {
        val player = driver.player1
        val engraver = driver.putCardInHand(player, "Axiom Engraver")
        driver.giveMana(player, Color.RED, 2)
        driver.castSpell(player, engraver).error shouldBe null
        driver.bothPass()
        return engraver
    }

    test("enters with two oil counters") {
        val driver = newDriver()
        val engraver = castEngraver(driver)
        oil(driver, engraver) shouldBe 2
    }

    test("tap, remove an oil counter, discard a card: draw a card") {
        val driver = newDriver()
        val player = driver.player1
        val engraver = castEngraver(driver)
        driver.removeSummoningSickness(engraver)
        val toDiscard = driver.putCardInHand(player, "Grizzly Bears")
        val handBefore = driver.getHand(player).size

        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = engraver,
                abilityId = lootId,
                costPayment = AdditionalCostPayment(discardedCards = listOf(toDiscard))
            )
        ).outcome shouldBe Outcome.Done

        driver.isTapped(engraver) shouldBe true
        oil(driver, engraver) shouldBe 1
        driver.state.getGraveyard(player).contains(toDiscard) shouldBe true

        driver.bothPass()
        driver.getHand(player).size shouldBe handBefore
    }

    test("cannot activate without an oil counter") {
        val driver = newDriver()
        val player = driver.player1
        val engraver = driver.putCreatureOnBattlefield(player, "Axiom Engraver")
        driver.removeSummoningSickness(engraver)
        oil(driver, engraver) shouldBe 0
        val toDiscard = driver.putCardInHand(player, "Grizzly Bears")

        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = engraver,
                abilityId = lootId,
                costPayment = AdditionalCostPayment(discardedCards = listOf(toDiscard))
            )
        ).outcome shouldNotBe Outcome.Done
    }
})
