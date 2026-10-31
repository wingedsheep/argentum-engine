package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.drk.cards.MazeOfIth
import com.wingedsheep.mtg.sets.definitions.thb.cards.LabyrinthOfSkophos
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Skophos Maze-Warden (THB) — "Whenever another creature becomes the target of an ability of a land
 * you control named Labyrinth of Skophos, you may have this creature fight that creature."
 *
 * The trigger's source filter reads the permanent whose ability targeted, not the ability on the
 * stack: your own Labyrinth wakes it, an opponent's Labyrinth or another land's ability doesn't.
 */
class SkophosMazeWardenScenarioTest : ScenarioTestBase() {

    private val labyrinthRemove = LabyrinthOfSkophos.activatedAbilities[1].id
    private val mazeUntap = MazeOfIth.activatedAbilities[0].id

    /** Player 1 attacks with Hill Giant; Player 2 blocks with Grizzly Bears. */
    private fun blockedGame(labyrinthOwner: Int = 1, extraLand: String? = null): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Skophos Maze-Warden")
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(labyrinthOwner, "Labyrinth of Skophos")
            .withLandsOnBattlefield(labyrinthOwner, "Mountain", 4)
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        if (extraLand != null) builder.withCardOnBattlefield(1, extraLand)
        val game = builder.build()

        game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
        return game
    }

    /** Pass until [playerNumber] holds priority in the declare blockers step. */
    private fun TestGame.priorityTo(playerNumber: Int) {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        var guard = 0
        while (state.priorityPlayerId != playerId && guard++ < 2) passPriority().error shouldBe null
        state.priorityPlayerId shouldBe playerId
        state.step shouldBe Step.DECLARE_BLOCKERS
    }

    private fun TestGame.damageOn(name: String): Int =
        state.getEntity(findPermanent(name)!!)?.get<DamageComponent>()?.amount ?: 0

    init {
        test("your Labyrinth targeting another creature lets the Warden fight it") {
            val game = blockedGame()
            val labyrinth = game.findPermanent("Labyrinth of Skophos")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.priorityTo(1)
            game.execute(
                ActivateAbility(game.player1Id, labyrinth, labyrinthRemove, targets = listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()
            withClue("the fight is optional") { game.hasPendingDecision() shouldBe true }
            game.answerYesNo(true)
            game.resolveStack()

            withClue("the Warden dealt 3 to the Bears, which died") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
            withClue("the Bears dealt 2 back") { game.damageOn("Skophos Maze-Warden") shouldBe 2 }
        }

        test("declining the fight deals no damage") {
            val game = blockedGame()
            val labyrinth = game.findPermanent("Labyrinth of Skophos")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.priorityTo(1)
            game.execute(
                ActivateAbility(game.player1Id, labyrinth, labyrinthRemove, targets = listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()
            game.answerYesNo(false)
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.damageOn("Skophos Maze-Warden") shouldBe 0
        }

        test("an opponent's Labyrinth of Skophos doesn't trigger it") {
            val game = blockedGame(labyrinthOwner = 2)
            val labyrinth = game.findPermanent("Labyrinth of Skophos")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.priorityTo(2)
            game.execute(
                ActivateAbility(game.player2Id, labyrinth, labyrinthRemove, targets = listOf(ChosenTarget.Permanent(giant)))
            ).error shouldBe null
            withClue("only the Labyrinth's own ability is on the stack") { game.state.stack.size shouldBe 1 }
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.damageOn("Skophos Maze-Warden") shouldBe 0
        }

        test("another land's targeting ability doesn't trigger it") {
            val game = blockedGame(extraLand = "Maze of Ith")
            val maze = game.findPermanent("Maze of Ith")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.priorityTo(1)
            game.execute(
                ActivateAbility(game.player1Id, maze, mazeUntap, targets = listOf(ChosenTarget.Permanent(giant)))
            ).error shouldBe null
            withClue("only Maze of Ith's ability is on the stack") { game.state.stack.size shouldBe 1 }
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.damageOn("Skophos Maze-Warden") shouldBe 0
        }
    }
}
