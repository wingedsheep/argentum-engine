package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.TemperamentalOozewagg
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Temperamental Oozewagg (MH3 #172): "{2}{G}: Adapt 2. Modified creatures you control have trample."
 */
class TemperamentalOozewaggScenarioTest : ScenarioTestBase() {

    init {
        context("Temperamental Oozewagg") {

            test("only modified creatures you control have trample") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Temperamental Oozewagg")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val courser = game.findPermanent("Centaur Courser")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                listOf(courser, giant).forEach { id ->
                    game.state = game.state.updateEntity(id) {
                        it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                    }
                }

                val projected = game.state.projectedState
                projected.hasKeyword(courser, Keyword.TRAMPLE) shouldBe true
                projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false
                projected.hasKeyword(giant, Keyword.TRAMPLE) shouldBe false
                projected.hasKeyword(game.findPermanent("Temperamental Oozewagg")!!, Keyword.TRAMPLE) shouldBe false
            }

            test("adapt 2 puts two counters once, giving it trample; a second activation adds none") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Temperamental Oozewagg")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val ooze = game.findPermanent("Temperamental Oozewagg")!!
                val abilityId = TemperamentalOozewagg.activatedAbilities.first().id

                repeat(2) {
                    game.execute(
                        ActivateAbility(playerId = game.player1Id, sourceId = ooze, abilityId = abilityId)
                    ).error shouldBe null
                    game.resolveStack()
                }

                val projected = game.state.projectedState
                projected.getPower(ooze) shouldBe 6
                projected.getToughness(ooze) shouldBe 6
                projected.hasKeyword(ooze, Keyword.TRAMPLE) shouldBe true
            }
        }
    }
}
