package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.apc.cards.LayOfTheLand
import com.wingedsheep.mtg.sets.definitions.tla.cards.WanShiTongLibrarian
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Wan Shi Tong, Librarian (TLA #78) — {X}{U}{U} Legendary Bird Spirit, 1/1.
 *
 * "Flash
 *  Flying, vigilance
 *  When Wan Shi Tong enters, put X +1/+1 counters on him. Then draw half X cards, rounded down.
 *  Whenever an opponent searches their library, put a +1/+1 counter on Wan Shi Tong and draw a card."
 *
 * Exercises:
 *  - the ETB clause reading the paid X for both counters (X) and draw (`floor(X / 2)`), and
 *  - the engine's new opponent-scoped library-search trigger (CR 701.23) — every tutor / fetch /
 *    basic-land search an opponent resolves fires it, while the controller's own searches do not.
 */
class WanShiTongLibrarianScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(WanShiTongLibrarian, LayOfTheLand))
        return d
    }

    fun GameTestDriver.plusOneCounters(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("cast with X=4: enters with 4 +1/+1 counters and you draw 2 (floor(4/2))") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wan = d.putCardInHand(active, "Wan Shi Tong, Librarian")
        d.giveMana(active, Color.BLUE, 2)
        d.giveColorlessMana(active, 4) // X = 4 → {4}{U}{U}
        val handBeforeCast = d.getHandSize(active)

        d.castXSpell(active, wan, xValue = 4).isSuccess shouldBe true
        // Resolve the cast + its ETB trigger, then STOP as soon as the stack is empty — do not keep
        // passing priority into the cleanup step (which would discard the freshly drawn cards back
        // down to the max hand size).
        var guard = 0
        while ((d.pendingDecision != null || d.stackSize > 0) && guard++ < 40) {
            if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass()
        }

        val perm = d.findPermanent(active, "Wan Shi Tong, Librarian")!!
        d.plusOneCounters(perm) shouldBe 4
        // Cast removes Wan Shi from hand (-1); the ETB draws floor(4/2) = 2.
        d.getHandSize(active) shouldBe handBeforeCast - 1 + 2
    }

    test("cast with X=3: enters with 3 +1/+1 counters and you draw 1 (floor(3/2))") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wan = d.putCardInHand(active, "Wan Shi Tong, Librarian")
        d.giveMana(active, Color.BLUE, 2)
        d.giveColorlessMana(active, 3) // X = 3 → {3}{U}{U}
        val handBeforeCast = d.getHandSize(active)

        d.castXSpell(active, wan, xValue = 3).isSuccess shouldBe true
        // Resolve the cast + its ETB trigger, then STOP as soon as the stack is empty — do not keep
        // passing priority into the cleanup step (which would discard the freshly drawn cards back
        // down to the max hand size).
        var guard = 0
        while ((d.pendingDecision != null || d.stackSize > 0) && guard++ < 40) {
            if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass()
        }

        val perm = d.findPermanent(active, "Wan Shi Tong, Librarian")!!
        d.plusOneCounters(perm) shouldBe 3
        // Cast removes Wan Shi from hand (-1); the ETB draws floor(3/2) = 1.
        d.getHandSize(active) shouldBe handBeforeCast - 1 + 1
    }

    test("opponent searching their library gives Wan Shi a +1/+1 counter and its controller draws") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        val searcher = d.activePlayer!!          // active player will resolve the search
        val me = d.getOpponent(searcher)         // I control Wan Shi (non-active)

        val wan = d.putCreatureOnBattlefield(me, "Wan Shi Tong, Librarian")
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val lay = d.putCardInHand(searcher, "Lay of the Land")
        d.giveMana(searcher, Color.GREEN, 1)
        val myHandBefore = d.getHandSize(me)

        d.castSpell(searcher, lay).isSuccess shouldBe true
        // Resolve the cast, its library search, and any resulting trigger, then STOP at an empty
        // stack — passing further would advance into the next turn's draw step and inflate the count.
        var guard = 0
        while ((d.pendingDecision != null || d.stackSize > 0) && guard++ < 40) {
            if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass()
        }

        d.plusOneCounters(wan) shouldBe 1
        d.getHandSize(me) shouldBe myHandBefore + 1
    }

    test("your own library search does NOT trigger it (opponent-scoped)") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        val active = d.activePlayer!!

        val wan = d.putCreatureOnBattlefield(active, "Wan Shi Tong, Librarian")
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val lay = d.putCardInHand(active, "Lay of the Land")
        d.giveMana(active, Color.GREEN, 1)
        val handBefore = d.getHandSize(active)

        d.castSpell(active, lay).isSuccess shouldBe true
        // Resolve the cast, its library search, and any resulting trigger, then STOP at an empty
        // stack — passing further would advance into the next turn's draw step and inflate the count.
        var guard = 0
        while ((d.pendingDecision != null || d.stackSize > 0) && guard++ < 40) {
            if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass()
        }

        // The controller's own search never triggers the opponent-scoped ability.
        d.plusOneCounters(wan) shouldBe 0
        // Lay of the Land leaves hand (-1); the auto-resolved search finds nothing, and Wan Shi adds
        // no draw (its trigger did not fire). Had it fired, the hand would be one higher.
        d.getHandSize(active) shouldBe handBefore - 1
    }

    test("printed keywords: flash (def), flying and vigilance (projected)") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        val active = d.activePlayer!!

        val wan = d.putCreatureOnBattlefield(active, "Wan Shi Tong, Librarian")

        d.cardRegistry.getCard("Wan Shi Tong, Librarian")!!.keywords.contains(Keyword.FLASH) shouldBe true
        d.state.projectedState.hasKeyword(wan, Keyword.FLYING) shouldBe true
        d.state.projectedState.hasKeyword(wan, Keyword.VIGILANCE) shouldBe true
    }
})
