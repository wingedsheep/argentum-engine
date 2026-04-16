package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.Stormsplitter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Stormsplitter {3}{R} — Otter Wizard 1/4, Haste.
 * "Whenever you cast an instant or sorcery spell, create a token that's a copy
 *  of this creature. Exile that token at the beginning of the next end step."
 *
 * Each Stormsplitter on the battlefield has its own copy of the triggered ability,
 * so one instant cast with N Stormsplitters on the battlefield produces N tokens
 * — doubling the board. Tokens inherit the ability (they're copies), so the next
 * cast doubles again.
 *
 * Engine bugs this guards against:
 *   • Firing only one trigger for the shared cast event (deduping by ability).
 *   • Not attaching the triggered ability to the newly-created token copies.
 *   • Firing triggers retroactively on tokens that didn't exist at cast time
 *     (would create 2^N growth from a single cast).
 */
class StormsplitterTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Stormsplitter))
        return driver
    }

    fun GameTestDriver.countStormsplitters(playerId: EntityId): Int =
        state.getBattlefield(playerId).count { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == "Stormsplitter"
        }

    fun GameTestDriver.countStormsplitterTokens(playerId: EntityId): Int =
        state.getBattlefield(playerId).count { id ->
            val container = state.getEntity(id) ?: return@count false
            container.get<CardComponent>()?.name == "Stormsplitter" &&
                container.has<TokenComponent>()
        }

    fun GameTestDriver.drainStack(maxPasses: Int = 50) {
        var passes = 0
        while (stackSize > 0 && state.pendingDecision == null && passes < maxPasses) {
            bothPass()
            passes++
        }
    }

    test("one instant with 3 Stormsplitters on the battlefield creates 3 token copies") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Three Stormsplitters on battlefield. They have Haste so no need to remove
        // summoning sickness, and triggered abilities fire regardless anyway.
        driver.putCreatureOnBattlefield(activePlayer, "Stormsplitter")
        driver.putCreatureOnBattlefield(activePlayer, "Stormsplitter")
        driver.putCreatureOnBattlefield(activePlayer, "Stormsplitter")
        driver.countStormsplitters(activePlayer) shouldBe 3

        // Put a Lightning Bolt in hand with just enough mana to cast it.
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")

        val cast = driver.castSpellWithTargets(
            activePlayer, bolt, listOf(ChosenTarget.Player(opponent))
        )
        cast.error shouldBe null

        // Drain the 3 Stormsplitter triggers + the bolt itself.
        driver.drainStack()

        driver.countStormsplitters(activePlayer) shouldBe 6
        driver.countStormsplitterTokens(activePlayer) shouldBe 3
    }

    test("exponential doubling: two consecutive instants take the board from 3 -> 6 -> 12") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.putCreatureOnBattlefield(activePlayer, "Stormsplitter")
        driver.putCreatureOnBattlefield(activePlayer, "Stormsplitter")
        driver.putCreatureOnBattlefield(activePlayer, "Stormsplitter")

        // Enough red mana for two bolts.
        driver.giveMana(activePlayer, Color.RED, 2)
        val bolt1 = driver.putCardInHand(activePlayer, "Lightning Bolt")
        val bolt2 = driver.putCardInHand(activePlayer, "Lightning Bolt")

        driver.castSpellWithTargets(
            activePlayer, bolt1, listOf(ChosenTarget.Player(opponent))
        ).error shouldBe null
        driver.drainStack()
        driver.countStormsplitters(activePlayer) shouldBe 6

        driver.castSpellWithTargets(
            activePlayer, bolt2, listOf(ChosenTarget.Player(opponent))
        ).error shouldBe null
        driver.drainStack()

        // 6 Stormsplitters each triggered, each creating one token copy → 12 total.
        driver.countStormsplitters(activePlayer) shouldBe 12
        driver.countStormsplitterTokens(activePlayer) shouldBe 9
    }
})
