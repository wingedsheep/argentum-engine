package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Galvanic Discharge (MH3) — "Choose target creature or planeswalker. You get {E}{E}{E} (three
 * energy counters), then you may pay any amount of {E}. Galvanic Discharge deals that much damage
 * to that permanent."
 *
 * Also the proving ground for the new player-scoped counter primitives (CR 107.14, 122.1):
 * `Effects.GetEnergy`, `Effects.PayCounters`, and reading the paid amount back via
 * `DynamicAmount.VariableReference` into `Effects.DealDamage`.
 *
 * Rulings pinned:
 *  - "You may pay zero {E}. You will get {E}{E}{E}, but Galvanic Discharge won't deal any damage."
 *    (2024-06-07)
 *  - "If a spell ... states that you 'may pay' some amount of {E}, and ... targets ha[ve] become
 *    an illegal target, the spell ... won't resolve. You can't pay any {E} even if you want to."
 *    (2024-06-07) — proven via Personify blinking the target in response (same fizzle mechanism
 *    as [BlinkedTargetFizzleScenarioTest]).
 */
class GalvanicDischargeScenarioTest : ScenarioTestBase() {

    private fun energy(game: TestGame, playerId: EntityId): Int =
        game.state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    private fun damageOn(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    init {
        context("Galvanic Discharge — get {E}{E}{E}, then pay any amount") {

            test("paying the full amount deals that much damage and empties the energy pool") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Galvanic Discharge")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Craw Wurm") // 6/4
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm")!!
                game.castSpell(1, "Galvanic Discharge", targetId = wurm).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("a single ChooseNumber prompt, capped at the three energy just gained") {
                    (decision is ChooseNumberDecision) shouldBe true
                    (decision as ChooseNumberDecision).minValue shouldBe 0
                    decision.maxValue shouldBe 3
                }
                game.chooseNumber(3).error shouldBe null

                withClue("3 damage marked on the Wurm, and all three energy counters spent") {
                    damageOn(game, wurm) shouldBe 3
                    energy(game, game.player1Id) shouldBe 0
                }
            }

            test("paying zero deals no damage, but the energy from getting {E}{E}{E} is kept") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Galvanic Discharge")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm")!!
                game.castSpell(1, "Galvanic Discharge", targetId = wurm).error shouldBe null
                game.resolveStack()
                game.chooseNumber(0).error shouldBe null

                withClue("no damage dealt, but the three energy counters from 'you get' are still there") {
                    damageOn(game, wurm) shouldBe 0
                    energy(game, game.player1Id) shouldBe 3
                }
            }

            test("paying a partial amount deals that much damage and leaves the rest of the energy") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Galvanic Discharge")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm")!!
                game.castSpell(1, "Galvanic Discharge", targetId = wurm).error shouldBe null
                game.resolveStack()
                game.chooseNumber(1).error shouldBe null

                withClue("1 damage marked, 2 energy counters remain") {
                    damageOn(game, wurm) shouldBe 1
                    energy(game, game.player1Id) shouldBe 2
                }
            }

            test("the spell fizzles entirely when its target is blinked in response — no energy gained") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Galvanic Discharge")
                    .withCardInHand(1, "Personify")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm")!!
                game.castSpell(1, "Galvanic Discharge", targetId = wurm).error shouldBe null
                // Respond to my own spell by blinking its target — CR 400.7, the returned Wurm is a
                // new object, so Galvanic Discharge no longer has a legal target (CR 608.2b).
                game.castSpell(1, "Personify", wurm).error shouldBe null

                game.resolveStack()

                val returned = game.findPermanent("Craw Wurm")
                    ?: error("Craw Wurm should have returned to the battlefield")
                withClue("Galvanic Discharge never resolved: no damage, and no energy counters gained at all") {
                    damageOn(game, returned) shouldBe 0
                    energy(game, game.player1Id) shouldBe 0
                }
            }
        }
    }
}
