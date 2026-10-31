package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** Proud Wildbonder: creatures you control with trample may assign combat damage as though unblocked. */
class ProudWildbonderScenarioTest : ScenarioTestBase() {
    init {
        test("a blocked trampler may assign all its damage to the player; a creature without trample may not") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Proud Wildbonder")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Wall of Granite")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Proud Wildbonder" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(
                mapOf("Wall of Granite" to listOf("Proud Wildbonder"), "Craw Wurm" to listOf("Grizzly Bears"))
            ).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

            withClue("only the trampling Wildbonder is asked about") {
                val d = game.getPendingDecision()
                d.shouldBeInstanceOf<YesNoDecision>()
                d.context.sourceName shouldBe "Proud Wildbonder"
            }
            game.answerYesNo(true).error shouldBe null
            withClue("no question for Grizzly Bears, which lacks trample") {
                (game.getPendingDecision() is YesNoDecision) shouldBe false
            }
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("all 4 of Wildbonder's damage skips the 0/7 Wall and hits the opponent") {
                game.getLifeTotal(2) shouldBe 16
            }
        }
    }
}
