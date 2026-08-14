package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mid.cards.LunarchVeteran
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Lunarch Veteran // Luminous Phantom (MID).
 *
 * Front: "Whenever another creature you control enters, you gain 1 life." + Disturb {1}{W}.
 * Back (Luminous Phantom): flying, "Whenever another creature you control leaves the battlefield,
 * you gain 1 life", and "if it would be put into a graveyard from anywhere, exile it instead".
 */
class LunarchVeteranScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LunarchVeteran))
        return driver
    }

    fun disturbCast(driver: GameTestDriver, player: EntityId, cardId: EntityId) =
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = cardId,
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.DISTURB,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

    test("front face gains 1 life whenever ANOTHER creature you control enters, but not for itself") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val veteran = driver.putCardInHand(player, "Lunarch Veteran")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.WHITE, 1)

        val lifeBefore = driver.getLifeTotal(player)
        driver.submit(CastSpell(player, veteran, paymentStrategy = PaymentStrategy.FromPool)).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        // Its own entry is not "another creature" — no life gained yet.
        driver.getLifeTotal(player) shouldBe lifeBefore

        // A second creature entering through the real cast pipeline does trigger it.
        val lions = driver.putCardInHand(player, "Savannah Lions")
        driver.giveMana(player, Color.WHITE, 1)
        driver.submit(CastSpell(player, lions, paymentStrategy = PaymentStrategy.FromPool)).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getLifeTotal(player) shouldBe lifeBefore + 1
    }

    test("disturb casts it as Luminous Phantom: a 1/1 flying Spirit Cleric, not the Human front face") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val veteran = driver.putCardInGraveyard(player, "Lunarch Veteran")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.WHITE, 2)

        val result = disturbCast(driver, player, veteran)
        io.kotest.assertions.withClue("error=${result.error}") { result.isSuccess shouldBe true }
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        val phantom = driver.findPermanent(player, "Luminous Phantom")
        phantom.shouldNotBeNull()
        driver.findPermanent(player, "Lunarch Veteran") shouldBe null
        driver.state.projectedState.hasKeyword(phantom, Keyword.FLYING).shouldBeTrue()
        driver.state.getEntity(phantom)?.get<DoubleFacedComponent>()?.isBack shouldBe true
    }

    test("Luminous Phantom gains 1 life whenever another creature you control leaves the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val veteran = driver.putCardInGraveyard(player, "Lunarch Veteran")
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        val victim = driver.putCreatureOnBattlefield(player, "Savannah Lions")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.WHITE, 2)
        driver.giveMana(player, Color.RED, 1)

        disturbCast(driver, player, veteran).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()
        val lifeBefore = driver.getLifeTotal(player)

        driver.submit(
            CastSpell(
                playerId = player, cardId = bolt,
                targets = listOf(ChosenTarget.Permanent(victim)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty() || driver.pendingDecision != null) driver.bothPass()

        driver.getLifeTotal(player) shouldBe lifeBefore + 1
    }

    test("Luminous Phantom is exiled when it dies, so it can never be disturbed twice") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!

        val veteran = driver.putCardInGraveyard(player, "Lunarch Veteran")
        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.WHITE, 2)
        driver.giveMana(player, Color.RED, 1)

        disturbCast(driver, player, veteran).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.submit(
            CastSpell(
                playerId = player, cardId = bolt,
                targets = listOf(ChosenTarget.Permanent(veteran)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty() || driver.pendingDecision != null) driver.bothPass()

        driver.state.getExile(player).shouldContain(veteran)
        driver.getGraveyard(player) shouldNotContain veteran
    }
})
