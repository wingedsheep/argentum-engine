package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Wild Instincts (ORI #206) — {3}{G} Sorcery.
 *
 *   Target creature you control gets +2/+2 until end of turn. It fights target creature an
 *   opponent controls. (Each deals damage equal to its power to the other.)
 *
 * The pump lands before the fight: a 2/2 becomes 4/4, kills a 3/3 and survives its 3 damage.
 */
class WildInstinctsScenarioTest : ScenarioTestBase() {

    init {
        context("Wild Instincts") {

            test("your pumped 2/2 fights and kills the opponent's 3/3, surviving as a 4/4") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wild Instincts")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val spell = game.state.getHand(game.player1Id).single {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Wild Instincts"
                }

                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        spell,
                        listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(giant))
                    )
                )
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Hill Giant took 4 and died") { game.isInGraveyard(2, "Hill Giant") shouldBe true }
                withClue("Grizzly Bears survived the Giant's 3 damage as a 4/4") {
                    game.findPermanent("Grizzly Bears") shouldBe bears
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }
                game.isInGraveyard(1, "Wild Instincts") shouldBe true
            }
        }
    }
}
