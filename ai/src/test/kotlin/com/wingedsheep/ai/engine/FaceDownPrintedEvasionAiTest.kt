package com.wingedsheep.ai.engine

import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * The AI's block math reads an attacker's printed `CantBeBlockedBy` the way the engine does: off
 * while the attacker is face down. Silumgar Assassin's "creatures with power greater than this
 * creature's power can't block it" keeps a 3/3 off it face up, but a face-down 2/2 has no
 * abilities — and reading its printed text anyway would let the AI see through the morph.
 */
class FaceDownPrintedEvasionAiTest : ScenarioTestBase() {

    init {
        test("a bigger creature can't block Silumgar Assassin face up, but can block it face down") {
            val game = scenario()
                .withPlayers("Attacker", "AI")
                .withCardOnBattlefield(1, "Silumgar Assassin")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val assassin = game.findPermanent("Silumgar Assassin")!!
            val giant = game.findPermanent("Hill Giant")!!

            CombatMath.canBeBlockedBy(game.state, game.state.projectedState, assassin, giant, cardRegistry) shouldBe false

            val faceDown = game.state.updateEntity(assassin) { it.with(FaceDownComponent) }
            withClue("a face-down 2/2 has no printed evasion to read") {
                CombatMath.canBeBlockedBy(faceDown, faceDown.projectedState, assassin, giant, cardRegistry) shouldBe true
            }
        }

        test("a face-down blocker is no longer held back by its printed restriction") {
            val game = scenario()
                .withPlayers("Attacker", "AI")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Spitfire Handler")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            val handler = game.findPermanent("Spitfire Handler")!!

            CombatMath.canBeBlockedBy(game.state, game.state.projectedState, giant, handler, cardRegistry) shouldBe false

            val faceDown = game.state.updateEntity(handler) { it.with(FaceDownComponent) }
            withClue("a face-down blocker has no printed restriction") {
                CombatMath.canBeBlockedBy(faceDown, faceDown.projectedState, giant, handler, cardRegistry) shouldBe true
            }
        }
    }
}
