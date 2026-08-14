package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.TheMistyMountainsCold
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
 * The Misty Mountains Cold — {2}{R} Enchantment — Saga (HOB #104).
 *
 * I, II, III, IV — Create a Treasure token. Then if you control four or more Treasures, sacrifice
 * this Saga. If you do, create a 6/6 red Dragon creature token with flying.
 *
 * The two gates are what these tests pin down: the state check ("four or more Treasures", counted
 * *after* the chapter's own Treasure is minted) and the "If you do" gate on the sacrifice actually
 * happening. Getting either wrong hands out a free Dragon or never hands one out at all.
 */
class TheMistyMountainsColdScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + PredefinedTokens.allTokens + listOf(TheMistyMountainsCold))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
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

    fun GameTestDriver.treasures(playerId: EntityId): List<EntityId> =
        getPermanents(playerId).filter { getCardName(it) == "Treasure" }

    fun GameTestDriver.dragons(playerId: EntityId): List<EntityId> =
        getPermanents(playerId).filter { getCardName(it) == "Dragon Token" }

    /** Cast the Saga and let chapter I resolve. */
    fun GameTestDriver.castSaga(playerId: EntityId): GameTestDriver {
        giveMana(playerId, Color.RED, 3)
        val card = putCardInHand(playerId, "The Misty Mountains Cold")
        castSpell(playerId, card).error shouldBe null
        drain()
        return this
    }

    test("chapter I under four Treasures: makes a Treasure, keeps the Saga, no Dragon") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        driver.castSaga(me)

        withClue("chapter I minted exactly one Treasure") {
            driver.treasures(me).size shouldBe 1
        }
        withClue("only one Treasure — the Saga is not sacrificed") {
            (driver.findPermanent(me, "The Misty Mountains Cold") != null) shouldBe true
        }
        withClue("no Dragon without the sacrifice") {
            driver.dragons(me).size shouldBe 0
        }
        withClue("the Saga is at lore I") {
            val saga = driver.findPermanent(me, "The Misty Mountains Cold")!!
            driver.state.getEntity(saga)?.get<CountersComponent>()?.getCount(CounterType.LORE) shouldBe 1
        }
    }

    test("chapter I with three Treasures already out: the fourth triggers the sacrifice and the Dragon") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        repeat(3) { driver.putPermanentOnBattlefield(me, "Treasure") }
        driver.castSaga(me)

        withClue("the chapter's own Treasure is counted, so three on board is enough") {
            driver.treasures(me).size shouldBe 4
        }
        withClue("the Saga sacrificed itself") {
            driver.findPermanent(me, "The Misty Mountains Cold") shouldBe null
            driver.getGraveyardCardNames(me).contains("The Misty Mountains Cold") shouldBe true
        }
        withClue("and 'if you do' paid off with a 6/6 flying Dragon") {
            driver.dragons(me).size shouldBe 1
        }
    }

    test("opponent Treasures do not count — 'you control four or more'") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        repeat(3) { driver.putPermanentOnBattlefield(opponent, "Treasure") }
        driver.castSaga(me)

        withClue("only my own single Treasure counts") {
            driver.treasures(me).size shouldBe 1
        }
        withClue("so the Saga survives and no Dragon appears") {
            (driver.findPermanent(me, "The Misty Mountains Cold") != null) shouldBe true
            driver.dragons(me).size shouldBe 0
        }
    }
})
