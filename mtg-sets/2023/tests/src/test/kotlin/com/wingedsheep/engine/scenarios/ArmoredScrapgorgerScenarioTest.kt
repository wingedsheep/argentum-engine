package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Armored Scrapgorger (ONE #158) — {1}{G} 0/3 Phyrexian Beast.
 *
 *   This creature gets +3/+0 as long as it has three or more oil counters on it.
 *   {T}: Add one mana of any color.
 *   Whenever this creature becomes tapped, exile target card from a graveyard and put an oil
 *   counter on this creature.
 */
class ArmoredScrapgorgerScenarioTest : ScenarioTestBase() {

    private fun seedOil(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.OIL, amount))
        }
    }

    private fun oil(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    init {
        test("becoming tapped exiles a graveyard card and adds an oil counter") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Armored Scrapgorger")
                .withCardInGraveyard(2, "Hill Giant")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            val gorger = game.findPermanent("Armored Scrapgorger")!!
            val giant = game.findCardsInGraveyard(2, "Hill Giant").single()

            game.declareAttackers(mapOf("Armored Scrapgorger" to 2)).error shouldBe null
            game.state.pendingDecision.shouldNotBeNull()
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Hill Giant") shouldBe true
            oil(game, gorger) shouldBe 1
            game.state.projectedState.getPower(gorger) shouldBe 0
        }

        test("gets +3/+0 only with three or more oil counters") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Armored Scrapgorger")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val gorger = game.findPermanent("Armored Scrapgorger")!!

            seedOil(game, gorger, 2)
            game.state.projectedState.getPower(gorger) shouldBe 0
            game.state.projectedState.getToughness(gorger) shouldBe 3

            seedOil(game, gorger, 1)
            game.state.projectedState.getPower(gorger) shouldBe 3
            game.state.projectedState.getToughness(gorger) shouldBe 3
        }
    }
}
