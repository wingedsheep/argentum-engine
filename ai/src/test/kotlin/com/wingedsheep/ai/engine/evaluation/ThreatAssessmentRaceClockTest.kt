package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.math.abs

/**
 * `AiProfile.discountedRaceClock`: score the race in urgency, not in turns.
 *
 * Asserted on [ThreatAssessment] alone rather than through a whole evaluator, for
 * `CardAdvantageLandDropTest`'s reason — the claim is about this one feature, and a composite lets
 * one wrong number hide behind a right one. That the change moves the *decision* is
 * `PuzzleSuiteTest`'s `lastchance-05`.
 *
 * The `off` numbers are exact, not approximate, because the point is the *scale* of the old term
 * rather than its sign: −133.5 and +178 on a feature weighted 1.2, in an evaluator where a point of
 * life is worth 1.0 and the whole 2/2 driving them is worth 3.6 of board presence.
 */
class ThreatAssessmentRaceClockTest : ScenarioTestBase() {

    init {
        /** [ThreatAssessment] for seat 1, with the race discounted or in raw turns. */
        fun GameState.threat(on: Boolean): Double =
            ThreatAssessment.score(this, projectedState, turnOrder[0], discountedRaceClock = on)

        /** Both seats at 20 unless [myLife] says otherwise; [mine] / [theirs] are creature names. */
        fun board(mine: String? = null, theirs: String? = null, myLife: Int = 20): GameState {
            var builder = scenario().withPlayers().withLifeTotal(1, myLife)
            mine?.let { builder = builder.withCardOnBattlefield(1, it) }
            theirs?.let { builder = builder.withCardOnBattlefield(2, it) }
            return builder.build().state
        }

        // ── 1. The sentinel, and what it was worth ──

        test("off, one 2/2 across an empty board is worth −133.5 of race") {
            // 20 life / 2 power = a 10-turn clock, against the no-clock sentinel's 99: the term
            // charges (99 − 10) × 1.5 for a creature `BoardPresence` prices at 2.4.
            board(theirs = "Grizzly Bears").threat(on = false) shouldBe (-133.5 plusOrMinus EPSILON)
        }

        test("off, the same 2/2 on our side is worth +178 — the sentinel fires both ways") {
            board(mine = "Grizzly Bears").threat(on = false) shouldBe (178.0 plusOrMinus EPSILON)
        }

        // ── 2. Urgency: no sentinel, and a distant clock discounted ──

        test("on, no creatures is urgency zero — nothing to special-case") {
            board().threat(on = true) shouldBe (0.0 plusOrMinus EPSILON)
        }

        test("on, a lone 2/2 at 20 life is a tenth of a clock, either way round") {
            // 2 power against 20 life removes a tenth of a life total per turn. ×4 scale, and the
            // same 1.5 / 2.0 slopes as before — applied to urgency instead of to turns.
            withClue("theirs") {
                board(theirs = "Grizzly Bears").threat(on = true) shouldBe (-0.6 plusOrMinus EPSILON)
            }
            withClue("ours") {
                board(mine = "Grizzly Bears").threat(on = true) shouldBe (0.8 plusOrMinus EPSILON)
            }
        }

        test("on, the term is bounded — urgency caps at 'dead this turn'") {
            // The widest one side can open is 1.0 of urgency: +8 ahead, −6 behind. Against the old
            // term's ±190, and alongside the +8/−10 lethal bonuses this sits next to.
            withClue("a 6/4 against nothing, and the same at 3 life") {
                abs(board(mine = "Craw Wurm").threat(on = true)) shouldBeLessThan 8.1
                abs(board(mine = "Craw Wurm", myLife = 3).threat(on = true)) shouldBeLessThan 8.1
            }
        }

        // ── 3. The two things the turns form got backwards ──

        test("on, a clock five times closer is five times the penalty — the discount") {
            // The same 2/2, us at 20 and at 4: a 10-turn clock against a 2-turn one. That is the
            // whole argument for discounting, and the old term could not express it — it priced the
            // two nine percent apart, because 99 dwarfs the difference between 10 turns and 2.
            val far = board(theirs = "Grizzly Bears", myLife = 20)
            val near = board(theirs = "Grizzly Bears", myLife = 4)

            near.threat(on = true) shouldBe (5.0 * far.threat(on = true) plusOrMinus EPSILON)
            withClue("off, the same pair is −133.5 against −145.5 — 9% apart, not 5×") {
                far.threat(on = false) shouldBe (-133.5 plusOrMinus EPSILON)
                near.threat(on = false) shouldBe (-145.5 plusOrMinus EPSILON)
            }
        }

        test("on, the race is linear in power — a 3/3 clock is exactly 1.5 of a 2/2 clock") {
            // Each point of power adds the same damage, so it should add the same race value. In
            // turns it did not: the 3/3 came out 4% ahead of the 2/2, because both were being
            // measured against 99 rather than against each other.
            val bears = board(mine = "Grizzly Bears").threat(on = true)
            val giant = board(mine = "Hill Giant").threat(on = true)
            giant shouldBe (1.5 * bears plusOrMinus EPSILON)

            withClue("off: +178 against +184.67, a 4% difference for 50% more power") {
                board(mine = "Hill Giant").threat(on = false) shouldBe (184.6666 plusOrMinus 0.001)
            }
        }

        // ── 4. What the discount must not cost ──

        test("on, a clock that actually arrives is still priced as the emergency it is") {
            // Empty board at 6 life against a 6/4: dead next turn. Urgency 1.0 is the cap, so −6 of
            // race — and the −10 lethal-range penalty the sentinel used to drown out now dominates
            // it, which is the right way round.
            val dying = board(theirs = "Craw Wurm", myLife = 6)
            dying.threat(on = true) shouldBe (-16.0 plusOrMinus EPSILON)
            dying.threat(on = true) shouldBeLessThan board(theirs = "Grizzly Bears").threat(on = true)
        }

        test("on, the faster side of a real race still scores positive") {
            board(mine = "Craw Wurm", theirs = "Grizzly Bears").threat(on = true) shouldBe
                (1.6 plusOrMinus EPSILON) // (6/20 − 2/20) × 4 × 2.0
        }
    }

    private companion object {
        const val EPSILON = 1e-9
    }
}
