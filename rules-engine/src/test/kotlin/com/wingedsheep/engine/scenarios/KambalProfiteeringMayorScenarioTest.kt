package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.KambalProfiteeringMayor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Kambal, Profiteering Mayor — {1}{W}{B} 2/4 Legendary Creature — Human Advisor
 *
 * 1. "Whenever one or more tokens your opponents control enter, for each of them, create a tapped
 *     token that's a copy of it. This ability triggers only once each turn."
 * 2. "Whenever one or more tokens you control enter, each opponent loses 1 life and you gain 1 life."
 *
 * Exercises:
 * - The you-controlled batch drain (fires once per batch, not per token).
 * - The opponent-controlled batch copy (one tapped copy per opponent token), and that the copies
 *   entering under your control then trigger the drain.
 * - The once-per-turn limit on the copy ability.
 * - Self-controlled tokens never trigger the (opponent-scoped) copy ability.
 */
class KambalProfiteeringMayorScenarioTest : FunSpec({

    fun createDriver(startingPlayer: Int = 0): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KambalProfiteeringMayor))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = startingPlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun mercenaryTokens(driver: GameTestDriver, playerId: EntityId): List<EntityId> =
        driver.getCreatures(playerId).filter {
            val e = driver.state.getEntity(it)
            e?.get<CardComponent>()?.name == "Mercenary Token" && e.has<TokenComponent>()
        }

    fun castFormAPosse(driver: GameTestDriver, playerId: EntityId, x: Int) {
        driver.giveMana(playerId, Color.RED, 1)
        driver.giveMana(playerId, Color.WHITE, 1)
        driver.giveColorlessMana(playerId, x)
        val posse = driver.putCardInHand(playerId, "Form a Posse")
        driver.castXSpell(playerId, posse, xValue = x).error shouldBe null
        // Resolve Form a Posse, then any Kambal triggers it created.
        repeat(6) { driver.bothPass() }
    }

    test("tokens you control entering: each opponent loses 1, you gain 1 — once per batch") {
        val driver = createDriver(startingPlayer = 0)
        val me = driver.player1
        val opp = driver.player2

        driver.putCreatureOnBattlefield(me, "Kambal, Profiteering Mayor")

        // I make two Mercenary tokens. The drain fires once for the whole batch (not twice).
        castFormAPosse(driver, me, x = 2)

        mercenaryTokens(driver, me).size shouldBe 2
        driver.getLifeTotal(opp) shouldBe 19   // 20 - 1
        driver.getLifeTotal(me) shouldBe 21    // 20 + 1
    }

    test("opponent tokens entering: create a tapped copy of each, and the copies drain") {
        // Player 2 is active so it can cast its sorcery; Kambal is on player 1's battlefield.
        val driver = createDriver(startingPlayer = 1)
        val me = driver.player1
        val opp = driver.player2

        driver.putCreatureOnBattlefield(me, "Kambal, Profiteering Mayor")

        // Opponent makes two Mercenary tokens. Kambal copies each (tapped) under my control.
        castFormAPosse(driver, opp, x = 2)

        val oppTokens = mercenaryTokens(driver, opp)
        val myCopies = mercenaryTokens(driver, me)
        oppTokens.size shouldBe 2
        myCopies.size shouldBe 2
        // The copies enter tapped.
        myCopies.all { driver.isTapped(it) } shouldBe true

        // The two copies entered under my control as a batch → drain fires once: opp -1, me +1.
        driver.getLifeTotal(opp) shouldBe 19
        driver.getLifeTotal(me) shouldBe 21
    }

    test("copy ability triggers only once each turn") {
        val driver = createDriver(startingPlayer = 1)
        val me = driver.player1
        val opp = driver.player2

        driver.putCreatureOnBattlefield(me, "Kambal, Profiteering Mayor")

        castFormAPosse(driver, opp, x = 1)
        mercenaryTokens(driver, me).size shouldBe 1   // first batch copied

        // Second batch of opponent tokens the same turn: copy ability already fired → no new copies.
        castFormAPosse(driver, opp, x = 1)
        mercenaryTokens(driver, me).size shouldBe 1   // still only the one copy
        // Opponent now has both its own tokens.
        mercenaryTokens(driver, opp).size shouldBe 2
    }

    test("tokens you control do not trigger the opponent-scoped copy ability") {
        val driver = createDriver(startingPlayer = 0)
        val me = driver.player1

        driver.putCreatureOnBattlefield(me, "Kambal, Profiteering Mayor")

        // I make two tokens: only the drain fires; no copies are made (copy ability is opponent-only).
        castFormAPosse(driver, me, x = 2)

        // Exactly the two I created — no extra copies.
        mercenaryTokens(driver, me).size shouldBe 2
    }
})
