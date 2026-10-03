package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Branching Evolution (JMP #29, reprinted MH3 #285) — {2}{G} Enchantment.
 * If one or more +1/+1 counters would be put on a creature you control, twice that many +1/+1
 * counters are put on that creature instead.
 *
 * Doubles only on creatures you control: an uncrewed (noncreature) Vehicle you control and an
 * opponent's creature are untouched. Two copies quadruple (ruling 2020-06-23).
 */
class BranchingEvolutionScenarioTest : ScenarioTestBase() {

    private fun abilityIdOf(card: String) = cardRegistry.getCard(card)!!.script.activatedAbilities[0].id

    private fun plusOneCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Dragon Blood targets a creature; Daring Mechanic targets a Mount or Vehicle. */
    private fun putCounterWith(game: TestGame, source: String, target: EntityId) {
        val result = game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = game.findPermanent(source)!!,
                abilityId = abilityIdOf(source),
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
        game.resolveStack()
    }

    private fun board(evolutions: Int = 1, bearsOwner: Int = 1) = scenario()
        .withPlayers("Player", "Opponent")
        .apply { repeat(evolutions) { withCardOnBattlefield(1, "Branching Evolution") } }
        .withCardOnBattlefield(1, "Daring Mechanic")
        .withCardOnBattlefield(1, "Dragon Blood")
        .withCardOnBattlefield(bearsOwner, "Grizzly Bears")
        .withCardOnBattlefield(1, "Air Response Unit") // uncrewed Vehicle
        .withLandsOnBattlefield(1, "Plains", 4)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Branching Evolution") {

            test("a creature you control gets twice the +1/+1 counters") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                putCounterWith(game, "Dragon Blood", bears)
                plusOneCounters(game, bears) shouldBe 2
            }

            test("two Branching Evolutions quadruple the counters") {
                val game = board(evolutions = 2)
                val bears = game.findPermanent("Grizzly Bears")!!
                putCounterWith(game, "Dragon Blood", bears)
                plusOneCounters(game, bears) shouldBe 4
            }

            test("a noncreature permanent you control is not doubled") {
                val game = board()
                val unit = game.findPermanent("Air Response Unit")!!
                game.state.projectedState.isCreature(unit) shouldBe false
                putCounterWith(game, "Daring Mechanic", unit)
                plusOneCounters(game, unit) shouldBe 1
            }

            test("an opponent's creature is not doubled") {
                val game = board(bearsOwner = 2)
                val bears = game.findPermanent("Grizzly Bears")!!
                putCounterWith(game, "Dragon Blood", bears)
                plusOneCounters(game, bears) shouldBe 1
            }
        }
    }
}
