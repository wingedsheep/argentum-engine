package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.CerebralConfiscation
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Cerebral Confiscation (MKM #81) — {2}{B} Sorcery.
 *
 * "Choose one —
 *  • Target opponent discards two cards.
 *  • Target opponent reveals their hand. You choose a nonland card from it. That player discards
 *    that card."
 *
 * Both modes target the same opponent but hand the *choice* to different players: mode 0 is the
 * opponent's own discard, mode 1 is yours. The cards always land in the opponent's graveyard —
 * the pair of things easiest to wire backwards, and what these tests pin.
 */
class CerebralConfiscationScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CerebralConfiscation))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castMode(driver: GameTestDriver, you: EntityId, opponent: EntityId, mode: Int) {
        val card = driver.putCardInHand(you, "Cerebral Confiscation")
        driver.giveMana(you, Color.BLACK, 1)
        driver.giveColorlessMana(you, 2)
        driver.submit(
            CastSpell(
                playerId = you,
                cardId = card,
                targets = listOf(ChosenTarget.Player(opponent)),
                chosenModes = listOf(mode),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(opponent))),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()
    }

    test("mode 0: the opponent discards two cards of their own choosing") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        driver.putCardInHand(opponent, "Grizzly Bears")
        driver.putCardInHand(opponent, "Hill Giant")
        val gyBefore = driver.getGraveyard(opponent).size
        val myGyBefore = driver.getGraveyard(you).size

        castMode(driver, you, opponent, mode = 0)

        val decision = driver.state.pendingDecision as? SelectCardsDecision
            ?: error("expected a SelectCardsDecision for the discard; got ${driver.state.pendingDecision}")
        withClue("the discarding player chooses — not the caster") {
            decision.playerId shouldBe opponent
        }
        driver.submitCardSelection(opponent, decision.options.take(2))

        withClue("two cards moved to the opponent's graveyard") {
            (driver.getGraveyard(opponent).size - gyBefore) shouldBe 2
        }
        withClue("the caster discards nothing — only the spell itself hits their graveyard") {
            (driver.getGraveyard(you).size - myGyBefore) shouldBe 1
            driver.getGraveyardCardNames(you).contains("Cerebral Confiscation") shouldBe true
        }
    }

    test("mode 1: you pick a nonland card from the revealed hand and they discard it") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        val nonland = driver.putCardInHand(opponent, "Grizzly Bears")
        val land = driver.putCardInHand(opponent, "Plains")
        val gyBefore = driver.getGraveyard(opponent).size

        castMode(driver, you, opponent, mode = 1)

        val decision = driver.state.pendingDecision as? SelectCardsDecision
            ?: error("expected a SelectCardsDecision for the chosen card; got ${driver.state.pendingDecision}")
        withClue("the caster chooses, and only nonland cards are on offer") {
            decision.playerId shouldBe you
            decision.options.contains(nonland) shouldBe true
            decision.options.contains(land) shouldBe false
        }
        driver.submitCardSelection(you, listOf(nonland))

        withClue("the chosen card lands in the opponent's graveyard") {
            driver.getGraveyard(opponent).contains(nonland) shouldBe true
            (driver.getGraveyard(opponent).size - gyBefore) shouldBe 1
        }
    }
})
