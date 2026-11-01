package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.war.cards.JiangYangguWildcrafter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Jiang Yanggu, Wildcrafter (WAR #164, {2}{G}, Loyalty 3).
 *
 *   Each creature you control with a +1/+1 counter on it has "{T}: Add one mana of any color."
 *   −1: Put a +1/+1 counter on target creature.
 *
 * The first card granting a mana ability through a counter-gated group filter: proves the grant
 * reaches a creature with a +1/+1 counter, skips one without, and switches on once the −1 adds a
 * counter.
 */
class JiangYangguWildcrafterScenarioTest : ScenarioTestBase() {

    private val name = "Jiang Yanggu, Wildcrafter"

    private val grantedManaAbilityId = JiangYangguWildcrafter.staticAbilities
        .filterIsInstance<GrantActivatedAbility>().single().ability.id

    private fun seedCounters(game: TestGame, id: EntityId, type: CounterType, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            val counters = c.get<CountersComponent>() ?: CountersComponent()
            c.with(counters.withAdded(type, amount)).without<AbilityActivatedThisTurnComponent>()
        }
    }

    private fun tapForMana(game: TestGame, creature: EntityId, color: Color) =
        game.execute(ActivateAbility(game.player1Id, creature, grantedManaAbilityId, manaColorChoice = color))

    private fun pool(game: TestGame) = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!

    init {
        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, name)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Hill Giant")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("a creature you control with a +1/+1 counter taps for one mana of the chosen colour") {
            val game = board().build()
            val bears = game.findPermanent("Grizzly Bears")!!
            seedCounters(game, bears, CounterType.PLUS_ONE_PLUS_ONE, 1)

            tapForMana(game, bears, Color.RED).error shouldBe null

            val pool = pool(game)
            withClue("one red mana from the granted ability, pool was $pool") {
                pool.red shouldBe 1
                pool.total shouldBe 1
            }
        }

        test("a creature without a +1/+1 counter does not have the ability") {
            val game = board().build()
            val giant = game.findPermanent("Hill Giant")!!

            tapForMana(game, giant, Color.GREEN).error shouldNotBe null
            pool(game).total shouldBe 0
        }

        test("−1 puts a +1/+1 counter on the target, which then gains the mana ability") {
            val game = board().build()
            val jiang = game.findPermanent(name)!!
            val giant = game.findPermanent("Hill Giant")!!

            val minusOne = cardRegistry.getCard(name)!!.script.activatedAbilities[0]
            game.execute(
                ActivateAbility(game.player1Id, jiang, minusOne.id, targets = listOf(ChosenTarget.Permanent(giant)))
            ).error shouldBe null
            game.resolveStack()

            game.state.getEntity(giant)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            game.state.getEntity(jiang)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 2

            tapForMana(game, giant, Color.BLUE).error shouldBe null
            pool(game).blue shouldBe 1
        }
    }
}
