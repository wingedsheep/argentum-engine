package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.cards.ReforgeTheSoul
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Reforge the Soul (AVR #151) — {3}{R}{R} Sorcery
 *
 * "Each player discards their hand, then draws seven cards.
 *  Miracle {1}{R}"
 *
 * A symmetric wheel over `ForEachPlayer(Player.Each)`: the per-player body must rebind to the player
 * being processed, so the check that matters is that *both* players end on seven cards with their old
 * hands in their graveyards — not that the caster wheels twice.
 */
class ReforgeTheSoulScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ReforgeTheSoul)
        return driver
    }

    test("each player discards their hand and draws seven") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        val reforge = driver.putCardInHand(you, "Reforge the Soul")
        val yourOtherCards = driver.getHand(you).filter { it != reforge }
        val theirCards = driver.getHand(opponent)
        yourOtherCards.isNotEmpty() shouldBe true
        theirCards.isNotEmpty() shouldBe true

        driver.giveColorlessMana(you, 3)
        driver.giveMana(you, com.wingedsheep.sdk.core.Color.RED, 2)
        driver.submitSuccess(
            CastSpell(playerId = you, cardId = reforge, paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.bothPass()

        // Both players wheeled.
        driver.getHandSize(you) shouldBe 7
        driver.getHandSize(opponent) shouldBe 7

        // Their old hands were discarded, not shuffled away.
        val yourGraveyard = driver.getGraveyard(you)
        yourOtherCards.all { it in yourGraveyard } shouldBe true
        val theirGraveyard = driver.getGraveyard(opponent)
        theirCards.all { it in theirGraveyard } shouldBe true

        // The spell itself resolved and is in its owner's graveyard (it isn't discarded — it was on
        // the stack when hands were discarded).
        (reforge in yourGraveyard) shouldBe true
    }

    test("an empty hand still draws seven") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        // Empty the opponent's hand before the wheel.
        driver.replaceState(
            driver.state.copy(
                zones = driver.state.zones +
                    (com.wingedsheep.engine.state.ZoneKey(opponent, com.wingedsheep.sdk.core.Zone.HAND) to emptyList())
            )
        )
        driver.getHandSize(opponent) shouldBe 0

        val reforge = driver.putCardInHand(you, "Reforge the Soul")
        driver.giveColorlessMana(you, 3)
        driver.giveMana(you, com.wingedsheep.sdk.core.Color.RED, 2)
        driver.submitSuccess(
            CastSpell(playerId = you, cardId = reforge, paymentStrategy = PaymentStrategy.FromPool)
        )
        driver.bothPass()

        driver.getHandSize(opponent) shouldBe 7
        driver.getHandSize(you) shouldBe 7
    }
})
