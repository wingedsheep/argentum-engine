package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class MenacingOgreScenarioTest : ScenarioTestBase() {
    init {
        for (originalControllerHighest in listOf(true, false)) {
            test("stolen Ogre grants counters only when the original trigger controller is highest: $originalControllerHighest") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Menacing Ogre")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(1, "Menacing Ogre").error shouldBe null
                // Resolve the creature spell, leaving its original controller's enters trigger.
                game.passPriority().error shouldBe null
                game.passPriority().error shouldBe null
                val ogre = game.findPermanent("Menacing Ogre")!!
                val stolen = services.effectExecutorRegistry.execute(
                    game.state, Effects.GainControl(EffectTarget.Self), EffectContext(ogre, game.player2Id)
                )
                stolen.error shouldBe null
                game.state = stolen.state
                game.state.projectedState.getController(ogre) shouldBe game.player2Id

                game.resolveStack()
                game.chooseNumber(if (originalControllerHighest) 3 else 1).error shouldBe null
                game.chooseNumber(if (originalControllerHighest) 1 else 3).error shouldBe null
                game.getPendingDecision() shouldBe null
                (game.state.getEntity(ogre)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe
                    if (originalControllerHighest) 2 else 0
                game.getLifeTotal(1) shouldBe if (originalControllerHighest) 17 else 20
                game.getLifeTotal(2) shouldBe if (originalControllerHighest) 20 else 17
            }
        }

        test("unrestricted secret numbers accept a bid above the former ninety-nine cap") {
            val game = scenario()
                .withPlayers()
                .withLifeTotal(1, 200)
                .withCardInHand(1, "Menacing Ogre")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Menacing Ogre").error shouldBe null
            game.resolveStack()
            val first = game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
            first.playerId shouldBe game.player1Id
            first.maxValue shouldBe Int.MAX_VALUE
            game.chooseNumber(100).error shouldBe null
            game.chooseNumber(99).error shouldBe null

            game.getPendingDecision() shouldBe null
            game.getLifeTotal(1) shouldBe 100
            game.getLifeTotal(2) shouldBe 20
            val ogre = game.findPermanent("Menacing Ogre")!!
            game.state.getEntity(ogre)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        }

        test("all-zero choices still give the controller the highest-number counters") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Menacing Ogre")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Menacing Ogre").error shouldBe null
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
            game.chooseNumber(0).error shouldBe null
            game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
            game.chooseNumber(0).error shouldBe null

            game.getPendingDecision() shouldBe null
            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 20
            val ogre = game.findPermanent("Menacing Ogre")!!
            game.state.getEntity(ogre)?.get<CountersComponent>()
                ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        }
    }
}
