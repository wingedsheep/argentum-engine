package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.all.cards.LimDulsVault
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Lim-Dûl's Vault (ALL #107) — "Look at the top five cards of your library. As many times as you
 * choose, you may pay 1 life, put those cards on the bottom of your library in any order, then
 * look at the top five cards of your library. Then shuffle and put the last cards you looked at
 * this way on top in any order."
 */
class LimDulsVaultScenarioTest : FunSpec({

    val names = listOf("Grizzly Bears", "Llanowar Elves", "Lightning Bolt", "Counterspell", "Test Enchantment")

    class Game(val d: GameTestDriver, val you: EntityId, val stacked: List<EntityId>, val events: MutableList<GameEvent>)

    /** Five distinct cards on top of an Island library; Lim-Dûl's Vault cast and resolving. */
    fun castVault(): Game {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(LimDulsVault))
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = d.player1
        val stacked = names.map { d.putCardOnTopOfLibrary(you, it) }.reversed() // top first
        val vault = d.putCardInHand(you, "Lim-Dûl's Vault")
        d.giveMana(you, Color.BLUE, 1)
        d.giveMana(you, Color.BLACK, 1)
        d.castSpell(you, vault).error shouldBe null
        val events = d.bothPass().events.toMutableList()
        return Game(d, you, stacked, events)
    }

    /** Answer the private look at the top five, which comes first on every pass. */
    fun Game.look(expected: List<EntityId>) {
        val look = d.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
        look.cards shouldContainExactlyInAnyOrder expected
        events += d.submitOrderedResponse(you, look.cards).events
    }

    fun Game.nothingRevealed() = withClue("a look is private: nothing is revealed to the table") {
        events.filterIsInstance<CardsRevealedEvent>() shouldBe emptyList()
    }

    test("keep the first five: no life paid, they go back on top in the chosen order") {
        val g = castVault()
        val d = g.d
        val librarySize = d.state.getLibrary(g.you).size

        g.look(g.stacked)
        g.events += d.submitYesNo(g.you, false).events

        val closing = d.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
        closing.cards shouldContainExactlyInAnyOrder g.stacked
        val chosenOrder = g.stacked.reversed()
        g.events += d.submitOrderedResponse(g.you, chosenOrder).events

        d.getLifeTotal(g.you) shouldBe 20
        d.state.getLibrary(g.you).size shouldBe librarySize
        d.state.getLibrary(g.you).take(5) shouldBe chosenOrder
        g.nothingRevealed()
    }

    test("pay 1 life: the five go to the bottom, and the next five are the ones kept on top") {
        val g = castVault()
        val d = g.d
        val library = d.state.getLibrary(g.you)
        val nextFive = library.drop(5).take(5)

        g.look(g.stacked)
        g.events += d.submitYesNo(g.you, true).events
        d.getLifeTotal(g.you) shouldBe 19
        val bottomOrder = d.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
        bottomOrder.cards shouldContainExactlyInAnyOrder g.stacked
        g.events += d.submitOrderedResponse(g.you, g.stacked).events
        d.state.getLibrary(g.you).takeLast(5) shouldBe g.stacked

        withClue("the second look offers the payment again") {
            g.look(nextFive)
            d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        }
        g.events += d.submitYesNo(g.you, false).events

        val topOrder = d.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
        topOrder.cards shouldContainExactlyInAnyOrder nextFive
        g.events += d.submitOrderedResponse(g.you, nextFive).events

        d.getLifeTotal(g.you) shouldBe 19
        d.state.getLibrary(g.you).size shouldBe library.size
        d.state.getLibrary(g.you).take(5) shouldBe nextFive
        d.stackSize shouldBe 0
        g.nothingRevealed()
    }
})
