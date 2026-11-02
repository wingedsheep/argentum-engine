package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
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
 * Vraska, Regal Gorgon (GRN #269, {5}{B}{G}, Loyalty 5).
 *
 *   +2: Put a +1/+1 counter on up to one target creature. That creature gains menace until end of turn.
 *   −3: Destroy target creature.
 *   −10: For each creature card in your graveyard, put a +1/+1 counter on each creature you control.
 *
 * Covers the −10's dynamic per-creature count (only creature cards in your graveyard count) and the
 * +2 both with a target and with no target at all.
 */
class VraskaRegalGorgonScenarioTest : ScenarioTestBase() {

    init {
        test("−10 puts one counter per creature card in your graveyard on each creature you control") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Vraska, Regal Gorgon")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withCardInGraveyard(1, "Forest")
                .withCardInGraveyard(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val vraska = game.findPermanent("Vraska, Regal Gorgon")!!
            val myBears = game.findPermanent("Grizzly Bears")!!
            val myGiant = game.findPermanent("Hill Giant")!!
            val theirBears = game.findPermanent("Savannah Lions")!!
            setLoyalty(game, vraska, 10)
            activate(game, vraska, index = 2)
            game.resolveStack()

            withClue("two creature cards in your graveyard: the land and the opponent's graveyard don't count") {
                plusOnes(game, myBears) shouldBe 2
                plusOnes(game, myGiant) shouldBe 2
            }
            withClue("only creatures you control get counters") {
                plusOnes(game, theirBears) shouldBe 0
            }
        }

        test("+2 puts a counter on the target creature and gives it menace") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Vraska, Regal Gorgon")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val vraska = game.findPermanent("Vraska, Regal Gorgon")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            setLoyalty(game, vraska, 5)
            activate(game, vraska, index = 0, targets = listOf(ChosenTarget.Permanent(bears)))
            game.resolveStack()

            plusOnes(game, bears) shouldBe 1
            game.state.projectedState.hasKeyword(bears, Keyword.MENACE) shouldBe true
            loyalty(game, vraska) shouldBe 7
        }

        test("+2 can be activated with no target, just for the loyalty") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Vraska, Regal Gorgon")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val vraska = game.findPermanent("Vraska, Regal Gorgon")!!
            setLoyalty(game, vraska, 5)
            activate(game, vraska, index = 0)
            game.resolveStack()

            loyalty(game, vraska) shouldBe 7
        }
    }

    private fun activate(
        game: TestGame,
        source: EntityId,
        index: Int,
        targets: List<ChosenTarget> = emptyList(),
    ) {
        val ability = cardRegistry.getCard("Vraska, Regal Gorgon")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
                targets = targets,
            )
        ).error shouldBe null
    }

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
