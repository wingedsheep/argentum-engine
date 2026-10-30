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
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Sweet Oblivion (THB #70).
 *
 * "Target player mills four cards. / Escape—{3}{U}, Exile four other cards from your graveyard."
 *
 * Pins the targeted mill from hand, and the escape cast from the graveyard: four other cards are
 * exiled to pay, the target mills four, and the escaped sorcery returns to its owner's graveyard
 * rather than exile (ruling — only permanent-spell escape or an explicit clause would differ).
 */
class SweetOblivionScenarioTest : FunSpec({

    fun setup(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to player
    }

    fun escape(player: EntityId, oblivion: EntityId, target: EntityId, fodder: List<EntityId>) =
        CastSpell(
            playerId = player,
            cardId = oblivion,
            targets = listOf(ChosenTarget.Player(target)),
            useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.ESCAPE,
            additionalCostPayment = AdditionalCostPayment(exiledCards = fodder),
            paymentStrategy = PaymentStrategy.FromPool
        )

    test("cast from hand, target player mills four cards") {
        val (driver, player) = setup()
        val opponent = driver.getOpponent(player)
        val oblivion = driver.putCardInHand(player, "Sweet Oblivion")
        driver.giveMana(player, Color.BLUE, 2)
        val graveyardBefore = driver.getGraveyard(opponent).size

        val result = driver.submit(
            CastSpell(
                playerId = player,
                cardId = oblivion,
                targets = listOf(ChosenTarget.Player(opponent)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.bothPass()

        driver.getGraveyard(opponent) shouldHaveSize graveyardBefore + 4
        driver.getGraveyard(player) shouldContain oblivion
    }

    test("escapes for {3}{U} and four other cards, mills four, and returns to the graveyard") {
        val (driver, player) = setup()
        val opponent = driver.getOpponent(player)
        val oblivion = driver.putCardInGraveyard(player, "Sweet Oblivion")
        val fodder = (1..4).map { driver.putCardInGraveyard(player, "Island") }
        driver.giveMana(player, Color.BLUE, 4)
        val opponentGraveyardBefore = driver.getGraveyard(opponent).size

        val result = driver.submit(escape(player, oblivion, opponent, fodder))
        withClue("error=${result.error}") { result.outcome shouldBe Outcome.Done }
        driver.bothPass()

        driver.getGraveyard(opponent) shouldHaveSize opponentGraveyardBefore + 4
        driver.getExile(player) shouldContainAll fodder
        withClue("an escaped sorcery returns to its owner's graveyard, not exile") {
            driver.getGraveyard(player) shouldContain oblivion
            driver.getExile(player) shouldNotContain oblivion
        }
    }

    test("three other cards can't pay the escape cost") {
        val (driver, player) = setup()
        val opponent = driver.getOpponent(player)
        val oblivion = driver.putCardInGraveyard(player, "Sweet Oblivion")
        val fodder = (1..3).map { driver.putCardInGraveyard(player, "Island") }
        driver.giveMana(player, Color.BLUE, 4)

        driver.submitExpectFailure(escape(player, oblivion, opponent, fodder))
        driver.getGraveyard(player) shouldContain oblivion
    }
})
