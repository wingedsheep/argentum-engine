package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Armored Transport (GTC #226) — {3} Artifact Creature — Construct 2/1.
 *
 * "Prevent all combat damage that would be dealt to this creature by creatures blocking it."
 *
 * The prevention reads live combat state (`StatePredicate.IsBlockingSource`): its blockers can't
 * hurt it, but a creature it blocks still can.
 */
class ArmoredTransportScenarioTest : ScenarioTestBase() {

    init {
        context("Armored Transport") {

            test("combat damage from the creature blocking it is prevented") {
                val game = scenario()
                    .withPlayers("Transport Player", "Defender")
                    .withCardOnBattlefield(1, "Armored Transport")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Armored Transport" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Armored Transport"))).outcome shouldBe Outcome.Done
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                val transport = game.findPermanent("Armored Transport")
                withClue("Hill Giant's 3 combat damage to the 2/1 Transport was prevented") {
                    transport.shouldNotBeNull()
                    (game.state.getEntity(transport)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
                }
                val giant = game.findPermanent("Hill Giant")
                withClue("the Transport's own combat damage still lands on its blocker") {
                    giant.shouldNotBeNull()
                    (game.state.getEntity(giant)?.get<DamageComponent>()?.amount ?: 0) shouldBe 2
                }
            }

            test("a creature it blocks still deals combat damage to it") {
                val game = scenario()
                    .withPlayers("Transport Player", "Attacker")
                    .withCardOnBattlefield(1, "Armored Transport")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Armored Transport" to listOf("Hill Giant"))).outcome shouldBe Outcome.Done
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Hill Giant isn't blocking the Transport, so its 3 damage kills the 2/1") {
                    game.isOnBattlefield("Armored Transport") shouldBe false
                }
            }
        }
    }
}
