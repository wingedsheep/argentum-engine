package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

class NyxbornHydraScenarioTest : ScenarioTestBase() {
    private val projector = StateProjector()

    private fun TestGame.hydraInHand() = findCardsInHand(1, "Nyxborn Hydra").single()

    private fun TestGame.castNormally(x: Int) = execute(
        CastSpell(playerId = player1Id, cardId = hydraInHand(), xValue = x)
    )

    private fun TestGame.bestow(target: EntityId, x: Int) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = hydraInHand(),
            targets = listOf(ChosenTarget.Permanent(target)),
            useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.BESTOW,
            xValue = x
        )
    )

    private fun TestGame.plusOneCounters(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardInHand(1, "Nyxborn Hydra")
        .withCardInHand(1, "Unsummon")
        .withLandsOnBattlefield(1, "Forest", 6)
        .withLandsOnBattlefield(1, "Island", 2)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("cast normally for X=3 it is a 3/4 with reach and trample") {
            val game = board()
            game.castNormally(3).error shouldBe null
            game.resolveStack()
            val hydra = game.findPermanent("Nyxborn Hydra")!!
            game.plusOneCounters(hydra) shouldBe 3
            val projected = projector.project(game.state)
            projected.isCreature(hydra) shouldBe true
            projected.getPower(hydra) shouldBe 3
            projected.getToughness(hydra) shouldBe 4
            projected.hasKeyword(hydra, Keyword.REACH) shouldBe true
            projected.hasKeyword(hydra, Keyword.TRAMPLE) shouldBe true
            val bear = game.findPermanent("Grizzly Bears")!!
            projected.getPower(bear) shouldBe 2
            projected.hasKeyword(bear, Keyword.TRAMPLE) shouldBe false
        }

        test("bestowed for X=2 the counters go on the Aura and pump the host with reach and trample") {
            val game = board()
            val bear = game.findPermanent("Grizzly Bears")!!
            game.bestow(bear, x = 2).error shouldBe null
            game.resolveStack()
            val hydra = game.findPermanent("Nyxborn Hydra")!!
            game.state.getEntity(hydra)?.get<AttachedToComponent>()?.targetId shouldBe bear
            game.plusOneCounters(hydra) shouldBe 2
            game.plusOneCounters(bear) shouldBe 0
            val projected = projector.project(game.state)
            projected.isCreature(hydra) shouldBe false
            projected.getPower(bear) shouldBe 4
            projected.getToughness(bear) shouldBe 4
            projected.hasKeyword(bear, Keyword.REACH) shouldBe true
            projected.hasKeyword(bear, Keyword.TRAMPLE) shouldBe true
        }

        test("losing its host leaves an X/X+1 creature that keeps its counters") {
            val game = board()
            val bear = game.findPermanent("Grizzly Bears")!!
            game.bestow(bear, x = 2).error shouldBe null
            game.resolveStack()
            val hydra = game.findPermanent("Nyxborn Hydra")!!
            game.castSpell(1, "Unsummon", bear).error shouldBe null
            game.resolveStack()
            game.findPermanent("Nyxborn Hydra") shouldBe hydra
            game.state.getEntity(hydra)?.get<AttachedToComponent>() shouldBe null
            val projected = projector.project(game.state)
            projected.isCreature(hydra) shouldBe true
            projected.getPower(hydra) shouldBe 2
            projected.getToughness(hydra) shouldBe 3
        }

        test("an illegal bestow target resolves it as a creature with its X counters") {
            val game = board()
            val bear = game.findPermanent("Grizzly Bears")!!
            game.bestow(bear, x = 1).error shouldBe null
            game.castSpell(1, "Unsummon", bear).error shouldBe null
            game.resolveStack()
            val hydra = game.findPermanent("Nyxborn Hydra")!!
            game.state.getEntity(hydra)?.get<AttachedToComponent>() shouldBe null
            val projected = projector.project(game.state)
            projected.isCreature(hydra) shouldBe true
            projected.getPower(hydra) shouldBe 1
            projected.getToughness(hydra) shouldBe 2
        }
    }
}
