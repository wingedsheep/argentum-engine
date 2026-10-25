package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fic.cards.KefkaDancingMad
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Kefka, Dancing Mad — "At the beginning of your end step, exile a card at random from each
 * opponent's graveyard. You may cast any number of spells from among cards exiled this way without
 * paying their mana costs. Then each player who owns a spell you cast this way loses life equal to
 * its mana value." Plus "During your turn, Kefka has indestructible."
 *
 * The life loss reads the free-cast loop's record of what it actually cast: a declined card, or one
 * that couldn't be cast for want of a legal target, costs its owner nothing.
 */
class KefkaDancingMadScenarioTest : FunSpec({

    fun setup(playerCount: Int = 2): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + KefkaDancingMad)
        val players = d.initMultiplayer(
            decks = List(playerCount) { Deck.of("Mountain" to 40) },
            skipMulligans = true,
            startingPlayer = 0,
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(players[0], "Kefka, Dancing Mad")
        return d to players
    }

    /** Pass until Kefka's end-step trigger resolves into its first cast prompt (or finishes). */
    fun GameTestDriver.resolveToPrompt() {
        passPriorityUntil(Step.END)
        var guard = 0
        while (pendingDecision == null && stackSize > 0 && guard++ < 20) passPriority(state.priorityPlayerId!!)
    }

    fun GameTestDriver.drainStack() {
        var guard = 0
        while (stackSize > 0 && guard++ < 40) {
            if (pendingDecision is SelectCardsDecision) submitCardSelection(state.priorityPlayerId!!, emptyList())
            else passPriority(state.priorityPlayerId!!)
        }
    }

    test("a spell cast this way costs its owner life equal to its mana value") {
        val (d, players) = setup()
        val (me, opp) = players
        val bears = d.putCardInGraveyard(opp, "Grizzly Bears")

        d.resolveToPrompt()
        withClue("the random graveyard card is exiled") { d.getExile(opp) shouldContain bears }
        val prompt = d.pendingDecision as SelectCardsDecision
        prompt.options shouldBe listOf(bears)
        d.submitCardSelection(me, listOf(bears)).error shouldBe null

        withClue("Grizzly Bears (mana value 2) was cast, so its owner loses 2") { d.getLifeTotal(opp) shouldBe 18 }
        d.getLifeTotal(me) shouldBe 20
        d.drainStack()
        withClue("the spell resolves under Kefka's controller") { d.getCreatures(me) shouldContain bears }
    }

    test("declining the cast costs nothing and leaves the card in exile") {
        val (d, players) = setup()
        val (me, opp) = players
        val bears = d.putCardInGraveyard(opp, "Grizzly Bears")

        d.resolveToPrompt()
        d.submitCardSelection(me, emptyList()).error shouldBe null

        d.getLifeTotal(opp) shouldBe 20
        d.getExile(opp) shouldContain bears
    }

    test("a spell with no legal target is not cast, so its owner loses nothing") {
        val (d, players) = setup()
        val (me, opp) = players
        // Counterspell needs a spell to target; only Kefka's ability is on the stack.
        val counterspell = d.putCardInGraveyard(opp, "Counterspell")

        d.resolveToPrompt()
        (d.pendingDecision as SelectCardsDecision).options shouldBe listOf(counterspell)
        d.submitCardSelection(me, listOf(counterspell)).error shouldBe null
        d.drainStack()

        d.getLifeTotal(opp) shouldBe 20
        d.getExile(opp) shouldContain counterspell
    }

    test("in multiplayer each opponent loses life for the spell they owned, across a targeted cast") {
        val (d, players) = setup(playerCount = 3)
        val me = players[0]
        val oppA = players[1]
        val oppB = players[2]
        val bears = d.putCardInGraveyard(oppA, "Grizzly Bears")
        val bolt = d.putCardInGraveyard(oppB, "Lightning Bolt")

        d.resolveToPrompt()
        withClue("one card is exiled from each opponent's graveyard") {
            d.getExile(oppA) shouldContain bears
            d.getExile(oppB) shouldContain bolt
        }
        (d.pendingDecision as SelectCardsDecision).options.toSet() shouldBe setOf(bears, bolt)

        // Bolt first: its cast pauses for a target, and the record must survive the pause.
        d.submitCardSelection(me, listOf(bolt)).error shouldBe null
        (d.pendingDecision is ChooseTargetsDecision) shouldBe true
        d.submitTargetSelection(me, listOf(oppB)).error shouldBe null
        (d.pendingDecision as SelectCardsDecision).options shouldBe listOf(bears)
        d.submitCardSelection(me, listOf(bears)).error shouldBe null

        withClue("each owner loses its spell's mana value before the spells resolve") {
            d.getLifeTotal(oppA) shouldBe 18
            d.getLifeTotal(oppB) shouldBe 19
            d.getLifeTotal(me) shouldBe 20
        }
        d.drainStack()
        withClue("then Lightning Bolt resolves at its target") { d.getLifeTotal(oppB) shouldBe 16 }
    }

    test("a land exiled this way is never offered, and an empty graveyard gives nothing") {
        val (d, players) = setup(playerCount = 3)
        val oppA = players[1]
        val oppB = players[2]
        val mountain = d.putCardInGraveyard(oppA, "Mountain")

        d.resolveToPrompt()

        withClue("the land is exiled but not offered, so the trigger finishes without a prompt") {
            d.getExile(oppA) shouldContain mountain
            d.pendingDecision shouldBe null
            d.stackSize shouldBe 0
        }
        players.forEach { d.getLifeTotal(it) shouldBe 20 }
        d.getExile(oppB) shouldBe emptyList()
    }

    test("Kefka has indestructible only during its controller's turn") {
        val (d, players) = setup()
        val kefka = d.getCreatures(players[0]).single()
        d.state.projectedState.hasKeyword(kefka, Keyword.INDESTRUCTIBLE) shouldBe true
        d.passPriorityUntil(Step.UPKEEP, players[1])
        d.state.projectedState.hasKeyword(kefka, Keyword.INDESTRUCTIBLE) shouldBe false
    }
})
