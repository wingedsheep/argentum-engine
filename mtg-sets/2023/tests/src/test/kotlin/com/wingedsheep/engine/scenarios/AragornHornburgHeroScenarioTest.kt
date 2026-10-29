package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.RenownedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Aragorn, Hornburg Hero (LTC #492) — "Attacking creatures you control have first strike and
 * renown 1. Whenever a renowned creature you control deals combat damage to a player, double the
 * number of +1/+1 counters on it."
 *
 * The 2023-11-03 ruling pinned here: a creature that isn't renowned when it connects gets renown's
 * counter and becomes renowned, but Aragorn's doubling doesn't trigger — it wasn't renowned when
 * it dealt the damage. Only a later hit, from an already-renowned creature, doubles.
 */
class AragornHornburgHeroScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(entity: EntityId): Int =
        state.getEntity(entity)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.isRenowned(entity: EntityId): Boolean =
        state.getEntity(entity)?.has<RenownedComponent>() == true

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Aragorn, Hornburg Hero")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.attackWithBearsAndFinishCombat() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        val bears = findPermanent("Grizzly Bears")!!
        withClue("an attacking creature you control has first strike and renown") {
            state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
            state.projectedState.hasKeyword(bears, Keyword.RENOWN) shouldBe true
        }
        passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
        resolveStack()
    }

    init {
        test("an attacker gains first strike and renown 1; its first hit renowns it without doubling") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val aragorn = game.findPermanent("Aragorn, Hornburg Hero")!!

            withClue("not attacking: no first strike, no renown") {
                game.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe false
                game.state.projectedState.hasKeyword(bears, Keyword.RENOWN) shouldBe false
                game.state.projectedState.hasKeyword(aragorn, Keyword.RENOWN) shouldBe false
            }

            game.attackWithBearsAndFinishCombat()

            withClue("the 2/2 Bears dealt 2 combat damage") { game.getLifeTotal(2) shouldBe 18 }
            withClue("renown 1 put one counter on and renowned the Bears") {
                game.isRenowned(bears) shouldBe true
            }
            withClue("Aragorn's doubling did not trigger — the Bears weren't renowned when they hit") {
                game.plusOnes(bears) shouldBe 1
            }
            withClue("combat over: the granted first strike and renown are gone") {
                game.state.projectedState.hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe false
                game.state.projectedState.hasKeyword(bears, Keyword.RENOWN) shouldBe false
            }
        }

        test("a renowned creature that connects again doubles its +1/+1 counters") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            // The Bears as a previous combat left them: renowned, with +1/+1 counters.
            game.state = game.state.updateEntity(bears) {
                it.with(RenownedComponent)
                    .with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 2))
            }

            game.attackWithBearsAndFinishCombat()

            withClue("the 4/4 Bears dealt 4 combat damage") { game.getLifeTotal(2) shouldBe 16 }
            withClue("renown's intervening-if failed; Aragorn doubled 2 counters to 4") {
                game.plusOnes(bears) shouldBe 4
            }
        }
    }
}
