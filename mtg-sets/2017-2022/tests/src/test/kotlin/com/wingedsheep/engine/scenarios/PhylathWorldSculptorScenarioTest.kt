package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Phylath, World Sculptor (ZNR #234) — {4}{R}{G} Legendary Creature — Elemental, 5/5.
 *
 *   When Phylath enters, create a 0/1 green Plant creature token for each basic land you control.
 *   Landfall — Whenever a land you control enters, put four +1/+1 counters on target Plant you control.
 *
 * Gruul Turf is a nonbasic land, so it pays mana but adds no Plant.
 */
class PhylathWorldSculptorScenarioTest : ScenarioTestBase() {

    init {
        test("enters making a Plant per basic land, then landfall puts four counters on a Plant") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Phylath, World Sculptor")
                .withCardInHand(1, "Forest")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(1, "Gruul Turf")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Phylath, World Sculptor").error shouldBe null
            game.resolveStack()

            val plants = game.findPermanents("Plant Token")
            withClue("four basic lands -> four Plants; the nonbasic Gruul Turf adds none") {
                plants.size shouldBe 4
            }

            val plant = plants.first()
            val forest = game.findCardsInHand(1, "Forest").single()
            game.execute(PlayLand(game.player1Id, forest)).error shouldBe null
            game.resolveStack()
            if (game.hasPendingDecision()) {
                game.selectTargets(listOf(plant)).error shouldBe null
            }
            game.resolveStack()

            withClue("the targeted Plant gets four +1/+1 counters") {
                game.state.getEntity(plant)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4
                game.state.projectedState.getPower(plant) shouldBe 4
                game.state.projectedState.getToughness(plant) shouldBe 5
            }
        }
    }
}
