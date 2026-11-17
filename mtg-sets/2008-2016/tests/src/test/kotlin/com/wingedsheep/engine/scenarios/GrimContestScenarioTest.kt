package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Grim Contest (FRF #153) — {1}{B}{G} Instant.
 *
 *   Choose target creature you control and target creature an opponent controls. Each of those
 *   creatures deals damage equal to its toughness to the other.
 *
 * Horned Turtle (1/4) deals 4 — its toughness, not its power — to Hill Giant (3/3), which deals 3
 * back. The illegal-target ruling is pinned in `MutualDamageEffectTest`.
 */
class GrimContestScenarioTest : ScenarioTestBase() {

    init {
        context("Grim Contest") {

            fun game() = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Grim Contest")
                .withCardOnBattlefield(1, "Horned Turtle")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("your 1/4 deals 4 to the opponent's 3/3 and survives its 3") {
                val game = game()
                val turtle = game.findPermanent("Horned Turtle")!!
                val giant = game.findPermanent("Hill Giant")!!
                val spell = game.state.getHand(game.player1Id).single {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Grim Contest"
                }

                val cast = game.execute(
                    CastSpell(game.player1Id, spell, listOf(ChosenTarget.Permanent(turtle), ChosenTarget.Permanent(giant)))
                )
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Hill Giant took 4 and died") { game.isInGraveyard(2, "Hill Giant") shouldBe true }
                withClue("Horned Turtle took 3 and survived") {
                    game.findPermanent("Horned Turtle") shouldBe turtle
                    game.state.getEntity(turtle)?.get<DamageComponent>()?.amount shouldBe 3
                }
            }
        }
    }
}
