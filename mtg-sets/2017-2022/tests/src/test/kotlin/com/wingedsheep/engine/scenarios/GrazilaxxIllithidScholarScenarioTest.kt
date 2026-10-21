package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Grazilaxx, Illithid Scholar (AFR #60): a creature you control that becomes blocked may be
 * returned to its owner's hand; one or more creatures you control dealing combat damage to a
 * player draws a card.
 */
class GrazilaxxIllithidScholarScenarioTest : ScenarioTestBase() {

    init {
        test("blocked attacker may bounce; the unblocked Grazilaxx connects and draws a card") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grazilaxx, Illithid Scholar", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Centaur Courser")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grazilaxx, Illithid Scholar" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Centaur Courser" to listOf("Grizzly Bears"))).error shouldBe null
            game.resolveStack()

            withClue("the blocked Bears' trigger asks whether to return it") {
                (game.getPendingDecision() is YesNoDecision) shouldBe true
            }
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Grizzly Bears") shouldBe true

            val handBefore = game.handSize(1)
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            withClue("one draw for the combat damage batch") {
                game.handSize(1) shouldBe handBefore + 1
            }
        }
    }
}
