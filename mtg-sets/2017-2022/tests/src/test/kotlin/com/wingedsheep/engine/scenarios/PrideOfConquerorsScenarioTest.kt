package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.PlayerCitysBlessingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Pride of Conquerors (RIX #17) — {1}{W} Instant.
 *   "Ascend. Creatures you control get +1/+1 until end of turn. If you have the city's blessing,
 *    those creatures get +2/+2 until end of turn instead."
 *
 * First card to lower ascend on an instant (CR 702.131a) through `Effects.GainCitysBlessing`:
 * the spell itself must grant the blessing at resolution — no ascend permanent is on the
 * battlefield — and the pump that follows must see the blessing it just granted.
 */
class PrideOfConquerorsScenarioTest : ScenarioTestBase() {

    init {
        test("with ten permanents the spell grants the city's blessing and the pump becomes +2/+2") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Pride of Conquerors")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 9) // Bears + 9 Plains = ten permanents
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("no ascend permanent, so no blessing before the spell resolves") {
                game.state.getEntity(game.player1Id)?.has<PlayerCitysBlessingComponent>() shouldBe false
            }

            game.castSpell(1, "Pride of Conquerors").error shouldBe null
            game.resolveStack()

            withClue("the spell's ascend granted the city's blessing") {
                game.state.getEntity(game.player1Id)?.has<PlayerCitysBlessingComponent>() shouldBe true
            }
            val mine = game.state.getBattlefield(game.player1Id)
                .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }
            withClue("your creature gets +2/+2 instead of +1/+1") {
                game.state.projectedState.getPower(mine) shouldBe 4
                game.state.projectedState.getToughness(mine) shouldBe 4
            }
            val theirs = game.state.getBattlefield(game.player2Id)
                .first { game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears" }
            withClue("opponents' creatures are untouched") {
                game.state.projectedState.getPower(theirs) shouldBe 2
            }
        }

        test("below ten permanents the pump is +1/+1 and no blessing is granted") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Pride of Conquerors")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Pride of Conquerors").error shouldBe null
            game.resolveStack()

            withClue("three permanents do not ascend") {
                game.state.getEntity(game.player1Id)?.has<PlayerCitysBlessingComponent>() shouldBe false
            }
            val bears = game.findPermanent("Grizzly Bears")!!
            withClue("your creature gets +1/+1") {
                game.state.projectedState.getPower(bears) shouldBe 3
                game.state.projectedState.getToughness(bears) shouldBe 3
            }
        }
    }
}
