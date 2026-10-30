package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Glimpse of Freedom (THB #50).
 *
 * "Draw a card. / Escape—{2}{U}, Exile five other cards from your graveyard."
 *
 * Escape on an untargeted instant: cast from the graveyard for {2}{U} plus five other cards, draw
 * a card, then return to the graveyard ready to escape again.
 */
class GlimpseOfFreedomScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        return driver to driver.activePlayer!!
    }

    fun escape(player: EntityId, glimpse: EntityId, fodder: List<EntityId>) = CastSpell(
        playerId = player,
        cardId = glimpse,
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.ESCAPE,
        additionalCostPayment = AdditionalCostPayment(exiledCards = fodder),
        paymentStrategy = PaymentStrategy.FromPool
    )

    test("escapes from the graveyard for {2}{U} and five other cards, draws, and returns to the graveyard") {
        val (driver, player) = setup()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val glimpse = driver.putCardInGraveyard(player, "Glimpse of Freedom")
        val fodder = (1..5).map { driver.putCardInGraveyard(player, "Island") }
        val handBefore = driver.getHandSize(player)
        driver.giveMana(player, Color.BLUE, 3)

        val result = driver.submit(escape(player, glimpse, fodder))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.bothPass()

        driver.getHandSize(player) shouldBe handBefore + 1
        driver.getExile(player) shouldContainAll fodder
        driver.getGraveyard(player) shouldContain glimpse
        driver.getExile(player) shouldNotContain glimpse
    }

    test("four other cards can't pay the escape cost") {
        val (driver, player) = setup()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val glimpse = driver.putCardInGraveyard(player, "Glimpse of Freedom")
        val fodder = (1..4).map { driver.putCardInGraveyard(player, "Island") }
        driver.giveMana(player, Color.BLUE, 3)

        driver.submitExpectFailure(escape(player, glimpse, fodder))
        driver.getGraveyard(player) shouldContain glimpse
    }
})
