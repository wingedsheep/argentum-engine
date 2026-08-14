package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The one feature, measured on its own rather than through an evaluator.
 *
 * `PuzzleSuiteTest` measures the outcome (`respond-02`, with the rest of its category as the
 * control); this pins the *shape* of the discount. Four claims, one test each:
 *
 *  1. The bar is **their open mana** — a tapped-out caster is answered, a rich one is not.
 *  2. The worth is **what the spell is**, not what it cost, which is why the same anthem is worth
 *     countering behind a board and not behind an empty one.
 *  3. It is **scoped** — instants and sorceries are out, which is what keeps `respond-03`'s Wrath
 *     and `respond-04`'s Murder safe by construction rather than by the size of a number.
 *  4. It **ends** — an empty hand across the table, and a late turn, both take it to zero.
 */
class CounterPatienceTest : ScenarioTestBase() {

    /** The default `EvaluationWeights.boardPresence`, which is what every profile here runs on. */
    private val boardPresence = 1.5

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    /**
     * The discount for seat 1 countering whatever seat 2 has on the stack, reading the spell off
     * real game state rather than synthesizing components.
     */
    private fun TestGame.counterDiscount(): Double {
        val spellId = state.stack.singleOrNull() ?: error("expected exactly one spell on the stack")
        return CounterPatience.discount(
            state,
            player1Id,
            intents.forName("Counterspell")!!,
            listOf(ChosenTarget.Spell(spellId)),
            intents,
            boardPresence,
        )
    }

    /**
     * `respond-02`'s board: seat 2 has lands, a grip, and has just cast [spell] off them. The land
     * counts are exact because the bar is `1.4 × the ones still untapped` — casting a two-drop off
     * eight lands leaves six.
     */
    private fun position(
        spell: String,
        forests: Int = 0,
        plains: Int = 0,
        grip: Int = 3,
        theirCreatures: Int = 0,
        turn: Int = 1,
    ) = scenario()
        .withPlayers()
        .withTurnNumber(turn)
        .withActivePlayer(2)
        .apply { if (forests > 0) withLandsOnBattlefield(2, "Forest", forests) }
        .apply { if (plains > 0) withLandsOnBattlefield(2, "Plains", plains) }
        .withCardInHand(2, spell)
        .withCardsInHand(2, "Craw Wurm", grip)
        .apply { repeat(theirCreatures) { withCardOnBattlefield(2, "Grizzly Bears") } }
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardInHand(1, "Counterspell")
        .build()
        .also { it.castSpell(2, spell) }

    init {
        test("the bar is the mana they still have open, so a tapped-out caster is answered") {
            // Grizzly Bears prices at 2.8 of board value. Cast off eight Forests it leaves six
            // untapped, so the bar is 1.4 x 6 = 8.4 and the Bears is 5.6 short of it.
            position("Grizzly Bears", forests = 8).counterDiscount() shouldBe
                (boardPresence * (1.4 * 6 - 2.8)).plusOrMinus(0.01)

            // The same spell with nothing left behind it: no better spell is coming this turn, the
            // bar collapses to zero, and the counter is spent on what is in front of it. This is
            // `respond-01`, `-03`, `-04` and `-06` in one line — every counter the AI *should* make
            // in the suite is cast by an opponent who tapped out.
            position("Grizzly Bears", forests = 2).counterDiscount() shouldBe 0.0
        }

        test("a spell worth more than the bar is countered however much mana is behind it") {
            // Craw Wurm is a 6/4 and prices at 7.6. Off twelve lands it leaves six open — a bar of
            // 8.4 — and is charged the 0.8 it falls short; off eight it leaves two, and clears the
            // bar outright. The same card, ranked by what else the turn could still produce.
            position("Craw Wurm", forests = 12).counterDiscount() shouldBeGreaterThan 0.0
            position("Craw Wurm", forests = 8).counterDiscount() shouldBe 0.0
        }

        test("an anthem is priced by the board it would pump, not by its mana value") {
            // The case that decides the shape of the whole feature. Glorious Anthem is the same
            // three-mana card in both positions and the caster has the same three mana left open —
            // a bar of 4.2. Behind an empty board the anthem is worth its 3.0 prior and is charged;
            // behind five creatures it is worth 3.0 + 5 x 0.25 and clears the bar.
            val alone = position("Glorious Anthem", plains = 6, theirCreatures = 0).counterDiscount()
            val backedUp = position("Glorious Anthem", plains = 6, theirCreatures = 5).counterDiscount()

            alone shouldBe (boardPresence * (1.4 * 3 - 3.0)).plusOrMinus(0.01)
            backedUp shouldBe 0.0
        }

        test("a sorcery is never charged") {
            // Half of the negative control: `respond-03` counters a Wrath with the caster's mana
            // still up. Its worth is what it does to the board, which the leaf score already
            // simulates, so this declines outright rather than trying to price it.
            position("Wrath of God", plains = 8).counterDiscount() shouldBe 0.0
        }

        test("an instant is never charged either, however small its target") {
            // The other half: `respond-04`'s Murder, aimed at our own creature, off a board with
            // five lands still open.
            val game = scenario()
                .withPlayers()
                .withActivePlayer(2)
                .withLandsOnBattlefield(2, "Swamp", 8)
                .withCardInHand(2, "Murder")
                .withCardsInHand(2, "Craw Wurm", 3)
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInHand(1, "Counterspell")
                .withCardOnBattlefield(1, "Serra Angel")
                .build()
            game.castSpell(2, "Murder", game.findPermanent("Serra Angel"))

            game.counterDiscount() shouldBe 0.0
        }

        test("an opponent with an empty hand has nothing better coming") {
            position("Grizzly Bears", forests = 8, grip = 0).counterDiscount() shouldBe 0.0
        }

        test("patience decays with the turn and is gone by the time the bet is bad") {
            fun onTurn(turn: Int) = position("Grizzly Bears", forests = 8, turn = turn).counterDiscount()

            val early = onTurn(Patience.FULL_THROUGH_TURN)
            val middle = onTurn((Patience.FULL_THROUGH_TURN + Patience.SPENT_BY_TURN) / 2)
            early shouldBeGreaterThan 0.0
            middle shouldBeGreaterThan 0.0
            middle shouldBeLessThan early
            onTurn(Patience.SPENT_BY_TURN) shouldBe 0.0
        }

        test("the hold policy stays silent unless the profile asks for counter patience") {
            val game = position("Grizzly Bears", forests = 8)
            val cast = CastSpell(
                playerId = game.player1Id,
                cardId = game.findCardsInHand(1, "Counterspell").first(),
                targets = listOf(ChosenTarget.Spell(game.state.stack.single())),
            )

            // Off is the pre-change behaviour: a counterspell with something on the stack is in its
            // window, and nothing charges it for what it is pointed at.
            HoldPolicy(intents)
                .verdictFor(game.state, game.player1Id, "Counterspell", cast) shouldBe TimingVerdict.Neutral

            HoldPolicy(intents, holdCountersForBetterSpells = true)
                .verdictFor(game.state, game.player1Id, "Counterspell", cast)
                .shouldBeInstanceOf<TimingVerdict.Adjust>()
                .delta shouldBeLessThan 0.0
        }
    }
}
