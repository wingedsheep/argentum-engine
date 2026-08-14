package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.DownDownToGoblinTown
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Down, Down to Goblin-town — {2}{B} Enchantment — Saga (HOB #65).
 *
 * I — Target opponent reveals their hand. You choose a nonland card from it. That player discards
 *     that card.
 * II — Amass Goblins 1.
 * III, IV — Target opponent loses 1 life and you gain 1 life.
 *
 * Chapter I is the interesting one: each chapter targets an opponent independently, and the chosen
 * card must be a *nonland* — a hand of nothing but lands strips nothing.
 */
class DownDownToGoblinTownScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + PredefinedTokens.allTokens + listOf(DownDownToGoblinTown))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Drain the stack, auto-answering anything that pauses. */
    fun GameTestDriver.drain() {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 60) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
            guard++
        }
    }

    /** Cast the Saga targeting [opponent] with chapter I, and let chapter I resolve. */
    fun GameTestDriver.castSaga(me: EntityId, opponent: EntityId) {
        giveMana(me, Color.BLACK, 3)
        val card = putCardInHand(me, "Down, Down to Goblin-town")
        submit(
            CastSpell(
                playerId = me,
                cardId = card,
                targets = listOf(ChosenTarget.Player(opponent)),
            )
        ).error shouldBe null
        drain()
    }

    test("chapter I strips a nonland card from the targeted opponent's hand") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        driver.putCardInHand(opponent, "Grizzly Bears")
        val handBefore = driver.getHandSize(opponent)

        driver.castSaga(me, opponent)

        withClue("the Saga entered and chapter I resolved") {
            (driver.findPermanent(me, "Down, Down to Goblin-town") != null) shouldBe true
        }
        withClue("the nonland card is the only legal pick, so it goes to the graveyard") {
            driver.getGraveyardCardNames(opponent).contains("Grizzly Bears") shouldBe true
            driver.getHandSize(opponent) shouldBe handBefore - 1
        }
        withClue("the Saga is at lore I — chapter II has not happened yet") {
            val saga = driver.findPermanent(me, "Down, Down to Goblin-town")!!
            driver.state.getEntity(saga)?.get<CountersComponent>()?.getCount(CounterType.LORE) shouldBe 1
        }
        withClue("no Army yet — amass is chapter II") {
            driver.getPermanents(me).none { driver.getCardName(it) == "Goblin Army" } shouldBe true
        }
    }

    test("chapter I strips nothing when the opponent's hand is all lands") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        // The deck is all Swamps, so the opening hand has no nonland card to strip.
        withClue("precondition: the opponent's hand is lands only") {
            driver.getHand(opponent).all { driver.getCardName(it) == "Swamp" } shouldBe true
        }
        val handBefore = driver.getHandSize(opponent)

        driver.castSaga(me, opponent)

        withClue("a nonland-only filter finds nothing, so no card is discarded") {
            driver.getHandSize(opponent) shouldBe handBefore
            driver.getGraveyard(opponent).isEmpty() shouldBe true
        }
        withClue("the chapter still resolved and the Saga is on the battlefield") {
            (driver.findPermanent(me, "Down, Down to Goblin-town") != null) shouldBe true
        }
    }
})
