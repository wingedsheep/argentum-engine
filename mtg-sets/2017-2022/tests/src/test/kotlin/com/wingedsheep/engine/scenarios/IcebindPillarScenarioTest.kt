package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class IcebindPillarScenarioTest : ScenarioTestBase() {
    private val abilityId = cardRegistry.getCard("Icebind Pillar")!!.activatedAbilities.first().id

    private fun TestGame.activatePillar(targetName: String) = execute(
        ActivateAbility(
            playerId = player1Id,
            sourceId = findPermanent("Icebind Pillar")!!,
            abilityId = abilityId,
            targets = listOf(ChosenTarget.Permanent(findPermanent(targetName)!!))
        )
    )

    init {
        for (targetName in listOf("Grizzly Bears", "Icy Manipulator")) {
            test("snow mana activates Pillar to tap $targetName") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Icebind Pillar")
                    .withLandsOnBattlefield(1, "Snow-Covered Mountain", 1)
                    .withCardOnBattlefield(2, targetName)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activatePillar(targetName).error shouldBe null
                game.state.getEntity(game.findPermanent("Icebind Pillar")!!)!!
                    .has<TappedComponent>() shouldBe true
                game.state.getEntity(game.findPermanent("Snow-Covered Mountain")!!)!!
                    .has<TappedComponent>() shouldBe true
                game.resolveStack()
                game.state.getEntity(game.findPermanent(targetName)!!)!!
                    .has<TappedComponent>() shouldBe true
            }
        }

        test("ordinary mana cannot pay the snow activation cost") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Icebind Pillar")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            (game.activatePillar("Grizzly Bears").error != null) shouldBe true
            game.state.getEntity(game.findPermanent("Icebind Pillar")!!)!!
                .has<TappedComponent>() shouldBe false
            game.state.getEntity(game.findPermanent("Grizzly Bears")!!)!!
                .has<TappedComponent>() shouldBe false
        }

        test("a land that is neither artifact nor creature is not a legal target") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Icebind Pillar")
                .withLandsOnBattlefield(1, "Snow-Covered Island", 1)
                .withLandsOnBattlefield(2, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            (game.activatePillar("Forest").error != null) shouldBe true
            game.state.getEntity(game.findPermanent("Icebind Pillar")!!)!!
                .has<TappedComponent>() shouldBe false
        }
    }
}
