package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Akroan Horse (THS #210): on entering, an opponent gains control of it; afterwards "your upkeep"
 * and "each opponent" follow its *current* controller, so it triggers on the opponent's upkeep and
 * the original caster gets the Soldier tokens — and never on the caster's own upkeep.
 */
class AkroanHorseScenarioTest : ScenarioTestBase() {

    private fun soldiersControlledBy(game: TestGame, player: EntityId): Int =
        game.findPermanents("Soldier Token").count { game.state.projectedState.getController(it) == player }

    init {
        test("the opponent gains control on entry, and its upkeep gives the caster a Soldier") {
            var builder = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Akroan Horse")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(5) { builder = builder.withCardInLibrary(1, "Plains") }
            repeat(5) { builder = builder.withCardInLibrary(2, "Plains") }
            val game = builder.build()

            game.castSpell(1, "Akroan Horse").error shouldBe null
            game.resolveStack()

            val horse = game.findPermanent("Akroan Horse")!!
            game.state.projectedState.getController(horse) shouldBe game.player2Id

            // Opponent's upkeep: the Horse's controller is the opponent, so the caster gets a Soldier.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()

            game.state.activePlayerId shouldBe game.player2Id
            soldiersControlledBy(game, game.player1Id) shouldBe 1
            soldiersControlledBy(game, game.player2Id) shouldBe 0

            // Caster's next upkeep: not the Horse controller's upkeep, so nothing new.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()

            game.state.activePlayerId shouldBe game.player1Id
            soldiersControlledBy(game, game.player1Id) shouldBe 1
            soldiersControlledBy(game, game.player2Id) shouldBe 0
        }
    }
}
