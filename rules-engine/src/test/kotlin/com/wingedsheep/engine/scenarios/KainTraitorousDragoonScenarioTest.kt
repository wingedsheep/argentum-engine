package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Kain, Traitorous Dragoon (FIN #105) — {2}{B} 2/4 Legendary Creature.
 *
 * "Jump — During your turn, Kain has flying.
 *  Whenever Kain deals combat damage to a player, that player gains control of Kain. If they do, you
 *  draw that many cards, create that many tapped Treasure tokens, then lose that much life."
 *
 * The rider composes [com.wingedsheep.sdk.scripting.effects.GiveControlToTargetPlayerEffect] (donate
 * Kain to the damaged player) with an [com.wingedsheep.sdk.scripting.effects.IfYouDoEffect] gated on
 * [com.wingedsheep.sdk.scripting.effects.SuccessCriterion.ControlChanged]. "That many / that much" all
 * read the triggering combat-damage amount; "you" stays Kain's original controller.
 */
class KainTraitorousDragoonScenarioTest : ScenarioTestBase() {

    init {
        test("combat damage donates Kain to the defender; original controller draws, makes tapped Treasures, and loses life") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Kain, Traitorous Dragoon", summoningSickness = false)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handBefore = game.state.getHand(game.player1Id).size

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Kain, Traitorous Dragoon" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("P2 took 2 combat damage from Kain") {
                game.getLifeTotal(2) shouldBe 18
            }

            val kain = game.findPermanent("Kain, Traitorous Dragoon")
            withClue("Kain should still be on the battlefield") { (kain != null) shouldBe true }
            withClue("the damaged player (P2) now controls Kain (control change is a projected effect)") {
                game.state.projectedState.getController(kain!!) shouldBe game.player2Id
            }

            withClue("original controller (P1) drew 2 cards (equal to the damage dealt)") {
                game.state.getHand(game.player1Id).size shouldBe handBefore + 2
            }

            val treasures = game.findAllPermanents("Treasure")
            withClue("P1 created 2 Treasure tokens") { treasures.size shouldBe 2 }
            treasures.forEach { id ->
                withClue("each Treasure is controlled by the original controller (P1)") {
                    game.state.projectedState.getController(id) shouldBe game.player1Id
                }
                withClue("each Treasure entered tapped") {
                    (game.state.getEntity(id)?.get<TappedComponent>() != null) shouldBe true
                }
            }

            withClue("P1 lost 2 life (equal to the damage dealt)") {
                game.getLifeTotal(1) shouldBe 18
            }
        }

        test("no trigger when Kain is blocked and deals no combat damage to a player") {
            // Jump gives Kain flying on its controller's turn, so the blocker must also be able to
            // block flyers — Wind Drake (2/2 flying).
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Kain, Traitorous Dragoon", summoningSickness = false)
                .withCardOnBattlefield(2, "Wind Drake", summoningSickness = false)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handBefore = game.state.getHand(game.player1Id).size

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Kain, Traitorous Dragoon" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Wind Drake" to listOf("Kain, Traitorous Dragoon"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("P2 took no combat damage (Kain was blocked)") {
                game.getLifeTotal(2) shouldBe 20
            }
            val kain = game.findPermanent("Kain, Traitorous Dragoon")
            withClue("Kain stays under its original controller — the trigger never fired") {
                game.state.projectedState.getController(kain!!) shouldBe game.player1Id
            }
            withClue("no Treasures were created") { game.findAllPermanents("Treasure").size shouldBe 0 }
            withClue("P1 did not draw off the rider") {
                game.state.getHand(game.player1Id).size shouldBe handBefore
            }
            withClue("P1 lost no life off the rider") { game.getLifeTotal(1) shouldBe 20 }
        }
    }
}
