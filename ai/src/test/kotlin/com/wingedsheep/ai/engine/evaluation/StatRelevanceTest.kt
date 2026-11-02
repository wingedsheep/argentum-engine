package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * [CreatureValuation.statsReadTheBoard]: power and toughness priced by what they do against the
 * table ([StatRelevance]) instead of linearly.
 *
 * Asserted on one creature's value at a time, like [BoardPresenceCreatureValuationTest], so the
 * claim is about this number and not about what the rest of the evaluator does with it.
 */
class StatRelevanceTest : ScenarioTestBase() {

    init {
        val intents = IntentCatalog.of(cardRegistry)
        val legacy = CreatureValuation.LEGACY
        val readsTheBoard = CreatureValuation(statsReadTheBoard = true)
        val none = emptySet<String>()

        fun GameState.idOf(name: String, controller: EntityId): EntityId =
            getBattlefield().first {
                getEntity(it)?.get<CardComponent>()?.name == name && projectedState.getController(it) == controller
            }

        fun GameState.valueOf(name: String, controller: EntityId, valuation: CreatureValuation): Double {
            val id = idOf(name, controller)
            val card = getEntity(id)!!.get<CardComponent>()!!
            return BoardPresence.permanentValue(this, projectedState, id, card, intents, valuation)
        }

        // ── Diminishing returns ──

        test("a 1000/1000 is worth less than twice a 20/20, not fifty times") {
            val linear = BoardPresence.creatureBodyValue(1000, 1000, none) /
                BoardPresence.creatureBodyValue(20, 20, none)
            linear shouldBe (50.0 plusOrMinus 0.001)

            val huge = BoardPresence.creatureBodyValue(1000, 1000, none, diminishingReturns = true)
            val big = BoardPresence.creatureBodyValue(20, 20, none, diminishingReturns = true)
            huge / big shouldBeLessThan 2.0
            huge shouldBeGreaterThan big
        }

        test("ordinary creatures are below the knees and keep their linear price") {
            BoardPresence.creatureBodyValue(3, 3, none, diminishingReturns = true) shouldBe (4.2 plusOrMinus EPSILON)
            BoardPresence.creatureBodyValue(10, 8, none, diminishingReturns = true) shouldBe
                (BoardPresence.creatureBodyValue(10, 8, none) plusOrMinus EPSILON)
        }

        // ── Board thresholds ──

        test("a 3/3 facing 2/2s is worth more than the same 3/3 facing a 6/4") {
            fun facing(theirs: String) = scenario().withPlayers()
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, theirs)
                .build()

            val outclassed = facing("Grizzly Bears")
            val outclassedBy = facing("Craw Wurm")
            val gap = outclassed.state.valueOf("Hill Giant", outclassed.player1Id, readsTheBoard) -
                outclassedBy.state.valueOf("Hill Giant", outclassedBy.player1Id, readsTheBoard)
            // Beats the Bears (kills and survives them); against the Wurm it does neither.
            gap shouldBe ((StatRelevance.BEATS + StatRelevance.OUTLASTS) plusOrMinus EPSILON)

            withClue("the linear reading cannot tell the two boards apart") {
                outclassed.state.valueOf("Hill Giant", outclassed.player1Id, legacy) shouldBe
                    (outclassedBy.state.valueOf("Hill Giant", outclassedBy.player1Id, legacy) plusOrMinus EPSILON)
            }
        }

        test("toughness is no defence against deathtouch") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Typhoid Rats")
                .build()
            val context = StatContext.facing(game.state, game.state.projectedState, game.player1Id, null)

            // It kills the Rats, but the Rats kill it too: a trade, which earns nothing.
            StatRelevance.thresholdBonus(3, 3, none, context) shouldBe (0.0 plusOrMinus EPSILON)
        }

        test("an even trade earns neither side anything, so trading Giant for Giant is not a loss") {
            // The PressWhenAheadTest shape: their Giants cannot kill our Serra Angel, ours can kill
            // their Giants — but neither Giant survives the other, so neither is paid.
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Serra Angel")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
            val ours = StatContext.facing(game.state, game.state.projectedState, game.player1Id, null)
            val theirs = StatContext.facing(game.state, game.state.projectedState, game.player2Id, null)

            StatRelevance.thresholdBonus(3, 3, none, ours) shouldBe (0.0 plusOrMinus EPSILON)
            StatRelevance.thresholdBonus(3, 3, none, theirs) shouldBe (0.0 plusOrMinus EPSILON)
        }

        test("on the whole board, a creature theirs beats adds nothing to their worth") {
            // Their Hill Giant facing our Alpine Grizzly (4/2), with and without our Savannah Lions
            // (2/1) beside it. The Giant beats the Lions, not the Grizzly. As a share, the Lions
            // raised the Giant from none of our board to half of it — so losing the Lions for
            // nothing made their Giant cheaper and read as a gain for us.
            fun giant(withLions: Boolean, wholeBoard: Boolean): Double {
                val powers = if (withLions) intArrayOf(4, 2) else intArrayOf(4)
                val toughnesses = if (withLions) intArrayOf(2, 1) else intArrayOf(2)
                return StatRelevance.thresholdBonus(3, 3, none, StatContext(powers, toughnesses, null, wholeBoard))
            }
            giant(withLions = true, wholeBoard = true) shouldBe (giant(withLions = false, wholeBoard = true) plusOrMinus EPSILON)
            giant(withLions = true, wholeBoard = false) shouldBeGreaterThan giant(withLions = false, wholeBoard = false)
        }

        test("on the whole board, it agrees with the share at both ends, and killing the last resister pays in full") {
            // Our Hill Giant facing their Grizzly Bears (beaten) and Craw Wurm (resists it), then the
            // Bears alone.
            val withWurm = StatContext(intArrayOf(2, 6), intArrayOf(2, 4), null, wholeBoard = true)
            StatRelevance.thresholdBonus(3, 3, none, withWurm) shouldBe (0.0 plusOrMinus EPSILON)
            val bearsAlone = StatContext(intArrayOf(2), intArrayOf(2), null, wholeBoard = true)
            StatRelevance.thresholdBonus(3, 3, none, bearsAlone) shouldBe
                ((StatRelevance.BEATS + StatRelevance.OUTLASTS) plusOrMinus EPSILON)
            StatRelevance.thresholdBonus(3, 3, none, StatContext(intArrayOf(), intArrayOf(), null, wholeBoard = true)) shouldBe
                ((StatRelevance.BEATS + StatRelevance.OUTLASTS) plusOrMinus EPSILON)
        }

        test("an empty opposing board satisfies both fight thresholds, so killing their last creature costs nothing") {
            val game = scenario().withPlayers().withCardOnBattlefield(1, "Grizzly Bears").build()
            val context = StatContext.facing(game.state, game.state.projectedState, game.player1Id, null)

            StatRelevance.thresholdBonus(2, 2, none, context) shouldBe
                ((StatRelevance.BEATS + StatRelevance.OUTLASTS) plusOrMinus EPSILON)
        }

        // ── Shown removal ──

        test("a Lightning Bolt in their graveyard makes four toughness worth more than three") {
            val game = scenario().withPlayers()
                .withCardInGraveyard(2, "Lightning Bolt")
                .build()
            val reach = StatContext.shownReachAt(game.state, intents)
            reach[game.player2Id] shouldBe 3
            reach[game.player1Id] shouldBe null

            val context = StatContext.facing(game.state, game.state.projectedState, game.player1Id, reach)
            val outOfReach = StatRelevance.thresholdBonus(6, 4, none, context)
            val inReach = StatRelevance.thresholdBonus(3, 3, none, context)
            outOfReach - inReach shouldBe (StatRelevance.OUT_OF_REACH plusOrMinus EPSILON)
        }

        test("without card knowledge nothing has been shown") {
            val game = scenario().withPlayers().withCardInGraveyard(2, "Lightning Bolt").build()
            StatContext.shownReachAt(game.state, IntentCatalog.NONE) shouldBe emptyMap()
        }
    }

    private companion object {
        const val EPSILON = 1e-9
    }
}
