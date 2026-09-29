package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
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
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Phlage, Titan of Fire's Fury (MH3 #197).
 *
 * "When Phlage enters, sacrifice it unless it escaped. / Whenever Phlage enters or attacks, it
 * deals 3 damage to any target and you gain 3 life. / Escape—{R}{R}{W}{W}, Exile five other cards
 * from your graveyard."
 */
class PhlageTitanOfFiresFuryScenarioTest : FunSpec({

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        return Triple(driver, player, opponent)
    }

    /** Resolve the stack, aiming every "any target" prompt at [target]. */
    fun resolveAll(driver: GameTestDriver, controller: EntityId, target: EntityId) {
        var guard = 0
        while ((driver.state.stack.isNotEmpty() || driver.pendingDecision != null) && guard++ < 50) {
            when (driver.pendingDecision) {
                null -> driver.bothPass()
                is ChooseTargetsDecision -> driver.submitTargetSelection(controller, listOf(target))
                else -> driver.autoResolveDecision()
            }
        }
    }

    test("hard-cast Phlage is sacrificed, but its enters trigger still deals 3 and gains 3") {
        val (driver, player, opponent) = setup()
        val phlage = driver.putCardInHand(player, "Phlage, Titan of Fire's Fury")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.RED, 2)
        driver.giveMana(player, Color.WHITE, 1)

        val result = driver.submit(CastSpell(player, phlage, paymentStrategy = PaymentStrategy.FromPool))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        resolveAll(driver, player, opponent)

        driver.findPermanent(player, "Phlage, Titan of Fire's Fury").shouldBeNull()
        driver.getGraveyard(player) shouldContain phlage
        driver.getLifeTotal(opponent) shouldBe 17
        driver.getLifeTotal(player) shouldBe 23
    }

    test("escaped Phlage exiles five other cards, stays, and deals 3 / gains 3; attacking does it again") {
        val (driver, player, opponent) = setup()
        val phlage = driver.putCardInGraveyard(player, "Phlage, Titan of Fire's Fury")
        val fodder = (1..5).map { driver.putCardInGraveyard(player, "Mountain") }
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.RED, 2)
        driver.giveMana(player, Color.WHITE, 2)

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = phlage,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.ESCAPE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = fodder),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.getExile(player) shouldContainAll fodder
        resolveAll(driver, player, opponent)

        val perm = driver.findPermanent(player, "Phlage, Titan of Fire's Fury").shouldNotBeNull()
        driver.getLifeTotal(opponent) shouldBe 17
        driver.getLifeTotal(player) shouldBe 23

        driver.removeSummoningSickness(perm)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(perm), opponent)
        resolveAll(driver, player, opponent)
        driver.getLifeTotal(opponent) shouldBe 14
        driver.getLifeTotal(player) shouldBe 26
    }

    test("escape can't be paid with only four other cards in the graveyard") {
        val (driver, player, opponent) = setup()
        val phlage = driver.putCardInGraveyard(player, "Phlage, Titan of Fire's Fury")
        val fodder = (1..4).map { driver.putCardInGraveyard(player, "Mountain") }
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.RED, 2)
        driver.giveMana(player, Color.WHITE, 2)

        driver.submitExpectFailure(
            CastSpell(
                playerId = player,
                cardId = phlage,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.ESCAPE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = fodder + phlage),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.getGraveyard(player) shouldContain phlage
        driver.getLifeTotal(opponent) shouldBe 20
    }
})
