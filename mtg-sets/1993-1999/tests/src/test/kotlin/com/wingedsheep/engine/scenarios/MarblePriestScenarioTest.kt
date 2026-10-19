package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Marble Priest (LEG #286) — {5} Artifact Creature — Cleric 3/3.
 *
 * "All Walls able to block this creature do so.
 *  Prevent all combat damage that would be dealt to this creature by Walls."
 */
class MarblePriestScenarioTest : ScenarioTestBase() {

    init {
        context("Marble Priest") {

            fun builder() = scenario()
                .withPlayers("Priest Player", "Opponent")
                .withCardOnBattlefield(1, "Marble Priest")
                .withCardOnBattlefield(2, "Wall of Swords")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

            test("every Wall able to block it must, while other creatures stay free") {
                val game = builder().build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Marble Priest" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("no blocks is illegal while a Wall is able to block") {
                    game.declareNoBlockers().outcome shouldNotBe Outcome.Done
                }
                withClue("the non-Wall blocking alone leaves the Wall's requirement unobeyed") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Marble Priest"))).outcome shouldNotBe Outcome.Done
                }
                withClue("the Wall blocking alone is legal: Grizzly Bears isn't bound") {
                    game.declareBlockers(mapOf("Wall of Swords" to listOf("Marble Priest"))).outcome shouldBe Outcome.Done
                }
            }

            test("a Wall may not block another attacker instead") {
                val game = builder()
                    .withCardOnBattlefield(1, "Hill Giant")
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Marble Priest" to 2, "Hill Giant" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("Wall of Swords on the Hill Giant ignores the Priest's requirement") {
                    game.declareBlockers(mapOf("Wall of Swords" to listOf("Hill Giant"))).outcome shouldNotBe Outcome.Done
                }
                withClue("Wall on the Priest, Bears on the Giant is legal") {
                    game.declareBlockers(
                        mapOf("Wall of Swords" to listOf("Marble Priest"), "Grizzly Bears" to listOf("Hill Giant"))
                    ).outcome shouldBe Outcome.Done
                }
            }

            test("combat damage from Walls is prevented, damage from other blockers is not") {
                val game = builder().build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Marble Priest" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(
                    mapOf("Wall of Swords" to listOf("Marble Priest"), "Grizzly Bears" to listOf("Marble Priest"))
                ).outcome shouldBe Outcome.Done
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                val priest = game.findPermanent("Marble Priest")
                withClue("Wall of Swords' 3 damage is prevented; the Bears' 2 lands, so the 3/3 survives") {
                    (priest != null) shouldBe true
                    game.state.getEntity(priest!!)?.get<DamageComponent>()?.amount shouldBe 2
                }
            }
        }
    }
}
