package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

class TritonWavebreakerScenarioTest : ScenarioTestBase() {
    private val projector = StateProjector()

    private fun TestGame.bestow(target: EntityId) = execute(
        CastSpell(
            playerId = player1Id,
            cardId = findCardsInHand(1, "Triton Wavebreaker").single(),
            targets = listOf(ChosenTarget.Permanent(target)),
            useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.BESTOW
        )
    )

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardInHand(1, "Triton Wavebreaker")
        .withCardInHand(1, "Wavebreaker Test Instant")
        .withCardInHand(1, "Unsummon")
        .withLandsOnBattlefield(1, "Island", 5)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        cardRegistry.register(CardDefinition.instant(
            name = "Wavebreaker Test Instant", manaCost = ManaCost.parse("{U}"), oracleText = ""
        ))

        test("normal casting makes a creature whose conditional prowess triggers") {
            val game = board()
            game.castSpell(1, "Triton Wavebreaker").error shouldBe null
            game.resolveStack()
            val triton = game.findPermanent("Triton Wavebreaker")!!
            projector.project(game.state).isCreature(triton) shouldBe true
            game.castSpell(1, "Wavebreaker Test Instant").error shouldBe null
            game.resolveStack()
            projector.project(game.state).getPower(triton) shouldBe 2
            projector.project(game.state).getToughness(triton) shouldBe 2
        }

        test("bestow grants prowess to the host and does not grant prowess to the Aura") {
            val game = board()
            val bear = game.findPermanent("Grizzly Bears")!!
            game.bestow(bear).error shouldBe null
            game.resolveStack()
            val triton = game.findPermanent("Triton Wavebreaker")!!
            game.state.getEntity(triton)?.get<AttachedToComponent>()?.targetId shouldBe bear
            val before = projector.project(game.state)
            before.isCreature(triton) shouldBe false
            before.hasKeyword(triton, Keyword.PROWESS) shouldBe false
            before.hasKeyword(bear, Keyword.PROWESS) shouldBe true
            before.getPower(bear) shouldBe 3
            game.castSpell(1, "Wavebreaker Test Instant").error shouldBe null
            game.resolveStack()
            projector.project(game.state).getPower(bear) shouldBe 4
            projector.project(game.state).getToughness(bear) shouldBe 4
        }

        test("an illegal bestow target makes the spell resolve as a creature") {
            val game = board()
            val bear = game.findPermanent("Grizzly Bears")!!
            game.bestow(bear).error shouldBe null
            game.castSpell(1, "Unsummon", bear).error shouldBe null
            game.resolveStack()
            val triton = game.findPermanent("Triton Wavebreaker")!!
            projector.project(game.state).isCreature(triton) shouldBe true
            projector.project(game.state).hasKeyword(triton, Keyword.PROWESS) shouldBe true
            game.state.getEntity(triton)?.get<AttachedToComponent>() shouldBe null
        }

        test("losing its host preserves the permanent and activates its own prowess") {
            val game = board()
            val bear = game.findPermanent("Grizzly Bears")!!
            game.bestow(bear).error shouldBe null
            game.resolveStack()
            val triton = game.findPermanent("Triton Wavebreaker")!!
            game.castSpell(1, "Unsummon", bear).error shouldBe null
            game.resolveStack()
            game.findPermanent("Triton Wavebreaker") shouldBe triton
            projector.project(game.state).isCreature(triton) shouldBe true
            projector.project(game.state).getPower(triton) shouldBe 1
            game.state.getEntity(triton)?.get<AttachedToComponent>() shouldBe null
            game.castSpell(1, "Wavebreaker Test Instant").error shouldBe null
            game.resolveStack()
            projector.project(game.state).getPower(triton) shouldBe 2
        }
    }
}
