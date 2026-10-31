package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ajani, Inspiring Leader (M20 #282, {4}{W}{W}, Loyalty 5).
 *
 *   +2: You gain 2 life. Put two +1/+1 counters on up to one target creature.
 *   −3: Exile target creature. Its controller gains 2 life.
 *   −10: Creatures you control gain flying and double strike until end of turn.
 *
 * Covers the +2 with and without a target, the −3 paying the exiled creature's controller (not
 * Ajani's), and the −10 reaching only creatures Ajani's controller controls.
 */
class AjaniInspiringLeaderScenarioTest : ScenarioTestBase() {

    private val name = "Ajani, Inspiring Leader"

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
                .without<AbilityActivatedThisTurnComponent>()
        }
    }

    private fun activate(game: TestGame, source: EntityId, index: Int, targets: List<ChosenTarget> = emptyList()) {
        val ability = cardRegistry.getCard(name)!!.script.activatedAbilities[index]
        game.execute(ActivateAbility(game.player1Id, source, ability.id, targets = targets)).error shouldBe null
        game.resolveStack()
    }

    init {
        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, name)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("+2 gains 2 life and puts two +1/+1 counters on the target") {
            val game = board().build()
            val ajani = game.findPermanent(name)!!
            val bears = game.findPermanent("Grizzly Bears")!!

            activate(game, ajani, 0, listOf(ChosenTarget.Permanent(bears)))

            game.getLifeTotal(1) shouldBe 22
            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 4
            loyalty(game, ajani) shouldBe 7
        }

        test("+2 with no target still gains 2 life") {
            val game = board().build()
            val ajani = game.findPermanent(name)!!
            val bears = game.findPermanent("Grizzly Bears")!!

            activate(game, ajani, 0)

            game.getLifeTotal(1) shouldBe 22
            game.state.projectedState.getPower(bears) shouldBe 2
            loyalty(game, ajani) shouldBe 7
        }

        test("−3 exiles the target and its controller gains 2 life") {
            val game = board().build()
            val ajani = game.findPermanent(name)!!
            val giant = game.findPermanent("Hill Giant")!!

            activate(game, ajani, 1, listOf(ChosenTarget.Permanent(giant)))

            game.findPermanent("Hill Giant") shouldBe null
            withClue("the opponent controlled the exiled creature, so they gain the life") {
                game.getLifeTotal(2) shouldBe 22
                game.getLifeTotal(1) shouldBe 20
            }
            loyalty(game, ajani) shouldBe 2
        }

        test("−10 gives only your creatures flying and double strike until end of turn") {
            val game = board().build()
            val ajani = game.findPermanent(name)!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            seedLoyalty(game, ajani, 10)

            activate(game, ajani, 2)

            game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
            game.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe true
            game.state.projectedState.hasKeyword(giant, Keyword.FLYING) shouldBe false
            game.state.projectedState.hasKeyword(giant, Keyword.DOUBLE_STRIKE) shouldBe false
        }
    }
}
