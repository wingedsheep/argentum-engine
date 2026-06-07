package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tmp.cards.FoolsTome
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Fool's Tome.
 *
 * Fool's Tome
 * {4}
 * Artifact — Book
 * {2}, {T}: Draw a card. Activate only if you have no cards in hand.
 */
class FoolsTomeScenarioTest : FunSpec({

    val abilityId = FoolsTome.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(FoolsTome)
        return driver
    }

    fun emptyHand(driver: GameTestDriver, playerId: EntityId) {
        val handZone = ZoneKey(playerId, Zone.HAND)
        var state = driver.state
        driver.getHand(playerId).toList().forEach { card ->
            state = state.removeFromZone(handZone, card)
        }
        driver.replaceState(state)
    }

    test("draw a card when hand is empty") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val tome = driver.putPermanentOnBattlefield(activePlayer, "Fool's Tome")
        emptyHand(driver, activePlayer)
        driver.getHandSize(activePlayer) shouldBe 0

        driver.giveMana(activePlayer, Color.GREEN, 2)

        val result = driver.submit(
            ActivateAbility(playerId = activePlayer, sourceId = tome, abilityId = abilityId)
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        driver.getHandSize(activePlayer) shouldBe 1
        driver.isTapped(tome) shouldBe true
    }

    test("cannot activate while holding cards in hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val tome = driver.putPermanentOnBattlefield(activePlayer, "Fool's Tome")
        // Opening hand of 7 is still present.
        driver.giveMana(activePlayer, Color.GREEN, 2)

        val result = driver.submit(
            ActivateAbility(playerId = activePlayer, sourceId = tome, abilityId = abilityId)
        )
        result.isSuccess shouldBe false
    }

    test("cannot activate without enough mana") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val tome = driver.putPermanentOnBattlefield(activePlayer, "Fool's Tome")
        emptyHand(driver, activePlayer)
        // Only 1 mana available (need {2}).
        driver.giveMana(activePlayer, Color.GREEN, 1)

        val result = driver.submit(
            ActivateAbility(playerId = activePlayer, sourceId = tome, abilityId = abilityId)
        )
        result.isSuccess shouldBe false
    }
})
