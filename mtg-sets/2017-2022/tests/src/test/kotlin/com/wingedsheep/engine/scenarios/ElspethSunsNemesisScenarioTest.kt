package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Elspeth, Sun's Nemesis (THB #14).
 *
 * The corpus's first planeswalker with escape: cast from the graveyard for {4}{W}{W} plus four
 * other cards, she resolves onto the battlefield as a new permanent with her printed loyalty.
 */
class ElspethSunsNemesisScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver to driver.activePlayer!!
    }

    fun escape(player: EntityId, elspeth: EntityId, fodder: List<EntityId>) = CastSpell(
        playerId = player,
        cardId = elspeth,
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.ESCAPE,
        additionalCostPayment = AdditionalCostPayment(exiledCards = fodder),
        paymentStrategy = PaymentStrategy.FromPool
    )

    test("escapes from the graveyard for {4}{W}{W} and four other cards, entering with 5 loyalty") {
        val (driver, player) = setup()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val elspeth = driver.putCardInGraveyard(player, "Elspeth, Sun's Nemesis")
        val fodder = (1..4).map { driver.putCardInGraveyard(player, "Plains") }
        driver.giveMana(player, Color.WHITE, 6)

        val result = driver.submit(escape(player, elspeth, fodder))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.bothPass()

        driver.getExile(player) shouldContainAll fodder
        val permanent = driver.findPermanent(player, "Elspeth, Sun's Nemesis")
        permanent shouldNotBe null
        driver.state.getEntity(permanent!!)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 5
    }

    test("three other cards can't pay the escape cost") {
        val (driver, player) = setup()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val elspeth = driver.putCardInGraveyard(player, "Elspeth, Sun's Nemesis")
        val fodder = (1..3).map { driver.putCardInGraveyard(player, "Plains") }
        driver.giveMana(player, Color.WHITE, 6)

        driver.submitExpectFailure(escape(player, elspeth, fodder))
        driver.getGraveyard(player) shouldContain elspeth
    }
})
