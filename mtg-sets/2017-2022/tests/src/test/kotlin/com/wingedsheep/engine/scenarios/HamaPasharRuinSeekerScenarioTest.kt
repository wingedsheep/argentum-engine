package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.dungeon.DungeonService
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dungeons.Dungeons
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hama Pashar, Ruin Seeker (AFR #224): "Room abilities of dungeons you own trigger an additional
 * time." The room you ventured into triggers twice, the marker moves only once (ruling), and a
 * doubled last room still completes the dungeon once both copies have left the stack. The venture
 * comes from Dungeon Map's sorcery-speed ability.
 */
class HamaPasharRuinSeekerScenarioTest : ScenarioTestBase() {

    private fun ventureWithMap(game: TestGame) {
        val map = game.findPermanent("Dungeon Map")!!
        val venture = cardRegistry.getCard("Dungeon Map")!!.activatedAbilities[1].id
        game.execute(ActivateAbility(game.player1Id, map, venture)).error shouldBe null
        game.resolveStack()
    }

    private fun chooseDungeon(game: TestGame, name: String) {
        val decision = game.getPendingDecision() as ChooseOptionDecision
        game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(name))).error shouldBe null
        game.resolveStack()
    }

    private fun board(hamaController: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(hamaController, "Hama Pashar, Ruin Seeker")
        .withCardOnBattlefield(1, "Dungeon Map")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("the room you venture into triggers an additional time; the marker moves once") {
            val game = board(hamaController = 1)

            ventureWithMap(game)
            chooseDungeon(game, "Dungeon of the Mad Mage")

            withClue("Yawning Portal (gain 1 life) resolved twice") { game.getLifeTotal(1) shouldBe 22 }
            DungeonService.marker(game.state, game.player1Id)!!.room shouldBe 0
        }

        test("a doubled last room completes the dungeon once, after both copies resolve") {
            val game = board(hamaController = 1)
            // The venture marker is on Fungi Cavern, one arrow from Lost Mine's last room.
            var s = DungeonService.enter(game.state, game.player1Id, Dungeons.LostMineOfPhandelver, "setup").first
            s = DungeonService.moveTo(s, game.player1Id, 5, "setup").first
            game.state = s
            val handBefore = game.handSize(1)

            ventureWithMap(game)

            withClue("Temple of Dumathoin (draw a card) resolved twice") { game.handSize(1) shouldBe handBefore + 2 }
            DungeonService.completedCount(game.state, game.player1Id) shouldBe 1
            DungeonService.marker(game.state, game.player1Id) shouldBe null
        }

        test("an opponent's Hama Pashar doesn't double your room abilities") {
            val game = board(hamaController = 2)

            ventureWithMap(game)
            chooseDungeon(game, "Dungeon of the Mad Mage")

            game.getLifeTotal(1) shouldBe 21
        }
    }
}
