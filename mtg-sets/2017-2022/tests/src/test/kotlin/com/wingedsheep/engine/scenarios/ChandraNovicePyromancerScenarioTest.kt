package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m20.cards.ChandraNovicePyromancer
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Chandra, Novice Pyromancer (M20 #128).
 *
 *   +1: Elementals you control get +2/+0 until end of turn.
 *   −1: Add {R}{R}.
 *   −2: Chandra deals 2 damage to any target.
 *
 * Pins the +1's filter (only your Elementals), the −1 going through the stack into the pool, and
 * the −2's damage.
 */
class ChandraNovicePyromancerScenarioTest : ScenarioTestBase() {

    init {
        test("+1 gives only Elementals you control +2/+0") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Chandra, Novice Pyromancer")
                .withCardOnBattlefield(1, "Fire Elemental")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Air Elemental")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val chandra = game.findPermanent("Chandra, Novice Pyromancer")!!
            val fire = game.findPermanent("Fire Elemental")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val air = game.findPermanent("Air Elemental")!!
            activate(game, chandra, index = 0)
            game.resolveStack()

            withClue("your Elemental 5/4 -> 7/4") {
                game.state.projectedState.getPower(fire) shouldBe 7
                game.state.projectedState.getToughness(fire) shouldBe 4
            }
            withClue("a non-Elemental is untouched") { game.state.projectedState.getPower(bears) shouldBe 2 }
            withClue("an opponent's Elemental is untouched") { game.state.projectedState.getPower(air) shouldBe 4 }
            withClue("+1 took Chandra from 5 to 6") { loyalty(game, chandra) shouldBe 6 }
        }

        test("−1 adds {R}{R}") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Chandra, Novice Pyromancer")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val chandra = game.findPermanent("Chandra, Novice Pyromancer")!!
            activate(game, chandra, index = 1)
            game.resolveStack()

            withClue("two red mana in the pool") {
                game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.red shouldBe 2
            }
            withClue("−1 took Chandra from 5 to 4") { loyalty(game, chandra) shouldBe 4 }
        }

        test("−2 deals 2 damage to any target") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Chandra, Novice Pyromancer")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val chandra = game.findPermanent("Chandra, Novice Pyromancer")!!
            activate(game, chandra, index = 2, targets = listOf(ChosenTarget.Player(game.player2Id)))
            game.resolveStack()

            withClue("opponent took 2") { game.getLifeTotal(2) shouldBe 18 }
            withClue("−2 took Chandra from 5 to 3") { loyalty(game, chandra) shouldBe 3 }
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int, targets: List<ChosenTarget> = emptyList()) {
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ChandraNovicePyromancer.activatedAbilities[index].id,
                targets = targets,
            )
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0
}
