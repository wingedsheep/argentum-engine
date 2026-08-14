package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
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
 * `PuzzleSuiteTest` measures the outcome (`removal-07`/`08`/`09`); this pins the *shape* of the
 * discount, which is the part that distinguishes it from the Phase 6 constant that was built,
 * measured and removed. Three claims, one test each:
 *
 *  1. It is **proportional** — a 1/1 under a Murder is charged, a 3/3 is not, and neither is a 6/4.
 *  2. It is **scoped** — noncreature targets, sweepers and creature bodies with removal riders are
 *     out, which is what keeps `noncreature-01`'s Disenchant safe by construction rather than by
 *     the size of a number.
 *  3. It **ends** — a full hand and a late turn both take it to zero.
 */
class RemovalPatienceTest : ScenarioTestBase() {

    /** The default `EvaluationWeights.boardPresence`, which is what every profile here runs on. */
    private val boardPresence = 1.5

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    /**
     * The discount for casting [spellName] out of seat 1's hand at [targetName], reading both the
     * spell and the target off real game state rather than synthesizing components.
     */
    private fun TestGame.discountOf(spellName: String, targetName: String?): Double {
        val spellId = findCardsInHand(1, spellName).firstOrNull()
            ?: error("$spellName is not in hand")
        val spell = state.getEntity(spellId)?.get<CardComponent>() ?: error("$spellName has no card")
        val targets = targetName
            ?.let { listOf(ChosenTarget.Permanent(findPermanent(it) ?: error("no $it on the battlefield"))) }
            .orEmpty()
        return RemovalPatience.discount(
            state, player1Id, intents.forName(spellName)!!, spell, targets, boardPresence,
        )
    }

    init {
        test("the discount is what the target falls short of the removal's own mana value") {
            // Murder is {1}{B}{B}: a fair trade is a creature worth 3 x 1.4 = 4.2 of board value.
            // Hill Giant (3/3) prices at exactly 4.2 and Craw Wurm (6/4) at 7.6, so both are free;
            // a 1/1 prices at 1.4 and is 2.8 short.
            val game = scenario()
                .withPlayers()
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(1, "Murder")
                .withCardOnBattlefield(2, "Mons's Goblin Raiders")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Craw Wurm")
                .build()

            game.discountOf("Murder", "Hill Giant") shouldBe 0.0
            game.discountOf("Murder", "Craw Wurm") shouldBe 0.0
            // Proportional, not a constant: this is the whole difference from the Phase 6 attempt.
            game.discountOf("Murder", "Mons's Goblin Raiders") shouldBe
                (boardPresence * 2.8).plusOrMinus(0.01)
        }

        test("a cheaper answer is content with a smaller creature") {
            // The bar moves with the card. Lightning Bolt is {R}, so a 1/1 clears it outright while
            // the same creature is 2.8 short of a Murder.
            val game = scenario()
                .withPlayers()
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Murder")
                .withCardOnBattlefield(2, "Mons's Goblin Raiders")
                .build()

            game.discountOf("Lightning Bolt", "Mons's Goblin Raiders") shouldBe 0.0
            game.discountOf("Murder", "Mons's Goblin Raiders") shouldBeGreaterThan 0.0
        }

        test("a Pacifism is judged exactly like a removal spell") {
            // The card that motivated the feature, and the reason `IntentTag.NEUTRALIZE` exists:
            // before it, Pacifism carried no tag at all and no policy could see it.
            val game = scenario()
                .withPlayers()
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(1, "Pacifism")
                .withCardOnBattlefield(2, "Mons's Goblin Raiders")
                .withCardOnBattlefield(2, "Craw Wurm")
                .build()

            game.discountOf("Pacifism", "Mons's Goblin Raiders") shouldBeGreaterThan 0.0
            game.discountOf("Pacifism", "Craw Wurm") shouldBe 0.0
        }

        test("a noncreature target is never charged") {
            // The negative control that matters most: a constant penalty on removal is what vetoed
            // `noncreature-01`'s Disenchant, and this is the line that makes that impossible here.
            val game = scenario()
                .withPlayers()
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(1, "Disenchant")
                .withCardOnBattlefield(2, "Icy Manipulator")
                .build()

            game.discountOf("Disenchant", "Icy Manipulator") shouldBe 0.0
        }

        test("our own creature is not a trade being made") {
            val game = scenario()
                .withPlayers()
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInHand(1, "Pacifism")
                .withCardOnBattlefield(1, "Mons's Goblin Raiders")
                .build()

            game.discountOf("Pacifism", "Mons's Goblin Raiders") shouldBe 0.0
        }

        test("a sweeper names no target to judge") {
            val game = scenario()
                .withPlayers()
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardInHand(1, "Wrath of God")
                .withCardOnBattlefield(2, "Mons's Goblin Raiders")
                .build()

            game.discountOf("Wrath of God", "Mons's Goblin Raiders") shouldBe 0.0
        }

        test("a body with a removal rider keeps the body, so it pays nothing") {
            // Flametongue Kavu is a 4/2 *and* four damage. Charging it for the small target would
            // be charging a card that is not being spent on the removal at all.
            val game = scenario()
                .withPlayers()
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInHand(1, "Flametongue Kavu")
                .withCardOnBattlefield(2, "Mons's Goblin Raiders")
                .build()

            game.discountOf("Flametongue Kavu", "Mons's Goblin Raiders") shouldBe 0.0
        }

        test("there is no bar at all on a turn where doing nothing loses the game") {
            // The guarantee, not the nudge. At 1 life with nothing to block with, the opponent's
            // 1/1 is lethal on board — so the worst target in Magic is the one that has to die, and
            // no amount of patience may say otherwise. The 20-life control is the same board with
            // the same 1/1, and there the discount is back.
            fun atLife(life: Int) = scenario()
                .withPlayers()
                .withLifeTotal(1, life)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(1, "Murder")
                .withCardsInHand(1, "Craw Wurm", 3)
                .withCardOnBattlefield(2, "Mons's Goblin Raiders")
                .build()
                .discountOf("Murder", "Mons's Goblin Raiders")

            atLife(1) shouldBe 0.0
            atLife(20) shouldBeGreaterThan 0.0
        }

        test("a blocker that eats the attack is not lethal, so patience survives it") {
            // The other half: the veto reads *unblockable* damage, not raw power. A 0/8 in the way
            // means the 1/1 is not killing anyone, and holding the Murder is still the right call
            // at any life total.
            val game = scenario()
                .withPlayers()
                .withLifeTotal(1, 1)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(1, "Murder")
                .withCardsInHand(1, "Craw Wurm", 3)
                .withCardOnBattlefield(1, "Wall of Stone")
                .withCardOnBattlefield(2, "Mons's Goblin Raiders")
                .build()

            game.discountOf("Murder", "Mons's Goblin Raiders") shouldBeGreaterThan 0.0
        }

        test("a hand at the discard limit stops holding for free") {
            fun handOf(extraCards: Int) = scenario()
                .withPlayers()
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(1, "Murder")
                .withCardsInHand(1, "Craw Wurm", extraCards)
                .withCardOnBattlefield(2, "Mons's Goblin Raiders")
                .build()

            handOf(3).discountOf("Murder", "Mons's Goblin Raiders") shouldBeGreaterThan 0.0
            // Seven cards, not eight: at the limit the next draw is already the discard, so waiting
            // for the strict overflow would spend the removal a turn late.
            handOf(6).discountOf("Murder", "Mons's Goblin Raiders") shouldBe 0.0
        }

        test("patience decays with the turn and is gone by the time the bet is bad") {
            fun onTurn(turn: Int) = scenario()
                .withPlayers()
                .withTurnNumber(turn)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(1, "Murder")
                .withCardsInHand(1, "Craw Wurm", 3)
                .withCardOnBattlefield(2, "Mons's Goblin Raiders")
                .build()
                .discountOf("Murder", "Mons's Goblin Raiders")

            val early = onTurn(Patience.FULL_THROUGH_TURN)
            val middle = onTurn((Patience.FULL_THROUGH_TURN + Patience.SPENT_BY_TURN) / 2)
            early shouldBeGreaterThan 0.0
            middle shouldBeGreaterThan 0.0
            middle shouldBeLessThan early
            onTurn(Patience.SPENT_BY_TURN) shouldBe 0.0
            onTurn(Patience.SPENT_BY_TURN + 10) shouldBe 0.0
        }

        test("the hold policy stays silent unless the profile asks for patience") {
            val game = scenario()
                .withPlayers()
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(1, "Murder")
                .withCardsInHand(1, "Craw Wurm", 3)
                .withCardOnBattlefield(2, "Mons's Goblin Raiders")
                .build()
            val cast = CastSpell(
                playerId = game.player1Id,
                cardId = game.findCardsInHand(1, "Murder").first(),
                targets = listOf(ChosenTarget.Permanent(game.findPermanent("Mons's Goblin Raiders")!!)),
            )

            // Off is the pre-change behaviour: instant removal in our own main phase is neutral,
            // deliberately, and stays so even with the target list in hand.
            HoldPolicy(intents)
                .verdictFor(game.state, game.player1Id, "Murder", cast) shouldBe TimingVerdict.Neutral

            HoldPolicy(intents, holdRemovalForBetterTargets = true)
                .verdictFor(game.state, game.player1Id, "Murder", cast)
                .shouldBeInstanceOf<TimingVerdict.Adjust>()
                .delta shouldBeLessThan 0.0
        }
    }
}
