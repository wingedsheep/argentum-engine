package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class TheWiseMothmanScenarioTest : ScenarioTestBase() {
    private val youMillThree = card("Mothman You Mill Three") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.Pipeline { mill(3) } }
    }
    private val eachMillsTwo = card("Mothman Each Mills Two") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.Pipeline { mill(2, Player.Each) } }
    }

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.rad(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.RAD) ?: 0

    private fun board(p1Library: List<String>, p2Library: List<String> = listOf("Forest", "Forest")): TestGame {
        val b = scenario().withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "The Wise Mothman")
            .withCardOnBattlefield(1, "Savannah Lions")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInHand(1, youMillThree.name)
            .withCardInHand(1, eachMillsTwo.name)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        p1Library.forEach { b.withCardInLibrary(1, it) }
        p2Library.forEach { b.withCardInLibrary(2, it) }
        return b.build()
    }

    /** Resolves the mill spell and returns the Mothman's target decision. */
    private fun TestGame.millAndAwaitTargets(spell: String): ChooseTargetsDecision {
        castSpell(1, spell).error shouldBe null
        resolveStack()
        return state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
    }

    init {
        cardRegistry.register(youMillThree)
        cardRegistry.register(eachMillsTwo)

        test("X is the nonland cards milled this way, fixed when the targets are chosen") {
            val game = board(listOf("Grizzly Bears", "Lightning Bolt", "Forest"))
            val decision = game.millAndAwaitTargets(youMillThree.name)
            decision.targetRequirements.single().maxTargets shouldBe 2
            decision.targetRequirements.single().minTargets shouldBe 0

            val mothman = game.findPermanent("The Wise Mothman").shouldNotBeNull()
            val lions = game.findPermanent("Savannah Lions").shouldNotBeNull()
            val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
            game.selectTargets(listOf(mothman, lions, giant)).error shouldNotBe null

            game.selectTargets(listOf(mothman, giant)).error shouldBe null
            game.resolveStack()
            game.plusOnes(mothman) shouldBe 1
            game.plusOnes(giant) shouldBe 1
            game.plusOnes(lions) shouldBe 0
        }

        test("a mill by several players at once triggers once, counting every nonland card") {
            val game = board(listOf("Grizzly Bears", "Forest"), listOf("Lightning Bolt", "Forest"))
            val decision = game.millAndAwaitTargets(eachMillsTwo.name)
            decision.targetRequirements.single().maxTargets shouldBe 2

            val lions = game.findPermanent("Savannah Lions").shouldNotBeNull()
            game.selectTargets(listOf(lions)).error shouldBe null
            game.resolveStack()
            game.plusOnes(lions) shouldBe 1
            game.state.pendingDecision shouldBe null
        }

        test("milling only lands does not trigger") {
            val game = board(listOf("Forest", "Forest", "Forest"))
            game.castSpell(1, youMillThree.name).error shouldBe null
            game.resolveStack()
            game.state.pendingDecision shouldBe null
            game.state.stack.isEmpty() shouldBe true
        }

        test("attacking gives each player a rad counter") {
            val game = board(listOf("Forest"))
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("The Wise Mothman" to 2)).error shouldBe null
            game.resolveStack()
            listOf(game.player1Id, game.player2Id).forEach { game.rad(it) shouldBe 1 }
        }
    }
}
