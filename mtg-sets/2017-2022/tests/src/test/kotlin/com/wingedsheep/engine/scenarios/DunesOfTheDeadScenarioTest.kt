package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.DunesOfTheDead
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Dunes of the Dead (HOU) — Land — Desert. Taps for {C}; when it's put into a graveyard from the
 * battlefield, its controller creates a 2/2 black Zombie.
 */
class DunesOfTheDeadScenarioTest : ScenarioTestBase() {
    init {
        test("taps for {C}") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Dunes of the Dead")
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val dunes = game.findPermanent("Dunes of the Dead")!!
            game.execute(
                ActivateAbility(game.player1Id, dunes, DunesOfTheDead.activatedAbilities.single().id)
            ).error shouldBe null
            game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.colorless shouldBe 1
        }

        test("destroyed by Stone Rain, it leaves a 2/2 Zombie token behind") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(2, "Dunes of the Dead")
                .withCardInHand(1, "Stone Rain")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpell(1, "Stone Rain", game.findPermanent("Dunes of the Dead")).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Dunes of the Dead") shouldBe true
            val zombies = game.findAllPermanents("Zombie Token")
            zombies.size shouldBe 1
            val zombie = zombies.single()
            game.state.projectedState.getPower(zombie) shouldBe 2
            game.state.projectedState.getToughness(zombie) shouldBe 2
            game.state.getBattlefield(game.player2Id).contains(zombie) shouldBe true
        }
    }
}
