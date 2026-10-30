package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
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
 * Sleep of the Dead (THB #66).
 *
 * "Tap target creature. It doesn't untap during its controller's next untap step. / Escape—{2}{U},
 * Exile three other cards from your graveyard."
 *
 * Escape on a targeted sorcery: cast from the graveyard for {2}{U} plus three other cards, freeze
 * an opponent's creature through *its controller's* untap step, then return to the graveyard ready
 * to escape again.
 */
class SleepOfTheDeadScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        return driver to driver.activePlayer!!
    }

    fun escape(driver: GameTestDriver, player: EntityId, sleep: EntityId, target: EntityId, fodder: List<EntityId>) =
        CastSpell(
            playerId = player,
            cardId = sleep,
            targets = listOf(entityIdToChosenTarget(driver.state, target)),
            useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.ESCAPE,
            additionalCostPayment = AdditionalCostPayment(exiledCards = fodder),
            paymentStrategy = PaymentStrategy.FromPool
        )

    test("escapes for {2}{U} and three other cards, taps the creature, which then skips its controller's untap") {
        val (driver, player) = setup()
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val sleep = driver.putCardInGraveyard(player, "Sleep of the Dead")
        val fodder = (1..3).map { driver.putCardInGraveyard(player, "Island") }
        driver.giveMana(player, Color.BLUE, 3)

        val result = driver.submit(escape(driver, player, sleep, bear, fodder))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.bothPass()

        driver.isTapped(bear) shouldBe true
        driver.getExile(player) shouldContainAll fodder
        withClue("an escaped sorcery returns to the graveyard") {
            driver.getGraveyard(player) shouldContain sleep
            driver.getExile(player) shouldNotContain sleep
        }

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe opponent
        withClue("the bear doesn't untap during its controller's next untap step") {
            driver.isTapped(bear) shouldBe true
        }
    }

    test("two other cards can't pay the escape cost") {
        val (driver, player) = setup()
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val bear = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val sleep = driver.putCardInGraveyard(player, "Sleep of the Dead")
        val fodder = (1..2).map { driver.putCardInGraveyard(player, "Island") }
        driver.giveMana(player, Color.BLUE, 3)

        driver.submitExpectFailure(escape(driver, player, sleep, bear, fodder))
        driver.getGraveyard(player) shouldContain sleep
        driver.isTapped(bear) shouldBe false
    }
})
