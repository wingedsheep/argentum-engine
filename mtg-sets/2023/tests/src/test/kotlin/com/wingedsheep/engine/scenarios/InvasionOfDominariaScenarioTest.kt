package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Invasion of Dominaria // Serra Faithkeeper.
 *
 * Front: the Siege's enter trigger gains 4 life and draws a card. Back: a 4/4 flying, vigilance Angel.
 */
class InvasionOfDominariaScenarioTest : ScenarioTestBase() {

    init {
        test("front: entering gains 4 life and draws a card") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Dominaria")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Dominaria").error shouldBe null
            game.resolveStack()
            game.resolveStack()

            withClue("the Siege is on the battlefield") {
                game.findPermanent("Invasion of Dominaria") shouldNotBe null
            }
            withClue("you gained 4 life") { game.getLifeTotal(1) shouldBe 24 }
            withClue("you drew a card") {
                game.handSize(1) shouldBe 1
                game.librarySize(1) shouldBe 0
            }
        }

        test("back: defeating the Siege casts Serra Faithkeeper, a 4/4 flying vigilance Angel") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Invasion of Dominaria")
                .withCardOnBattlefield(1, "Craw Wurm", summoningSickness = false)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.checkStateBasedActions()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackersWithPermanentTargets(
                permanentAttackers = mapOf("Craw Wurm" to "Invasion of Dominaria")
            ).error shouldBe null
            var guard = 0
            while (game.state.pendingDecision == null && guard++ < 30) {
                if (game.state.step == Step.DECLARE_BLOCKERS &&
                    game.state.getEntity(game.player2Id)
                        ?.has<BlockersDeclaredThisCombatComponent>() != true
                ) {
                    game.declareNoBlockers()
                } else {
                    game.passPriority()
                }
            }
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            val angel = game.findPermanent("Serra Faithkeeper")
            angel shouldNotBe null
            val projected = game.state.projectedState
            projected.getPower(angel!!) shouldBe 4
            projected.getToughness(angel) shouldBe 4
            projected.hasKeyword(angel, Keyword.FLYING) shouldBe true
            projected.hasKeyword(angel, Keyword.VIGILANCE) shouldBe true
        }
    }
}
