package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.mtg.sets.definitions.rix.cards.HadanasClimb
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hadana's Climb // Winged Temple of Orazca (RIX #158).
 *
 * Pins the counter threshold, read on resolution off the targeted creature after the new counter
 * lands (two existing counters + one = transform; one + one = stay), and the back face's
 * "+X/+X where X is its power" pump with flying.
 */
class HadanasClimbScenarioTest : ScenarioTestBase() {

    private fun game(existingCounters: Int): Pair<TestGame, EntityId> {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Hadana's Climb")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Forest", 1)
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val bears = game.findPermanent("Grizzly Bears")!!
        game.state = game.state.updateEntity(bears) { c ->
            c.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, existingCounters))
        }
        return game to bears
    }

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("transforms when the targeted creature reaches three +1/+1 counters") {
            val (game, bears) = game(existingCounters = 2)
            val climb = game.findPermanent("Hadana's Climb")!!

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            plusOnes(game, bears) shouldBe 3
            game.state.getEntity(climb)?.get<CardComponent>()?.name shouldBe "Winged Temple of Orazca"
        }

        test("stays an enchantment when the creature has fewer than three counters") {
            val (game, bears) = game(existingCounters = 1)
            val climb = game.findPermanent("Hadana's Climb")!!

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            plusOnes(game, bears) shouldBe 2
            game.state.getEntity(climb)?.get<CardComponent>()?.name shouldBe "Hadana's Climb"
        }

        test("Winged Temple gives target creature you control flying and +X/+X equal to its power") {
            val (game, bears) = game(existingCounters = 2)
            val climb = game.findPermanent("Hadana's Climb")!!
            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            withClue("Grizzly Bears with three counters is 5/5") {
                game.state.projectedState.getPower(bears) shouldBe 5
            }

            val pump = HadanasClimb.backFace!!.activatedAbilities[1].id
            game.execute(
                ActivateAbility(game.player1Id, climb, pump, targets = listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 10
            projected.getToughness(bears) shouldBe 10
            projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
        }
    }
}
