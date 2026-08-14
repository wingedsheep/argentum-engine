package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Gisa's Bidding (Shadows over Innistrad #114) — {2}{B}{B} Sorcery.
 *
 * "Create two 2/2 black Zombie creature tokens."
 * "Madness {2}{B}"
 *
 * The tokens are the whole card; the interesting half is madness (CR 702.35), which both makes it
 * a mana cheaper and — because the cast happens while the madness trigger resolves — lets a
 * *sorcery* make its Zombies at instant speed.
 */
class GisasBiddingScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun settle(driver: GameTestDriver) {
        var guard = 0
        while (driver.state.stack.isNotEmpty() && driver.state.pendingDecision == null && guard++ < 20) {
            driver.bothPass()
        }
    }

    /** Discard [cardId] as Tormenting Voice's additional cost. */
    fun discardAsCost(driver: GameTestDriver, player: EntityId, cardId: EntityId) {
        val voice = driver.putCardInHand(player, "Tormenting Voice")
        driver.giveMana(player, Color.RED, 2)
        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = voice,
                additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(cardId)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
    }

    /** The 2/2 black Zombie tokens this makes, by their default "Zombie Token" name. */
    fun zombies(driver: GameTestDriver, player: EntityId): List<EntityId> =
        driver.getPermanents(player).filter { driver.getCardName(it) == "Zombie Token" }

    test("cast for its printed cost, it creates two 2/2 black Zombies") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bidding = driver.putCardInHand(player, "Gisa's Bidding")
        driver.giveMana(player, Color.BLACK, 4)
        driver.castSpell(player, bidding).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val tokens = zombies(driver, player)
        tokens.size shouldBe 2
        tokens.forEach { token ->
            driver.state.projectedState.getPower(token) shouldBe 2
            driver.state.projectedState.getToughness(token) shouldBe 2
        }
    }

    test("discarded, it is exiled and can be cast for its madness cost of {2}{B}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bidding = driver.putCardInHand(player, "Gisa's Bidding")
        discardAsCost(driver, player, bidding)

        driver.getExile(player) shouldContain bidding
        driver.getGraveyard(player).shouldNotContain(bidding)

        // {2}{B} instead of the printed {2}{B}{B}.
        driver.giveMana(player, Color.BLACK, 3)
        settle(driver)
        driver.submitYesNo(player, true)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        zombies(driver, player).size shouldBe 2
        driver.getGraveyard(player) shouldContain bidding
    }

    test("declining the madness trigger puts it into the graveyard with no Zombies made") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bidding = driver.putCardInHand(player, "Gisa's Bidding")
        discardAsCost(driver, player, bidding)

        driver.giveMana(player, Color.BLACK, 3)
        settle(driver)
        driver.submitYesNo(player, false)
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getGraveyard(player) shouldContain bidding
        driver.getExile(player).shouldNotContain(bidding)
        zombies(driver, player).size shouldBe 0
    }
})
