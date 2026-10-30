package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
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
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Kroxa, Titan of Death's Hunger (THB #221).
 *
 * "When Kroxa enters, sacrifice it unless it escaped. / Whenever Kroxa enters or attacks, each
 * opponent discards a card, then each opponent who didn't discard a nonland card this way loses 3
 * life. / Escape—{B}{B}{R}{R}, Exile five other cards from your graveyard."
 *
 * Pins the per-opponent tracking of what was discarded: a land discard costs 3 life, a nonland
 * discard costs nothing, and the sacrifice applies only to a Kroxa that didn't escape.
 */
class KroxaTitanOfDeathsHungerScenarioTest : FunSpec({

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        return Triple(driver, player, opponent)
    }

    /** Resolve the stack; the opponent discards [pick] when it is offered, otherwise the first card. */
    fun resolveAll(driver: GameTestDriver, opponent: EntityId, pick: EntityId? = null) {
        var guard = 0
        while ((driver.state.stack.isNotEmpty() || driver.pendingDecision != null) && guard++ < 50) {
            val decision = driver.pendingDecision
            when {
                decision == null -> driver.bothPass()
                decision is SelectCardsDecision && decision.playerId == opponent && pick != null &&
                    pick in decision.options -> driver.submitCardSelection(opponent, listOf(pick))
                else -> driver.autoResolveDecision()
            }
        }
    }

    fun escape(driver: GameTestDriver, player: EntityId): EntityId {
        val kroxa = driver.putCardInGraveyard(player, "Kroxa, Titan of Death's Hunger")
        val fodder = (1..5).map { driver.putCardInGraveyard(player, "Mountain") }
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.BLACK, 2)
        driver.giveMana(player, Color.RED, 2)
        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = kroxa,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.ESCAPE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = fodder),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.getExile(player) shouldContainAll fodder
        return kroxa
    }

    test("hard-cast Kroxa is sacrificed; an opponent who discards a land loses 3 life") {
        val (driver, player, opponent) = setup()
        val kroxa = driver.putCardInHand(player, "Kroxa, Titan of Death's Hunger")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val opponentHand = driver.getHandSize(opponent)
        driver.giveMana(player, Color.BLACK, 1)
        driver.giveMana(player, Color.RED, 1)

        val result = driver.submit(CastSpell(player, kroxa, paymentStrategy = PaymentStrategy.FromPool))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        resolveAll(driver, opponent)

        driver.findPermanent(player, "Kroxa, Titan of Death's Hunger").shouldBeNull()
        driver.getGraveyard(player) shouldContain kroxa
        driver.getHandSize(opponent) shouldBe opponentHand - 1
        driver.getLifeTotal(opponent) shouldBe 17
        withClue("the controller is not an opponent") { driver.getLifeTotal(player) shouldBe 20 }
    }

    test("escaped Kroxa stays; a nonland discard spares the life, a land discard on attack doesn't") {
        val (driver, player, opponent) = setup()
        val bears = driver.putCardInHand(opponent, "Grizzly Bears")
        escape(driver, player)
        resolveAll(driver, opponent, pick = bears)

        val perm = driver.findPermanent(player, "Kroxa, Titan of Death's Hunger").shouldNotBeNull()
        driver.getGraveyard(opponent) shouldContain bears
        withClue("discarding a nonland card costs no life") { driver.getLifeTotal(opponent) shouldBe 20 }

        driver.removeSummoningSickness(perm)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(perm), opponent)
        val handBeforeAttackTrigger = driver.getHandSize(opponent)
        resolveAll(driver, opponent)

        driver.getHandSize(opponent) shouldBe handBeforeAttackTrigger - 1
        withClue("only Mountains were left to discard") { driver.getLifeTotal(opponent) shouldBe 17 }
    }
})
