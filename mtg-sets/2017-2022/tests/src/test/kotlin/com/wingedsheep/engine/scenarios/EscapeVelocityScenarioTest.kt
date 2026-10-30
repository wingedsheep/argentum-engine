package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
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
 * Escape Velocity (THB #132).
 *
 * "Enchant creature / Enchanted creature gets +1/+0 and has haste. / Escape—{1}{R}, Exile two other
 * cards from your graveyard."
 *
 * Escape on an Aura: cast from the graveyard for {1}{R} plus two other cards, it still targets a
 * creature as it is cast, enters attached to it, and goes back to the graveyard when it dies.
 */
class EscapeVelocityScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver to driver.activePlayer!!
    }

    fun GameTestDriver.escapeOnto(player: EntityId, aura: EntityId, creature: EntityId, fodder: List<EntityId>) =
        submit(
            CastSpell(
                playerId = player,
                cardId = aura,
                targets = listOf(ChosenTarget.Permanent(creature)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.ESCAPE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = fodder),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )

    test("escapes from the graveyard onto a creature, granting +1/+0 and haste") {
        val (driver, player) = setup()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val aura = driver.putCardInGraveyard(player, "Escape Velocity")
        val fodder = (1..2).map { driver.putCardInGraveyard(player, "Mountain") }
        driver.giveMana(player, Color.RED, 2)

        val result = driver.escapeOnto(player, aura, bears, fodder)
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.bothPass()

        driver.getExile(player) shouldContainAll fodder
        driver.getPermanents(player) shouldContain aura
        driver.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe bears
        driver.state.projectedState.getPower(bears) shouldBe 3
        driver.state.projectedState.getToughness(bears) shouldBe 2
        driver.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
    }

    test("an escaped Aura goes to the graveyard, not exile, when its creature dies") {
        val (driver, player) = setup()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val aura = driver.putCardInGraveyard(player, "Escape Velocity")
        val fodder = (1..2).map { driver.putCardInGraveyard(player, "Mountain") }
        driver.giveMana(player, Color.RED, 2)
        driver.escapeOnto(player, aura, bears, fodder).outcome shouldBe Outcome.Done
        driver.bothPass()

        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.giveMana(player, Color.RED, 1)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = bolt,
                targets = listOf(ChosenTarget.Permanent(bears)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getGraveyard(player) shouldContain bears
        driver.getGraveyard(player) shouldContain aura
        driver.getExile(player) shouldNotContain aura
    }

    test("one other card can't pay the escape cost") {
        val (driver, player) = setup()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val aura = driver.putCardInGraveyard(player, "Escape Velocity")
        val fodder = listOf(driver.putCardInGraveyard(player, "Mountain"))
        driver.giveMana(player, Color.RED, 2)

        driver.submitExpectFailure(
            CastSpell(
                playerId = player,
                cardId = aura,
                targets = listOf(ChosenTarget.Permanent(bears)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.ESCAPE,
                additionalCostPayment = AdditionalCostPayment(exiledCards = fodder),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.getGraveyard(player) shouldContain aura
    }
})
