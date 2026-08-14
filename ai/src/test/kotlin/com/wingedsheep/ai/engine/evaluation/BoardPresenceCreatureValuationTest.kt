package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * `AiProfile.creatureValuation`: the two things [BoardPresence] was pricing wrong about a creature.
 *
 * Asserted on `permanentValue` for **one permanent at a time**, rather than through a whole
 * evaluator or even a whole board, for `CardAdvantageLandDropTest`'s reason and one more. The claim
 * is about this one number, and a composite would let `ThreatAssessment` cover a wrong value here
 * with a right one there — which is precisely how both of these survived as long as they did, masked
 * by the race clock's `99.0` no-attacker sentinel until
 * `AiProfile.discountedRaceClock` removed it. The extra reason is the Aura: a board-level score of a
 * Pacifism'd creature also contains Pacifism's own value as a permanent, and that is not the term
 * under test.
 *
 * That the corrections move the *decision* is `PuzzleSuiteTest`'s `activate-04` and `removal-03`.
 */
class BoardPresenceCreatureValuationTest : ScenarioTestBase() {

    init {
        val intents = IntentCatalog.of(cardRegistry)
        val legacy = CreatureValuation.LEGACY
        val damageFades = CreatureValuation(markedDamageFadesAtCleanup = true)
        val cantAttackCosts = CreatureValuation(cantAttackCostsPower = true)

        /** What [BoardPresence] pays for the battlefield permanent named [name], under [valuation]. */
        fun GameState.valueOf(name: String, valuation: CreatureValuation): Double {
            val id = getBattlefield().first { getEntity(it)?.get<CardComponent>()?.name == name }
            val card = getEntity(id)!!.get<CardComponent>()!!
            return BoardPresence.permanentValue(this, projectedState, id, card, intents, valuation)
        }

        /** Mark [amount] damage on the battlefield permanent named [name] — no combat required. */
        fun GameState.damage(name: String, amount: Int): GameState {
            val id = getBattlefield().first { getEntity(it)?.get<CardComponent>()?.name == name }
            return updateEntity(id) { it.with(DamageComponent(amount)) }
        }

        fun creature(name: String): GameState =
            scenario().withPlayers().withCardOnBattlefield(2, name).build().state

        fun pacified(name: String): GameState =
            scenario().withPlayers()
                .withCardOnBattlefield(2, name)
                .withCardAttachedTo(2, "Pacifism", name)
                .build().state

        // ── 1. Marked damage is not board progress ──

        test("off, one damage on a 3/3 reads as a sixth of a creature killed") {
            // The `activate-04` mistake, priced. Hill Giant is 3*1.0 + 3*0.4 = 4.2, and the old
            // `0.5 + 0.5 * healthFraction` takes it to 3.5 for a ping that kills nothing.
            val board = creature("Hill Giant")

            board.valueOf("Hill Giant", legacy) shouldBe (4.2 plusOrMinus EPSILON)
            board.damage("Hill Giant", 1).valueOf("Hill Giant", legacy) shouldBe (3.5 plusOrMinus EPSILON)
        }

        test("on, damage that wears off at cleanup changes nothing") {
            val board = creature("Hill Giant")

            withClue("one damage") {
                board.damage("Hill Giant", 1).valueOf("Hill Giant", damageFades) shouldBe
                    (4.2 plusOrMinus EPSILON)
            }
            withClue("two — and still not the undamaged value by coincidence of one point") {
                board.damage("Hill Giant", 2).valueOf("Hill Giant", damageFades) shouldBe
                    (4.2 plusOrMinus EPSILON)
            }
        }

        // ── 2. "Can't attack" costs the power, not a slice of everything ──

        test("off, a Pacifism'd 6/4 still outranks an untouched 3/3") {
            // The `removal-03` mistake, priced. Craw Wurm is 6*1.0 + 4*0.4 = 7.6; can't-block and
            // can't-attack take 0.85 each, leaving 5.49 — above Hill Giant's 4.2, so the Murder
            // points at the creature Pacifism has already answered.
            pacified("Craw Wurm").valueOf("Craw Wurm", legacy) shouldBe (5.491 plusOrMinus 0.001)
            creature("Hill Giant").valueOf("Hill Giant", legacy) shouldBe (4.2 plusOrMinus EPSILON)
        }

        test("on, the pacified 6/4 drops below the 3/3 that can still attack") {
            // 7.6 - 6*0.8 = 2.8 for the lost power, then 0.85 for can't-block: 2.38.
            pacified("Craw Wurm").valueOf("Craw Wurm", cantAttackCosts) shouldBe (2.38 plusOrMinus 0.001)

            pacified("Craw Wurm").valueOf("Craw Wurm", cantAttackCosts) shouldBeLessThan
                creature("Hill Giant").valueOf("Hill Giant", cantAttackCosts)
        }

        test("on, the two spellings of can't-attack agree on the same body") {
            // A 2/3 that cannot attack is a 2/3 that cannot attack, whether the restriction is the
            // word DEFENDER printed on the card or an Aura hung on it afterwards.
            val printed = creature("Wall of Spears") // 2/3 defender, first strike
            val hung = pacified("Wall of Spears")

            withClue("DEFENDER already paid power*0.8; the Aura now pays the same and no more") {
                // The only remaining gap is can't-*block*, which Pacifism grants and DEFENDER does
                // not — so the enchanted copy is worth exactly 0.85 of the printed one, rather than
                // 0.85 of a body that never lost its power in the first place.
                hung.valueOf("Wall of Spears", cantAttackCosts) shouldBe
                    (printed.valueOf("Wall of Spears", cantAttackCosts) * 0.85 plusOrMinus 0.001)
            }
            withClue("and a card carrying both restrictions is not charged the power twice") {
                printed.valueOf("Wall of Spears", cantAttackCosts) shouldBe
                    (printed.valueOf("Wall of Spears", legacy) plusOrMinus EPSILON)
            }
        }

        test("on, a creature that can attack is untouched") {
            // The negative control the whole change rests on: this must not become a general
            // discount on creatures.
            listOf("Craw Wurm", "Grizzly Bears", "Hill Giant", "Serra Angel").forEach { name ->
                withClue(name) {
                    creature(name).valueOf(name, cantAttackCosts) shouldBe
                        (creature(name).valueOf(name, legacy) plusOrMinus EPSILON)
                }
            }
        }

        test("on, a 6/4 is still worth more than a 2/2 — pacified or not") {
            // `removal-01` in feature form: the correction must not invert raw creature value.
            withClue("both pacified, the body still counts") {
                pacified("Grizzly Bears").valueOf("Grizzly Bears", cantAttackCosts) shouldBeLessThan
                    pacified("Craw Wurm").valueOf("Craw Wurm", cantAttackCosts)
            }
        }
    }

    companion object {
        /** Every constant here is a sum of tenths; the tolerance is about binary floats, not slack. */
        private const val EPSILON = 1e-9
    }
}
