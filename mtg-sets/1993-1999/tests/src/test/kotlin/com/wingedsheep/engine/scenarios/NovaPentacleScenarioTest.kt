package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Nova Pentacle: the opponent chooses the target creature at activation, the controller chooses
 * the damage source at resolution, and the next damage that source would deal to the controller
 * goes to that creature instead.
 */
class NovaPentacleScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario().withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Nova Pentacle")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Forest")
            .withCardOnBattlefield(1, "Forest")
            .withCardOnBattlefield(1, "Forest")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Mountain")
            .withCardOnBattlefield(2, "Mountain")
            .withCardInHand(2, "Lightning Bolt")
            .withCardInHand(2, "Lightning Bolt")
            .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
            .withActivePlayer(2).withPriorityPlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

        fun activate(game: TestGame) {
            game.execute(ActivateAbility(game.player1Id, game.findPermanent("Nova Pentacle")!!,
                cardRegistry.getCard("Nova Pentacle")!!.script.activatedAbilities.single().id)).error shouldBe null
        }

        fun chooseSource(game: TestGame, source: com.wingedsheep.sdk.model.EntityId) {
            when (val decision = game.getPendingDecision()) {
                is SelectCardsDecision -> {
                    decision.playerId shouldBe game.player1Id
                    game.selectCards(listOf(source)).error shouldBe null
                }
                is ChooseOptionDecision -> game.submitDecision(OptionChosenResponse(decision.id,
                    decision.options.indexOf("Lightning Bolt"))).error shouldBe null
                else -> error("Expected a damage source choice, got $decision")
            }
        }

        test("opponent picks your creature and the chosen spell's damage to you is dealt to it") {
            val game = board(); val bear = game.findPermanent("Grizzly Bears")!!
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            val bolt = game.state.stack.last()
            game.passPriority().error shouldBe null
            activate(game)
            val targeting = game.getPendingDecision()
            targeting.shouldBeInstanceOf<ChooseTargetsDecision>()
            targeting.playerId shouldBe game.player2Id
            game.selectTargets(listOf(bear)).error shouldBe null
            game.resolveStack()
            chooseSource(game, bolt)
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 20
            game.findPermanent("Grizzly Bears") shouldBe null

            // Only the next instance is redirected.
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 17
        }

        test("opponent may pick their own creature") {
            val game = board()
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            val bolt = game.state.stack.last()
            game.passPriority().error shouldBe null
            activate(game)
            game.selectTargets(listOf(game.findPermanent("Hill Giant")!!)).error shouldBe null
            game.resolveStack()
            chooseSource(game, bolt)
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 20
            game.findPermanent("Hill Giant") shouldBe null
            game.findPermanent("Grizzly Bears").shouldNotBeNull()
        }
    }
}
