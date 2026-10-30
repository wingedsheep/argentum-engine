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
 * Satyr's Cunning (THB #152).
 *
 * "Create a 1/1 red Satyr creature token with "This token can't block." / Escape—{2}{R}, Exile two
 * other cards from your graveyard."
 *
 * Escape on a sorcery: cast from the graveyard (at sorcery speed) for {2}{R} plus two other cards,
 * create the Satyr, then return to the graveyard ready to escape again.
 */
class SatyrsCunningScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver to driver.activePlayer!!
    }

    fun escape(player: EntityId, cunning: EntityId, fodder: List<EntityId>) = CastSpell(
        playerId = player,
        cardId = cunning,
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.ESCAPE,
        additionalCostPayment = AdditionalCostPayment(exiledCards = fodder),
        paymentStrategy = PaymentStrategy.FromPool
    )

    fun satyrs(driver: GameTestDriver, player: EntityId) =
        driver.getCreatures(player).filter { driver.getCardName(it) == "Satyr Token" }

    test("escapes from the graveyard for {2}{R} and two other cards, makes a Satyr, and returns to the graveyard") {
        val (driver, player) = setup()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val cunning = driver.putCardInGraveyard(player, "Satyr's Cunning")
        val fodder = (1..2).map { driver.putCardInGraveyard(player, "Mountain") }
        driver.giveMana(player, Color.RED, 3)

        val result = driver.submit(escape(player, cunning, fodder))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.bothPass()

        satyrs(driver, player).size shouldBe 1
        driver.getExile(player) shouldContainAll fodder
        driver.getGraveyard(player) shouldContain cunning
        driver.getExile(player) shouldNotContain cunning
    }

    test("one other card can't pay the escape cost") {
        val (driver, player) = setup()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val cunning = driver.putCardInGraveyard(player, "Satyr's Cunning")
        val fodder = listOf(driver.putCardInGraveyard(player, "Mountain"))
        driver.giveMana(player, Color.RED, 3)

        driver.submitExpectFailure(escape(player, cunning, fodder))
        driver.getGraveyard(player) shouldContain cunning
        satyrs(driver, player).size shouldBe 0
    }
})
