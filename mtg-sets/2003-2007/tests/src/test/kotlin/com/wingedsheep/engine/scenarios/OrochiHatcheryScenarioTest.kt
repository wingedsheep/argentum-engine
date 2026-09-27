package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.OrochiHatchery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Orochi Hatchery (CHK #266) — {X}{X} Artifact.
 *
 * "This artifact enters with X charge counters on it.
 *  {5}, {T}: Create a 1/1 green Snake creature token for each charge counter on this artifact."
 */
class OrochiHatcheryScenarioTest : ScenarioTestBase() {

    private val abilityId = OrochiHatchery.activatedAbilities.single().id

    private fun charge(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0

    private fun snakes(game: TestGame): List<EntityId> =
        game.state.getBattlefield(game.player1Id).filter { id ->
            val e = game.state.getEntity(id)!!
            e.has<TokenComponent>() && game.state.projectedState.isCreature(id) &&
                game.state.projectedState.getSubtypes(id).contains("Snake")
        }

    init {
        context("Orochi Hatchery") {

            test("cast for X = 3 enters with three charge counters and makes three 1/1 green Snakes") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withLandsOnBattlefield(1, "Forest", 11)
                    .withCardInHand(1, "Orochi Hatchery")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Orochi Hatchery", xValue = 3).error shouldBe null
                game.resolveStack()

                val hatchery = game.findPermanent("Orochi Hatchery")!!
                charge(game, hatchery) shouldBe 3

                game.execute(ActivateAbility(game.player1Id, hatchery, abilityId)).error shouldBe null
                game.resolveStack()

                val made = snakes(game)
                made.size shouldBe 3
                made.forEach { id ->
                    game.state.projectedState.getPower(id) shouldBe 1
                    game.state.projectedState.getToughness(id) shouldBe 1
                    game.state.getEntity(id)!!.get<CardComponent>()!!.colors shouldBe setOf(Color.GREEN)
                }
            }

            test("cast for X = 0 enters with no counters and its ability makes no Snakes") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInHand(1, "Orochi Hatchery")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Orochi Hatchery", xValue = 0).error shouldBe null
                game.resolveStack()

                val hatchery = game.findPermanent("Orochi Hatchery")!!
                charge(game, hatchery) shouldBe 0

                game.execute(ActivateAbility(game.player1Id, hatchery, abilityId)).error shouldBe null
                game.resolveStack()

                snakes(game).size shouldBe 0
            }
        }
    }
}
