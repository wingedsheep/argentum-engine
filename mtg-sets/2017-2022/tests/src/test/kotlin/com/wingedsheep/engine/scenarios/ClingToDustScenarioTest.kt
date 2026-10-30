package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
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
 * Cling to Dust (THB #87).
 *
 * "Exile target card from a graveyard. If it was a creature card, you gain 3 life. Otherwise, you
 * draw a card. / Escape—{3}{B}, Exile five other cards from your graveyard."
 *
 * Escape on an instant: cast from the graveyard for {3}{B} plus five other cards, and — unlike
 * flashback — it goes back to the graveyard once it resolves.
 */
class ClingToDustScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        return driver to driver.activePlayer!!
    }

    test("exiling a creature card gains 3 life and draws nothing") {
        val (driver, player) = setup()
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val cling = driver.putCardInHand(player, "Cling to Dust")
        val bears = driver.putCardInGraveyard(opponent, "Grizzly Bears")
        val handBefore = driver.getHandSize(player)
        driver.giveMana(player, Color.BLACK, 1)

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = cling,
                targets = listOf(ChosenTarget.Card(bears, opponent, Zone.GRAVEYARD)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.bothPass()

        driver.getExile(opponent) shouldContain bears
        driver.getLifeTotal(player) shouldBe 23
        driver.getHandSize(player) shouldBe handBefore - 1
        driver.getGraveyard(player) shouldContain cling
    }

    test("exiling a noncreature card draws a card and gains no life") {
        val (driver, player) = setup()
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val cling = driver.putCardInHand(player, "Cling to Dust")
        val bolt = driver.putCardInGraveyard(opponent, "Lightning Bolt")
        val handBefore = driver.getHandSize(player)
        driver.giveMana(player, Color.BLACK, 1)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cling,
                targets = listOf(ChosenTarget.Card(bolt, opponent, Zone.GRAVEYARD)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getExile(opponent) shouldContain bolt
        driver.getLifeTotal(player) shouldBe 20
        driver.getHandSize(player) shouldBe handBefore
    }

    test("escapes from the graveyard for {3}{B} and five other cards, then returns to the graveyard") {
        val (driver, player) = setup()
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val cling = driver.putCardInGraveyard(player, "Cling to Dust")
        val fodder = (1..5).map { driver.putCardInGraveyard(player, "Swamp") }
        val bears = driver.putCardInGraveyard(opponent, "Grizzly Bears")
        driver.giveMana(player, Color.BLACK, 4)

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = cling,
                targets = listOf(ChosenTarget.Card(bears, opponent, Zone.GRAVEYARD)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.ESCAPE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = fodder),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.bothPass()

        driver.getExile(player) shouldContainAll fodder
        driver.getExile(opponent) shouldContain bears
        driver.getLifeTotal(player) shouldBe 23
        driver.getGraveyard(player) shouldContain cling
        driver.getExile(player) shouldNotContain cling
    }

    test("four other cards can't pay the escape cost") {
        val (driver, player) = setup()
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val cling = driver.putCardInGraveyard(player, "Cling to Dust")
        val fodder = (1..4).map { driver.putCardInGraveyard(player, "Swamp") }
        val bears = driver.putCardInGraveyard(opponent, "Grizzly Bears")
        driver.giveMana(player, Color.BLACK, 4)

        driver.submitExpectFailure(
            CastSpell(
                playerId = player,
                cardId = cling,
                targets = listOf(ChosenTarget.Card(bears, opponent, Zone.GRAVEYARD)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.ESCAPE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = fodder),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.getGraveyard(player) shouldContain cling
    }
})
