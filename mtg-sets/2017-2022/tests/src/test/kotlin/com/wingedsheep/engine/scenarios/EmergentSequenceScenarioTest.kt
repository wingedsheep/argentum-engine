package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Emergent Sequence (STX #129) — {1}{G} Sorcery.
 *
 * "Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.
 *  That land becomes a 0/0 green and blue Fractal creature that's still a land. Put a +1/+1
 *  counter on it for each land you had enter the battlefield under your control this turn."
 *
 * Covers the searched land being animated through its pipeline handle (still a land, keeps its
 * basic land type, green and blue, 0/0 base) and the counter count reading the per-turn entry log
 * *after* the land entered, so it counts itself (ruling) plus any land played earlier.
 */
class EmergentSequenceScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, entityId: EntityId): Int =
        game.state.getEntity(entityId)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("the searched land becomes a tapped green-blue Fractal land creature counting itself and the land played") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Emergent Sequence")
                .withCardInHand(1, "Forest")
                .withCardInLibrary(1, "Island")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val forestInHand = game.findCardsInHand(1, "Forest").single()
            game.execute(PlayLand(game.player1Id, forestInHand)).error shouldBe null

            game.castSpell(1, "Emergent Sequence").error shouldBe null
            game.resolveStack()

            val island = game.findCardsInLibrary(1, "Island").single()
            withClue("the caster picks the basic land") {
                game.getPendingDecision()?.playerId shouldBe game.player1Id
            }
            game.selectCards(listOf(island)).error shouldBe null

            val projected = game.state.projectedState
            withClue("it entered tapped and is a land creature") {
                game.state.getEntity(island)!!.has<TappedComponent>() shouldBe true
                projected.isCreature(island) shouldBe true
                projected.hasType(island, "LAND") shouldBe true
                projected.hasSubtype(island, "Island") shouldBe true
                projected.hasSubtype(island, "Fractal") shouldBe true
                projected.getColors(island) shouldBe setOf("GREEN", "BLUE")
            }
            withClue("two lands entered this turn: the played Forest and the Island itself") {
                plusOneCounters(game, island) shouldBe 2
                projected.getPower(island) shouldBe 2
                projected.getToughness(island) shouldBe 2
            }
        }

        test("with no other land this turn the Fractal is a 1/1") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Emergent Sequence")
                .withCardInLibrary(1, "Forest")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Emergent Sequence").error shouldBe null
            game.resolveStack()

            val forest = game.findCardsInLibrary(1, "Forest").single()
            game.selectCards(listOf(forest)).error shouldBe null

            plusOneCounters(game, forest) shouldBe 1
            game.state.projectedState.getPower(forest) shouldBe 1
            game.state.projectedState.getToughness(forest) shouldBe 1
        }
    }
}
