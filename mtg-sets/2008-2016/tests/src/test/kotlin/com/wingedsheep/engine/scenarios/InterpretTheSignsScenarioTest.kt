package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.jou.cards.InterpretTheSigns
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Interpret the Signs — "Scry 3, then reveal the top card of your library. Draw cards equal to
 * that card's mana value."
 */
class InterpretTheSignsScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(InterpretTheSigns))
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Library top → bottom: [first], [second], [third], then the Plains deck. */
    fun GameTestDriver.castWithTop(third: String, second: String, first: String): Triple<EntityId, EntityId, EntityId> {
        val c = putCardOnTopOfLibrary(player1, third)
        val b = putCardOnTopOfLibrary(player1, second)
        val a = putCardOnTopOfLibrary(player1, first)
        val spell = putCardInHand(player1, "Interpret the Signs")
        giveMana(player1, Color.BLUE, 6)
        castSpell(player1, spell).error shouldBe null
        bothPass().error shouldBe null
        return Triple(a, b, c)
    }

    fun GameTestDriver.scry(looked: List<EntityId>, toBottom: List<EntityId>, topOrder: List<EntityId>) {
        val scry = pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        scry.playerId shouldBe player1
        scry.options shouldContainExactlyInAnyOrder looked
        submitCardSelection(player1, toBottom).error shouldBe null
        val pending = pendingDecision
        if (pending is ReorderLibraryDecision) {
            submitDecision(player1, OrderedResponse(pending.id, topOrder)).error shouldBe null
        }
    }

    test("scry keeps a mana value 5 card on top, reveals it, and draws five cards starting with it") {
        val d = driver()
        val (plains, force, courser) = d.castWithTop(third = "Centaur Courser", second = "Force of Nature", first = "Plains")
        val handBefore = d.getHandSize(d.player1)
        val libraryBefore = d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)).size

        d.scry(listOf(plains, force, courser), toBottom = listOf(plains), topOrder = listOf(force, courser))
        d.pendingDecision shouldBe null

        // Force of Nature ({3}{G}{G}) has mana value 5 — it is the first of five cards drawn.
        d.getHandSize(d.player1) shouldBe handBefore + 5
        val hand = d.getHand(d.player1)
        hand shouldContain force
        hand shouldContain courser
        hand shouldNotContain plains
        val library = d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY))
        library.size shouldBe libraryBefore - 5
        library.last() shouldBe plains
    }

    test("reorders after scry so the mana value 3 card is revealed, drawing three") {
        val d = driver()
        val (plains, force, courser) = d.castWithTop(third = "Centaur Courser", second = "Force of Nature", first = "Plains")
        val handBefore = d.getHandSize(d.player1)

        d.scry(listOf(plains, force, courser), toBottom = listOf(force), topOrder = listOf(courser, plains))
        d.pendingDecision shouldBe null

        // Centaur Courser has mana value 3: draws Courser, Plains, and the next deck Plains.
        d.getHandSize(d.player1) shouldBe handBefore + 3
        val hand = d.getHand(d.player1)
        hand shouldContain courser
        hand shouldContain plains
        hand shouldNotContain force
        d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)).last() shouldBe force
    }

    test("revealing a land (mana value 0) draws nothing and leaves it on top") {
        val d = driver()
        val (plains, force, courser) = d.castWithTop(third = "Centaur Courser", second = "Force of Nature", first = "Plains")
        val handBefore = d.getHandSize(d.player1)

        d.scry(listOf(plains, force, courser), toBottom = listOf(force, courser), topOrder = listOf(plains))
        d.pendingDecision shouldBe null

        d.getHandSize(d.player1) shouldBe handBefore
        d.state.getZone(ZoneKey(d.player1, Zone.LIBRARY)).first() shouldBe plains
    }
})
