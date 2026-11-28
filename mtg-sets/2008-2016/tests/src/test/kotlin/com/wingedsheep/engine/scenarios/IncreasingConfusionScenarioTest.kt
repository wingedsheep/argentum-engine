package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.cards.IncreasingConfusion
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Increasing Confusion — "Target player mills X cards. If this spell was cast from a graveyard,
 * that player mills twice that many cards instead. Flashback {X}{U}"
 *
 * Its flashback cost carries an {X}: the graveyard offer must announce X (slider + ceiling), and the
 * flashback cast mills twice the X paid.
 */
class IncreasingConfusionScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(IncreasingConfusion)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.settle() {
        var guard = 0
        while (guard++ < 30) {
            when {
                isPaused -> autoResolveDecision()
                state.stack.isNotEmpty() -> bothPass()
                else -> break
            }
        }
    }

    fun GameTestDriver.librarySize(player: EntityId) =
        state.getZone(ZoneKey(player, Zone.LIBRARY)).size

    test("cast from hand, the target player mills X") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val before = driver.librarySize(opponent)

        val confusion = driver.putCardInHand(you, "Increasing Confusion")
        driver.giveMana(you, Color.BLUE, 4)
        driver.submit(
            CastSpell(you, confusion, targets = listOf(ChosenTarget.Player(opponent)), xValue = 3,
                paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.settle()

        driver.librarySize(opponent) shouldBe before - 3
        driver.getGraveyardCardNames(you).contains("Increasing Confusion") shouldBe true
    }

    test("the flashback offer announces X, and the flashback cast mills twice X then exiles it") {
        val driver = newDriver()
        val you = driver.player1
        val opponent = driver.getOpponent(you)
        val confusion = driver.putCardInGraveyard(you, "Increasing Confusion")
        repeat(4) { driver.putPermanentOnBattlefield(you, "Island") }

        val offer = driver.legalActions(you).single {
            (it.action as? CastSpell)?.cardId == confusion && it.actionType == "CastWithFlashback"
        }
        withClue("{X}{U} flashback from four Islands: X slider up to 3") {
            offer.affordable shouldBe true
            offer.hasXCost shouldBe true
            offer.maxAffordableX shouldBe 3
        }

        val before = driver.librarySize(opponent)
        driver.submit(
            CastSpell(you, confusion, targets = listOf(ChosenTarget.Player(opponent)), xValue = 3,
                useAlternativeCost = true, alternativeCostType = AlternativeCostType.FLASHBACK)
        ).error shouldBe null
        driver.settle()

        withClue("cast from a graveyard: twice X = 6 cards milled") {
            driver.librarySize(opponent) shouldBe before - 6
        }
        driver.getExile(you).contains(confusion) shouldBe true
    }
})
