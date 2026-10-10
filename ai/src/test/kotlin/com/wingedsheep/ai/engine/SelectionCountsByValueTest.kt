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
 * four-drop creature is worth drawing; another land is not.
 */
class SelectionCountsByValueTest : FunSpec({

    val vow = MtgSetCatalog.requireByCode("VOW")
    val allCards = (vow.cards + vow.basicLands).distinctBy { it.name }

    /** A game at [me]'s precombat main: six Swamps out, two in hand, the rest in the library. */
    fun floodedGame(): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver().apply {
            registerCards(allCards)
            initMirrorMatch(Deck.of("Swamp" to 40))
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val me = d.activePlayer!!
        var s = d.state
        for (id in s.getZone(me, Zone.HAND)) {
            s = s.moveToZone(id, ZoneKey(me, Zone.HAND), ZoneKey(me, Zone.LIBRARY))
        }
        d.replaceState(s)
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
})
