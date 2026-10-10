package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [AiProfile.selectionCountsByValue], one test per misplay from the 2026-10-10 engine-vs-engine
 * logs. Each test also pins the legacy count, so the misplay it documents stays visible.
 *
 * The board is the flooded one from game 1 turn 12: six lands out, only lands in hand. A castable
 * four-drop creature is worth drawing; another land is worth drawing only when the rest of the
 * library is no better, since scry and surveil price a card against the draw that replaces it.
 */
class SelectionCountsByValueTest : FunSpec({

    val vow = MtgSetCatalog.requireByCode("VOW")
    val allCards = (vow.cards + vow.basicLands).distinctBy { it.name }

    /** Half lands, half castable creatures: the average draw is well worth more than a seventh land. */
    val mixedDeck = Deck.of("Swamp" to 20, "Diregraf Scavenger" to 20)

    /** A game at [me]'s precombat main with an empty hand; the rest of [deck] is the library. */
    fun game(deck: Deck): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver().apply {
            registerCards(allCards)
            initMirrorMatch(deck)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val me = d.activePlayer!!
        var s = d.state
        for (id in s.getZone(me, Zone.HAND)) {
            s = s.moveToZone(id, ZoneKey(me, Zone.HAND), ZoneKey(me, Zone.LIBRARY))
        }
        d.replaceState(s)
        return d to me
    }

    /** [game] with six Swamps out and two in hand. */
    fun floodedGame(deck: Deck = mixedDeck): Pair<GameTestDriver, EntityId> {
        val (d, me) = game(deck)
        repeat(6) { d.putLandOnBattlefield(me, "Swamp") }
        repeat(2) { d.putCardInHand(me, "Swamp") }
        return d to me
    }

    fun choose(
        d: GameTestDriver,
        me: EntityId,
        options: List<EntityId>,
        maxSelections: Int,
        selectedLabel: String?,
        flag: Boolean,
    ): List<EntityId> {
        val decision = SelectCardsDecision(
            id = "test",
            playerId = me,
            prompt = "Choose up to $maxSelections card${if (maxSelections != 1) "s" else ""}",
            context = DecisionContext(phase = DecisionPhase.RESOLUTION),
            options = options,
            minSelections = 0,
            maxSelections = maxSelections,
            selectedLabel = selectedLabel,
            remainderLabel = selectedLabel?.let { "Put on top" },
        )
        val responder = DecisionResponder(
            GameSimulator(d.cardRegistry),
            AIPlayer.defaultEvaluator(),
            intents = IntentCatalog.of(d.cardRegistry),
            castabilityAwareCardSelection = true,
            selectionCountsByValue = flag,
        )
        return (responder.respond(d.state, decision, me) as CardsSelectedResponse).selectedCards
    }

    test("g3 T19: a search whose only candidate fits the count takes it") {
        val (d, me) = floodedGame()
        val land = d.putCardOnTopOfLibrary(me, "Swamp")

        choose(d, me, listOf(land), 1, selectedLabel = null, flag = false) shouldBe emptyList()
        choose(d, me, listOf(land), 1, selectedLabel = null, flag = true) shouldBe listOf(land)
    }

    test("g1 T12: scry keeps a castable creature on top while flooded") {
        val (d, me) = floodedGame()
        val creature = d.putCardOnTopOfLibrary(me, "Diregraf Scavenger")

        choose(d, me, listOf(creature), 1, "Put on bottom", flag = false) shouldBe listOf(creature)
        choose(d, me, listOf(creature), 1, "Put on bottom", flag = true) shouldBe emptyList()
    }

    test("scry 2 bottoms only the land it doesn't need") {
        val (d, me) = floodedGame()
        val land = d.putCardOnTopOfLibrary(me, "Swamp")
        val creature = d.putCardOnTopOfLibrary(me, "Diregraf Scavenger")

        choose(d, me, listOf(creature, land), 2, "Put on bottom", flag = true) shouldBe listOf(land)
    }

    test("g1/g2: surveil mills a dead land and keeps a live card") {
        val (d, me) = floodedGame()
        val land = d.putCardOnTopOfLibrary(me, "Swamp")

        choose(d, me, listOf(land), 1, "Put in graveyard", flag = false) shouldBe emptyList()
        choose(d, me, listOf(land), 1, "Put in graveyard", flag = true) shouldBe listOf(land)

        val creature = d.putCardOnTopOfLibrary(me, "Diregraf Scavenger")
        choose(d, me, listOf(creature), 1, "Put in graveyard", flag = true) shouldBe emptyList()
    }

    test("the replacement draw sets the bar: a seventh land stays on top of a library of lands") {
        // Same land, same board; only the rest of the library differs. An absolute cut-off would
        // mill it both times — but surveilling it away into another Swamp gains nothing.
        val (lands, me1) = floodedGame(Deck.of("Swamp" to 40))
        val onLands = lands.putCardOnTopOfLibrary(me1, "Swamp")
        choose(lands, me1, listOf(onLands), 1, "Put in graveyard", flag = true) shouldBe emptyList()

        val (mixed, me2) = floodedGame(mixedDeck)
        val onMixed = mixed.putCardOnTopOfLibrary(me2, "Swamp")
        choose(mixed, me2, listOf(onMixed), 1, "Put in graveyard", flag = true) shouldBe listOf(onMixed)
    }

    test("scry 2 over two lands keeps the one land it needs and bottoms the second") {
        // Three Swamps out and a four-drop in hand: one more land is exactly what the hand wants.
        // Judged together both lands clear the bar; judged in turn, the first one kept makes the
        // second a fifth land coming.
        val (d, me) = game(mixedDeck)
        repeat(3) { d.putLandOnBattlefield(me, "Swamp") }
        d.putCardInHand(me, "Diregraf Scavenger")
        val first = d.putCardOnTopOfLibrary(me, "Swamp")
        val second = d.putCardOnTopOfLibrary(me, "Swamp")

        choose(d, me, listOf(second, first), 2, "Put on bottom", flag = true).size shouldBe 1
    }

    test("scry 2 keeps the Swamp a {2}{B}{B} spell is short of and bottoms the Plains") {
        // One black source for two black pips. The legacy colour credit fired only when no source
        // made the colour, so it priced the Swamp and the Plains the same and kept the first listed.
        val (d, me) = game(Deck.of("Plains" to 10, "Swamp" to 10, "Diregraf Scavenger" to 20))
        repeat(2) { d.putLandOnBattlefield(me, "Plains") }
        d.putLandOnBattlefield(me, "Swamp")
        d.putCardInHand(me, "Bleed Dry")
        val swamp = d.putCardOnTopOfLibrary(me, "Swamp")
        val plains = d.putCardOnTopOfLibrary(me, "Plains")

        choose(d, me, listOf(plains, swamp), 2, "Put on bottom", flag = true) shouldBe listOf(plains)
    }

    test("scry 2 keeps the Swamp the library's {B}{B} spells want and bottoms the Island") {
        // Nothing in hand asks for a colour; the library is half {2}{B}{B} spells and has no blue
        // ones. One Swamp out leaves those spells a pip short, so the Swamp is the better land —
        // without the library term the two tie and the Island, listed first, is kept.
        val (d, me) = game(Deck.of("Swamp" to 10, "Island" to 10, "Bleed Dry" to 20))
        d.putLandOnBattlefield(me, "Swamp")
        repeat(3) { d.putLandOnBattlefield(me, "Island") }
        val swamp = d.putCardOnTopOfLibrary(me, "Swamp")
        val island = d.putCardOnTopOfLibrary(me, "Island")

        choose(d, me, listOf(island, swamp), 2, "Put on bottom", flag = true) shouldBe listOf(island)
    }
})
