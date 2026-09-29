package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.layers.StateProjector
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
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Ox of Agonas (THB #147).
 *
 * "When this creature enters, discard your hand, then draw three cards. / Escape—{R}{R}, Exile
 * eight other cards from your graveyard. / This creature escapes with a +1/+1 counter on it."
 */
class OxOfAgonasScenarioTest : FunSpec({

    val projector = StateProjector()

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver to driver.activePlayer!!
    }

    fun resolveAll(driver: GameTestDriver) {
        var guard = 0
        while ((driver.state.stack.isNotEmpty() || driver.pendingDecision != null) && guard++ < 50) {
            if (driver.pendingDecision == null) driver.bothPass() else driver.autoResolveDecision()
        }
    }

    fun plusOne(driver: GameTestDriver, id: EntityId) =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("escaped Ox enters with a +1/+1 counter, discards the hand and draws three") {
        val (driver, player) = setup()
        val ox = driver.putCardInGraveyard(player, "Ox of Agonas")
        val fodder = (1..8).map { driver.putCardInGraveyard(player, "Mountain") }
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val handBefore = driver.getHand(player)
        driver.giveMana(player, Color.RED, 2)

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = ox,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.ESCAPE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = fodder),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        resolveAll(driver)

        val perm = driver.findPermanent(player, "Ox of Agonas").shouldNotBeNull()
        plusOne(driver, perm) shouldBe 1
        projector.getProjectedPower(driver.state, perm) shouldBe 5
        projector.getProjectedToughness(driver.state, perm) shouldBe 3
        driver.getExile(player) shouldContainAll fodder
        driver.getGraveyard(player) shouldContainAll handBefore
        driver.getHandSize(player) shouldBe 3
    }

    test("hard-cast Ox enters without a counter") {
        val (driver, player) = setup()
        val ox = driver.putCardInHand(player, "Ox of Agonas")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.RED, 5)

        driver.submit(CastSpell(player, ox, paymentStrategy = PaymentStrategy.FromPool)).outcome shouldBe Outcome.Done
        resolveAll(driver)

        val perm = driver.findPermanent(player, "Ox of Agonas").shouldNotBeNull()
        plusOne(driver, perm) shouldBe 0
        projector.getProjectedPower(driver.state, perm) shouldBe 4
        driver.getHandSize(player) shouldBe 3
    }

    test("seven other cards can't pay the escape cost") {
        val (driver, player) = setup()
        val ox = driver.putCardInGraveyard(player, "Ox of Agonas")
        val fodder = (1..7).map { driver.putCardInGraveyard(player, "Mountain") }
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.RED, 2)

        driver.submitExpectFailure(
            CastSpell(
                playerId = player,
                cardId = ox,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.ESCAPE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = fodder),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.getGraveyard(player) shouldContain ox
    }
})
