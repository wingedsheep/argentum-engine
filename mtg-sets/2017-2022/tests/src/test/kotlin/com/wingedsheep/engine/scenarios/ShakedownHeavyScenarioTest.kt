package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Shakedown Heavy (SNC #95) — {2}{B} 6/4 menace.
 *
 *   Whenever this creature attacks, defending player may have you draw a card. If they do, untap
 *   this creature and remove it from combat.
 *
 * The yes/no belongs to the defending player, not the Heavy's controller. Accepting draws the
 * controller a card and pulls the Heavy out of combat untapped; declining leaves it attacking.
 */
class ShakedownHeavyScenarioTest : ScenarioTestBase() {

    init {
        context("Shakedown Heavy") {

            test("defending player accepts: controller draws, Heavy untaps and leaves combat") {
                val game = heavyGame()
                val heavy = game.findPermanent("Shakedown Heavy")!!
                val handBefore = game.handSize(1)

                game.declareAttackers(mapOf("Shakedown Heavy" to 2)).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<YesNoDecision>()
                withClue("the defending player answers the may") {
                    decision.playerId shouldBe game.player2Id
                }
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe handBefore + 1
                game.state.getEntity(heavy)!!.has<TappedComponent>() shouldBe false
                game.state.getEntity(heavy)!!.has<AttackingComponent>() shouldBe false

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.getLifeTotal(2) shouldBe 20
            }

            test("defending player declines: no draw, Heavy keeps attacking") {
                val game = heavyGame()
                val heavy = game.findPermanent("Shakedown Heavy")!!
                val handBefore = game.handSize(1)

                game.declareAttackers(mapOf("Shakedown Heavy" to 2)).error shouldBe null
                game.resolveStack()

                (game.getPendingDecision() as YesNoDecision).playerId shouldBe game.player2Id
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe handBefore
                game.state.getEntity(heavy)!!.has<TappedComponent>() shouldBe true
                game.state.getEntity(heavy)!!.has<AttackingComponent>() shouldBe true

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.getLifeTotal(2) shouldBe 14
            }
        }
    }

    private fun heavyGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Shakedown Heavy", summoningSickness = false)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .withTurnNumber(3)
        .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        .build()
}
