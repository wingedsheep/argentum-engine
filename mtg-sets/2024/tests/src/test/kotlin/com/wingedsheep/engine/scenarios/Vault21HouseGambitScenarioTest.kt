package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.Vault21HouseGambit
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens

/**
 * Vault 21: House Gambit (PIP #69).
 *
 * Chapters I and II loot; chapter III reveals up to five nonland cards from hand and makes a
 * Treasure for each revealed card whose mana value matches another revealed card's.
 */
class Vault21HouseGambitScenarioTest : FunSpec({
    // Adds a lore counter to the Saga so chapters II and III trigger without walking turns.
    val loreProbe = card("Vault 21 Lore Probe") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        spell {
            val saga = target(TargetFilter.Enchantment)
            effect = Effects.AddCounters(CounterType.LORE, 1, saga)
        }
    }
    val sagaName = Vault21HouseGambit.name

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (guard++ < 40 && state.stack.isNotEmpty() && !isPaused) {
            passPriority(state.priorityPlayerId!!)
        }
    }

    fun GameTestDriver.treasures(player: EntityId): Int =
        getPermanents(player).count { getCardName(it) == "Treasure" }

    /** Answers a loot chapter's discard with a Plains, then lets the draw resolve. */
    fun GameTestDriver.discardPlains(me: EntityId) {
        val decision = pendingDecision as SelectCardsDecision
        val plains = decision.options.first { getCardName(it) == "Plains" }
        submitCardSelection(me, listOf(plains)).error shouldBe null
        resolveAll()
    }

    fun GameTestDriver.addLore(me: EntityId) {
        val probe = putCardInHand(me, loreProbe.name)
        giveMana(me, Color.RED, 1)
        castSpell(me, probe, listOf(findPermanent(me, sagaName)!!)).error shouldBe null
        resolveAll()
    }

    /** Casts the Saga and walks chapters I and II (discarding Plains), leaving chapter III pending. */
    fun GameTestDriver.reachChapterThree(me: EntityId) {
        val saga = putCardInHand(me, sagaName)
        giveMana(me, Color.RED, 2)
        castSpell(me, saga).error shouldBe null
        resolveAll()
        discardPlains(me)
        addLore(me)
        discardPlains(me)
        addLore(me)
    }

    fun setUp(): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(Vault21HouseGambit, loreProbe) + PredefinedTokens.allTokens)
        d.initMirrorMatch(Deck.of("Plains" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d to d.activePlayer!!
    }

    test("chapters I and II loot, chapter III makes a Treasure per revealed card sharing a mana value") {
        val (d, me) = setUp()
        val bolt = d.putCardInHand(me, "Lightning Bolt") // MV 1
        val growth = d.putCardInHand(me, "Giant Growth") // MV 1
        val bears = d.putCardInHand(me, "Grizzly Bears") // MV 2
        val counterspell = d.putCardInHand(me, "Counterspell") // MV 2
        val giant = d.putCardInHand(me, "Hill Giant") // MV 4
        val elves = d.putCardInHand(me, "Llanowar Elves") // MV 1
        val handBefore = d.getHandSize(me)

        d.reachChapterThree(me)
        // Two loots: hand size unchanged, two Plains in the graveyard.
        d.getHandSize(me) shouldBe handBefore
        d.state.getZone(me, Zone.GRAVEYARD).count { d.getCardName(it) == "Plains" } shouldBe 2

        val reveal = d.pendingDecision as SelectCardsDecision
        reveal.options shouldContainExactlyInAnyOrder listOf(bolt, growth, bears, counterspell, giant, elves)
        reveal.maxSelections shouldBe 5
        d.submitCardSelection(me, listOf(bolt, growth, bears, counterspell, giant)).error shouldBe null
        d.resolveAll()

        // Bolt + Growth share MV 1, Bears + Counterspell share MV 2; Hill Giant stands alone.
        d.treasures(me) shouldBe 4
        listOf(bolt, growth, bears, counterspell, giant).forEach { (it in d.getHand(me)) shouldBe true }
        findSaga(d, me) shouldBe null
    }

    test("revealed cards with all-distinct mana values create no Treasures") {
        val (d, me) = setUp()
        val bolt = d.putCardInHand(me, "Lightning Bolt") // MV 1
        val bears = d.putCardInHand(me, "Grizzly Bears") // MV 2
        val giant = d.putCardInHand(me, "Hill Giant") // MV 4
        d.putCardInHand(me, "Giant Growth") // MV 1, not revealed

        d.reachChapterThree(me)
        d.submitCardSelection(me, listOf(bolt, bears, giant)).error shouldBe null
        d.resolveAll()

        d.treasures(me) shouldBe 0
    }
})

private fun findSaga(d: GameTestDriver, me: EntityId): EntityId? = d.findPermanent(me, Vault21HouseGambit.name)
