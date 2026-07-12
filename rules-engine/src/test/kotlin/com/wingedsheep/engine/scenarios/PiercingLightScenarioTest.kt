package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Piercing Light (VOW #30) — {W} Instant.
 *
 *   Piercing Light deals 2 damage to target attacking or blocking creature. Scry 1.
 *
 * The target restriction (`AttackingOrBlockingCreature`) means the creature has to be in combat, so
 * the test declares an attacker first, then casts the instant during the declare-attackers step. It
 * exercises the composite: 2 damage lands on the attacker, then a Scry 1 selection pauses.
 */
class PiercingLightScenarioTest : ScenarioTestBase() {

    init {
        context("Piercing Light — damage an attacking creature + scry") {

            test("deals 2 damage to a target attacking creature, then scrys 1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Piercing Light")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    // A 3/3 attacker so it survives 2 damage and we can read the marked damage.
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardInLibrary(1, "Plains") // scry 1 fodder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Move to combat and declare Hill Giant as an attacker so it is a legal target.
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Piercing Light", targetId = giant).error shouldBe null
                game.resolveStack()

                // The composite deals damage, then pauses for the Scry 1 selection.
                withClue("Scry 1 presents a selection over the single top card") {
                    val decision = game.getPendingDecision()
                    (decision is SelectCardsDecision) shouldBe true
                    (decision as SelectCardsDecision).options.size shouldBe 1
                }
                game.skipSelection() // keep the card on top

                withClue("Hill Giant (3/3) survives with 2 marked damage") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    val damage = game.state.getEntity(giant)?.get<DamageComponent>()?.amount ?: 0
                    damage shouldBe 2
                }
            }
        }
    }
}
