package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.OilGorgerTroll
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Oil-Gorger Troll (ONE #177) — {3}{G}{G} 3/4 Creature — Phyrexian Troll Warrior.
 *
 * "When this creature enters, you gain 3 life. Then if you control a permanent with an oil counter
 *  on it, draw a card."
 *
 * Proof card for [CounterType.OIL] as a battlefield filter: the draw is gated on *you* controlling
 * a permanent carrying oil, and the life gain happens regardless.
 */
class OilGorgerTrollScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(OilGorgerTroll))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun giveOil(driver: GameTestDriver, id: EntityId) =
        driver.addComponent(id, CountersComponent(mapOf(CounterType.OIL to 1)))

    /** Casts the Troll and resolves it plus its enters trigger; returns (life gained, cards drawn). */
    fun castTroll(driver: GameTestDriver): Pair<Int, Int> {
        val player = driver.player1
        val troll = driver.putCardInHand(player, "Oil-Gorger Troll")
        driver.giveMana(player, Color.GREEN, 5)
        val life = driver.getLifeTotal(player)
        val hand = driver.getHandSize(player)
        driver.castSpell(player, troll).error shouldBe null
        driver.bothPass() // Troll resolves
        driver.bothPass() // enters trigger resolves
        return (driver.getLifeTotal(player) - life) to (driver.getHandSize(player) - (hand - 1))
    }

    test("with an oil counter on a permanent you control: gain 3 and draw a card") {
        val driver = newDriver()
        giveOil(driver, driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears"))
        castTroll(driver) shouldBe (3 to 1)
    }

    test("without one: gain 3, no draw") {
        val driver = newDriver()
        driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        castTroll(driver) shouldBe (3 to 0)
    }

    test("an opponent's oil permanent doesn't count") {
        val driver = newDriver()
        giveOil(driver, driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears"))
        castTroll(driver) shouldBe (3 to 0)
    }
})
