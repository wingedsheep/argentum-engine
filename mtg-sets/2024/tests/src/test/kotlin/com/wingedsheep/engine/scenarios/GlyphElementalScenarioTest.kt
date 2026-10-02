package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

class GlyphElementalScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.playForest() {
        execute(PlayLand(player1Id, findCardsInHand(1, "Forest").first())).error shouldBe null
        resolveStack()
    }

    init {
        test("as a creature, landfall puts a +1/+1 counter on itself") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Glyph Elemental")
                .withCardInHand(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val glyph = game.findPermanent("Glyph Elemental")!!

            game.playForest()

            game.plusOneCounters(glyph) shouldBe 1
            game.state.projectedState.getPower(glyph) shouldBe 3
            game.state.projectedState.getToughness(glyph) shouldBe 3
        }

        test("an opponent's land entering does not trigger landfall") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Glyph Elemental")
                .withCardInHand(2, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val glyph = game.findPermanent("Glyph Elemental")!!

            game.execute(PlayLand(game.player2Id, game.findCardsInHand(2, "Forest").first())).error shouldBe null
            game.resolveStack()

            game.plusOneCounters(glyph) shouldBe 0
        }

        test("bestowed, counters land on the Aura and the enchanted creature gets +1/+1 for each") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Glyph Elemental")
                .withCardInHand(1, "Forest")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val glyphCard = game.findCardsInHand(1, "Glyph Elemental").single()
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = glyphCard,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.BESTOW
                )
            ).error shouldBe null
            game.resolveStack()
            val glyph = game.findPermanent("Glyph Elemental")!!

            // No counters yet: the Aura grants nothing.
            game.state.projectedState.getPower(bears) shouldBe 2
            game.state.projectedState.isCreature(glyph) shouldBe false

            game.playForest()

            game.plusOneCounters(glyph) shouldBe 1
            game.plusOneCounters(bears) shouldBe 0
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 3
        }

        test("when the host leaves, it becomes a creature that keeps its counters") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Glyph Elemental")
                .withCardInHand(1, "Forest")
                .withCardInHand(1, "Unsummon")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Glyph Elemental").single(),
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.BESTOW
                )
            ).error shouldBe null
            game.resolveStack()
            val glyph = game.findPermanent("Glyph Elemental")!!
            game.playForest()

            game.castSpell(1, "Unsummon", bears).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.state.projectedState.isCreature(glyph) shouldBe true
            game.plusOneCounters(glyph) shouldBe 1
            game.state.projectedState.getPower(glyph) shouldBe 3
            game.state.projectedState.getToughness(glyph) shouldBe 3
        }
    }
}
