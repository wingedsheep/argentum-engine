package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * [AiProfile.tapToPingAtTheirEndStep]: Lightning-Rig Crew's `{T}: This creature deals 1 damage to
 * each opponent` waits for the opponent's end step, where the tap costs no block.
 *
 * Production game `3d30821e` (Ixalan Sealed, turn 15): at 2 life, facing a 6/5 lifelinking Wanted
 * Scoundrels, the AI attacked with both Emissaries of Sunrise — keeping Lightning-Rig Crew home as
 * the chump blocker the attack plan counted on — then tapped the Crew to ping the opponent from 31
 * to 26. No untapped creature was left, and the Scoundrels' swing back killed it. The leaf saw one
 * point of damage; the board's threat term read the 6/5 as lethal through a 0/5 either way, so the
 * blocker was worth nothing to it.
 */
class SelfTapPingWindowTest : ScenarioTestBase() {

    private val liveWithoutFlag = AiProfile.LIVE.copy(id = "live-without-tapping", tapToPingAtTheirEndStep = false)

    /**
     * The turn-15 board: seat 1 at [myLife] with two Emissaries of Sunrise and the Crew, facing a
     * 6/5 lifelinker; seat 2 at [theirLife].
     */
    private fun crewBoard(activePlayer: Int, step: Step, myLife: Int = 2, theirLife: Int = 26): TestGame =
        board(activePlayer, myLife, theirLife).advanceToPriority(1, step)

    private fun board(activePlayer: Int, myLife: Int = 2, theirLife: Int = 26, scoundrelsTapped: Boolean = false): TestGame =
        scenario().withPlayers()
            .withRngSeed(20261105L)
            .withActivePlayer(activePlayer)
            .withLifeTotal(1, myLife)
            .withLifeTotal(2, theirLife)
            .withCardOnBattlefield(1, "Lightning-Rig Crew")
            .withCardOnBattlefield(1, "Emissary of Sunrise")
            .withCardOnBattlefield(1, "Emissary of Sunrise")
            .withLandsOnBattlefield(1, "Mountain", 4)
            .withLandsOnBattlefield(1, "Plains", 4)
            .withCardOnBattlefield(2, "Wanted Scoundrels", tapped = scoundrelsTapped)
            .withCardAttachedTo(2, "Mark of the Vampire", "Wanted Scoundrels")
            .withLandsOnBattlefield(2, "Swamp", 4)
            .withLandsOnBattlefield(2, "Plains", 3)
            .apply { repeat(8) { withCardInLibrary(1, "Mountain") } }
            .apply { repeat(8) { withCardInLibrary(2, "Swamp") } }
            .build()

    private fun TestGame.activatesCrew(profile: AiProfile): Boolean {
        val action: GameAction = AIPlayer.create(cardRegistry, player1Id, profile).chooseAction(state)
        return withClue("chose $action") {
            action is ActivateAbility &&
                state.getEntity(action.sourceId)?.get<CardComponent>()?.name == "Lightning-Rig Crew"
        }
    }

    init {
        test("after attacking with the Emissaries at 2 life: the Crew is the only block left, so it is held") {
            // The game's exact window: both Emissaries sent in, priority in declare attackers.
            // The Scoundrels attacked last turn, so they are still tapped on ours.
            val game = board(activePlayer = 1, theirLife = 31, scoundrelsTapped = true).advanceToDeclaration(1, Step.DECLARE_ATTACKERS)
            game.execute(
                DeclareAttackers(game.player1Id, game.findPermanents("Emissary of Sunrise").associateWith { game.player2Id }),
            )
            game.advanceToPriority(1, Step.DECLARE_ATTACKERS)

            game.activatesCrew(liveWithoutFlag) shouldBe true
            game.activatesCrew(AiProfile.LIVE) shouldBe false
        }

        test("our precombat main: held") {
            val game = crewBoard(activePlayer = 1, step = Step.PRECOMBAT_MAIN)

            game.activatesCrew(AiProfile.LIVE) shouldBe false
        }

        test("their precombat main: the tap would cost the block, so it is held") {
            val game = crewBoard(activePlayer = 2, step = Step.PRECOMBAT_MAIN)

            game.activatesCrew(AiProfile.LIVE) shouldBe false
        }

        test("their end step: the tap is free, and the AI pings") {
            val game = crewBoard(activePlayer = 2, step = Step.END)

            game.activatesCrew(AiProfile.LIVE) shouldBe true
        }

        test("a ping that kills is taken in any window") {
            val game = crewBoard(activePlayer = 1, step = Step.PRECOMBAT_MAIN, theirLife = 1)

            game.activatesCrew(AiProfile.LIVE) shouldBe true
        }
    }
}
