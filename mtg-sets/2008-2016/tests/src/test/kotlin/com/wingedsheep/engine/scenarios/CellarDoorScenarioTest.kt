package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Cellar Door (ISD #218) — {3}, {T}: target player puts the bottom card of their library into
 * their graveyard; if it's a creature card, you create a 2/2 black Zombie.
 */
class CellarDoorScenarioTest : ScenarioTestBase() {
    init {
        // Library cards are added top-down, so the last one added is the bottom card.
        fun doorGame(top: String, bottom: String) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Cellar Door")
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withCardInLibrary(2, top)
            .withCardInLibrary(2, bottom)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.openDoor(targetPlayerNumber: Int) {
            val door = findPermanent("Cellar Door")!!
            val abilityId = cardRegistry.getCard("Cellar Door")!!.activatedAbilities.single().id
            execute(
                ActivateAbility(
                    playerId = player1Id,
                    sourceId = door,
                    abilityId = abilityId,
                    targets = listOf(ChosenTarget.Player(if (targetPlayerNumber == 1) player1Id else player2Id)),
                )
            ).error shouldBe null
            resolveStack()
        }

        test("a creature card off the bottom gives you a Zombie, even from an opponent's library") {
            val game = doorGame(top = "Mountain", bottom = "Grizzly Bears")
            game.openDoor(2)

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.state.getLibrary(game.player2Id) shouldHaveSize 1
            game.isInGraveyard(2, "Mountain") shouldBe false
            game.findAllPermanents("Zombie Token") shouldHaveSize 1
            game.state.getEntity(game.findAllPermanents("Zombie Token").single())
                ?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
        }

        test("the bottom card is taken, not the top one; a noncreature makes no token") {
            val game = doorGame(top = "Grizzly Bears", bottom = "Mountain")
            game.openDoor(2)

            game.isInGraveyard(2, "Mountain") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe false
            game.findAllPermanents("Zombie Token") shouldHaveSize 0
        }
    }
}
