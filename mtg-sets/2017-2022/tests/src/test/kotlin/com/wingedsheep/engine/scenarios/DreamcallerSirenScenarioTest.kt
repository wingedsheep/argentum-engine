package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class DreamcallerSirenScenarioTest : ScenarioTestBase() {
    init {
        for (targetCount in 0..2) {
            test("another Pirate enables choosing $targetCount nonland targets") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dreamcaller Siren")
                    .withCardOnBattlefield(1, "Siren Stormtamer")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Sol Ring")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val targets = listOf(game.findPermanent("Grizzly Bears")!!, game.findPermanent("Sol Ring")!!)
                game.castSpell(1, "Dreamcaller Siren").error shouldBe null
                game.resolveStack()
                game.selectTargets(targets.take(targetCount)).error shouldBe null
                game.resolveStack()
                targets.forEachIndexed { index, id ->
                    game.state.getEntity(id)?.has<TappedComponent>() shouldBe (index < targetCount)
                }
                game.state.stack.isEmpty() shouldBe true
                game.state.pendingDecision shouldBe null
            }
        }

        test("the Siren itself does not satisfy another Pirate") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Dreamcaller Siren")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Dreamcaller Siren").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Dreamcaller Siren") shouldBe true
            game.state.pendingDecision shouldBe null
            game.state.stack.isEmpty() shouldBe true
            game.state.getEntity(game.findPermanent("Grizzly Bears")!!)?.has<TappedComponent>() shouldBe false
        }

        test("losing the other Pirate in response prevents tapping the targets") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Dreamcaller Siren")
                .withCardInHand(1, "Unsummon")
                .withCardOnBattlefield(1, "Siren Stormtamer")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val pirate = game.findPermanent("Siren Stormtamer")!!
            val target = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Dreamcaller Siren").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(target)).error shouldBe null
            game.castSpell(1, "Unsummon", pirate).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Siren Stormtamer") shouldBe false
            game.state.getEntity(target)?.has<TappedComponent>() shouldBe false
            game.state.stack.isEmpty() shouldBe true
        }
    }
}
