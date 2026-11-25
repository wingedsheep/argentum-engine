package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Underworld Breach (THB #161).
 *
 * "Each nonland card in your graveyard has escape. The escape cost is equal to the card's mana cost
 * plus exile three other cards from your graveyard. / At the beginning of the end step, sacrifice
 * this enchantment."
 */
class UnderworldBreachScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver to driver.activePlayer!!
    }

    fun escapeBolt(player: EntityId, bolt: EntityId, target: EntityId, exiled: List<EntityId>) = CastSpell(
        playerId = player,
        cardId = bolt,
        targets = listOf(ChosenTarget.Player(target)),
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.ESCAPE,
        additionalCostPayment = AdditionalCostPayment(exiledCards = exiled),
        paymentStrategy = PaymentStrategy.FromPool
    )

    test("a graveyard Lightning Bolt escapes twice for {R} plus three other cards each time") {
        val (driver, player) = setup()
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(player, "Underworld Breach")
        val bolt = driver.putCardInGraveyard(player, "Lightning Bolt")
        val fodder = (1..6).map { driver.putCardInGraveyard(player, "Mountain") }

        for (batch in fodder.chunked(3)) {
            driver.giveMana(player, Color.RED, 1)
            val result = driver.submit(escapeBolt(player, bolt, opponent, batch))
            withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
            driver.bothPass()
            driver.getGraveyard(player) shouldContain bolt
        }

        driver.getLifeTotal(opponent) shouldBe 14
        driver.getExile(player) shouldContainAll fodder
    }

    test("it is sacrificed at the beginning of the end step") {
        val (driver, player) = setup()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(player, "Underworld Breach")

        driver.passPriorityUntil(Step.END)
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        driver.findPermanent(player, "Underworld Breach").shouldBeNull()
        driver.getGraveyardCardNames(player) shouldContain "Underworld Breach"
    }

    // Big Score prints its own "discard a card" on top of the escape exile, so the escape offer
    // must ask for both — regression: only the discard picker was published, and the cast then
    // failed for want of the three exiled cards.
    test("escaping Big Score asks for its discard and the three escape exiles, and both are paid") {
        val (driver, player) = setup()
        driver.registerCard(PredefinedTokens.Treasure)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(player, "Underworld Breach")
        val bigScore = driver.putCardInGraveyard(player, "Big Score")
        val fodder = (1..3).map { driver.putCardInGraveyard(player, "Mountain") }
        val discard = driver.putCardInHand(player, "Lightning Bolt")
        driver.giveMana(player, Color.RED, 4)

        val offer = LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, player)
            .firstOrNull { it.actionType == "CastWithEscape" && it.action.let { a -> a is CastSpell && a.cardId == bigScore } }
            .shouldNotBeNull()
        offer.affordable shouldBe true
        val info = offer.additionalCostInfo.shouldNotBeNull()
        val pickers = listOf(info) + info.alsoRequired
        pickers shouldHaveSize 2
        pickers.single { it.validDiscardTargets.isNotEmpty() }.validDiscardTargets shouldContain discard
        val exile = pickers.single { it.validExileTargets.isNotEmpty() }
        exile.validExileTargets shouldContainExactly fodder
        exile.validExileTargets shouldNotContain bigScore
        exile.exileMinCount shouldBe 3

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = bigScore,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.ESCAPE,
                additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(discard), exiledCards = fodder),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.bothPass()

        driver.getExile(player) shouldContainAll fodder
        driver.getGraveyard(player) shouldContainAll listOf(bigScore, discard)
        driver.getHand(player) shouldNotContain discard
        driver.getPermanents(player).count { driver.getCardName(it) == "Treasure" } shouldBe 2
    }
})
