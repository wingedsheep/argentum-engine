package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh2.cards.JadeAvenger
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Jade Avenger (MH2 #167) — {1}{G} Creature — Frog Samurai 2/2.
 *
 * Oracle: "Bushido 2 (Whenever this creature blocks or becomes blocked, it gets +2/+2 until end of
 * turn.)"
 *
 * **The first bushido card in the corpus.** `Keyword.BUSHIDO` is display-only vocabulary — nothing
 * in `rules-engine` reads it — so the ability CR 702.45a spells out is lowered by hand on the card
 * as two triggers over the two distinct events (`Triggers.Blocks` and `Triggers.BecomesBlocked`),
 * each pumping `EffectTarget.Self`. These tests pin that lowering end to end, because a card that
 * carried only the keyword would compile, read correctly in the client, and do nothing.
 *
 * Covered:
 *  1. Blocking fires bushido — the Avenger is a 4/4 for the rest of the turn.
 *  2. Being blocked fires it too — the other half of the same printed ability.
 *  3. Attacking *unblocked* fires neither, so the split into two triggers can't double-count or
 *     leak a bonus into a combat where bushido does nothing.
 */
class JadeAvengerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(JadeAvenger)
        return driver
    }

    fun GameTestDriver.power(id: EntityId): Int = state.projectedState.getPower(id) ?: 0
    fun GameTestDriver.toughness(id: EntityId): Int = state.projectedState.getToughness(id) ?: 0

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: bushido, half one — "whenever this creature blocks"
    // ─────────────────────────────────────────────────────────────────────────
    test("blocking gives Jade Avenger +2/+2 until end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val bears = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        driver.removeSummoningSickness(bears)
        val avenger = driver.putCreatureOnBattlefield(defender, "Jade Avenger")

        driver.power(avenger) shouldBe 2
        driver.toughness(avenger) shouldBe 2

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(attacker, listOf(bears), defender)
        driver.bothPass()

        driver.declareBlockers(defender, mapOf(avenger to listOf(bears)))
        // Resolve the blocks trigger.
        driver.bothPass()

        driver.power(avenger) shouldBe 4
        driver.toughness(avenger) shouldBe 4
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: bushido, half two — "or becomes blocked"
    // ─────────────────────────────────────────────────────────────────────────
    test("becoming blocked gives Jade Avenger +2/+2 until end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val defender = driver.getOpponent(me)

        val avenger = driver.putCreatureOnBattlefield(me, "Jade Avenger")
        driver.removeSummoningSickness(avenger)
        val blocker = driver.putCreatureOnBattlefield(defender, "Grizzly Bears")

        driver.power(avenger) shouldBe 2
        driver.toughness(avenger) shouldBe 2

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(avenger), defender)
        driver.bothPass()

        driver.declareBlockers(defender, mapOf(blocker to listOf(avenger)))
        // Resolve the becomes-blocked trigger.
        driver.bothPass()

        driver.power(avenger) shouldBe 4
        driver.toughness(avenger) shouldBe 4
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: control — an unblocked attack fires neither half
    // ─────────────────────────────────────────────────────────────────────────
    test("attacking unblocked leaves Jade Avenger a 2/2") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val me = driver.activePlayer!!
        val defender = driver.getOpponent(me)

        val avenger = driver.putCreatureOnBattlefield(me, "Jade Avenger")
        driver.removeSummoningSickness(avenger)
        // The defender has nothing to block with.

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(avenger), defender)
        driver.bothPass()

        driver.declareBlockers(defender, emptyMap())
        driver.bothPass()

        driver.power(avenger) shouldBe 2
        driver.toughness(avenger) shouldBe 2
    }
})
