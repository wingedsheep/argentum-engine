package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Rules of Acquisition (TRK #78) — {3}{U}{U} sorcery, choose one:
 *  • Gain control of target nonland permanent an opponent controls. That opponent draws three cards.
 *  • Target opponent gains control of target nonland permanent you control. If they do, you draw
 *    three cards.
 *
 * Pins mode 1's "that opponent": the draw goes to the permanent's controller from before the steal,
 * not to whoever controls it once the spell has taken it.
 */
class RulesOfAcquisitionScenarioTest : ScenarioTestBase() {

    init {
        fun board(bearsOwner: Int) = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Rules of Acquisition")
            .withCardOnBattlefield(bearsOwner, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Island", 5)
            .apply { repeat(4) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("3rd Rule steals the permanent and the opponent who controlled it draws three") {
            val game = board(bearsOwner = 2)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpellWithMode(1, "Rules of Acquisition", modeIndex = 0, targetId = bears).error shouldBe null
            game.resolveStack()

            withClue("the caster gains control of the Bears") {
                game.state.projectedState.getController(bears) shouldBe game.player1Id
            }
            withClue("the former controller draws three") { game.handSize(2) shouldBe 3 }
            withClue("the caster draws nothing") { game.handSize(1) shouldBe 0 }
        }

        test("98th Rule gives the permanent away and the caster draws three") {
            val game = board(bearsOwner = 1)
            val bears = game.findPermanent("Grizzly Bears")!!
            val spell = game.state.getHand(game.player1Id).first {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Rules of Acquisition"
            }
            val targets = listOf(ChosenTarget.Player(game.player2Id), ChosenTarget.Permanent(bears))

            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = spell,
                    targets = targets,
                    chosenModes = listOf(1),
                    modeTargetsOrdered = listOf(targets),
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("the opponent gains control of the Bears") {
                game.state.projectedState.getController(bears) shouldBe game.player2Id
            }
            withClue("the caster draws three") { game.handSize(1) shouldBe 3 }
            withClue("the opponent draws nothing") { game.handSize(2) shouldBe 0 }
        }
    }
}
