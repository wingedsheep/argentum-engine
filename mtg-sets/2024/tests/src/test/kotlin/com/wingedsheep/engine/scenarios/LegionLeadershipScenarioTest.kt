package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Legion Leadership // Legion Stronghold (MH3).
 *
 * Front: "Until end of turn, double target creature's power and it gains first strike."
 * Back: "This land enters tapped. {T}: Add {R} or {W}."
 */
class LegionLeadershipScenarioTest : ScenarioTestBase() {

    private fun castGame() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Legion Leadership")
        .withCardInHand(1, "Giant Growth")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withLandsOnBattlefield(1, "Plains", 1)
        .withLandsOnBattlefield(1, "Forest", 1)
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Legion Leadership — the instant front") {

            test("doubles the target's power until end of turn and grants first strike") {
                val game = castGame()
                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Legion Leadership", giant).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                projected.getPower(giant) shouldBe 6
                projected.getToughness(giant) shouldBe 3
                projected.hasKeyword(giant, Keyword.FIRST_STRIKE) shouldBe true
            }

            test("doubles the power the creature has as the spell resolves") {
                val game = castGame()
                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Legion Leadership", giant).error shouldBe null
                // Giant Growth in response resolves first: 6/6, then doubled to 12/6.
                game.castSpell(1, "Giant Growth", giant).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(giant) shouldBe 12
                game.state.projectedState.getToughness(giant) shouldBe 6
            }

            test("the bonus is locked in at resolution; a later pump isn't doubled") {
                val game = castGame()
                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Legion Leadership", giant).error shouldBe null
                game.resolveStack()
                game.castSpell(1, "Giant Growth", giant).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(giant) shouldBe 9
                game.state.projectedState.getToughness(giant) shouldBe 6
            }
        }

        context("Legion Stronghold — the land back") {

            test("played as a land, it enters tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Legion Leadership")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val card = game.state.getHand(game.player1Id).single()
                game.execute(PlayLand(game.player1Id, card, asBackFace = true)).error shouldBe null

                val land = game.findPermanent("Legion Stronghold")!!
                game.state.getEntity(land)!!.has<TappedComponent>() shouldBe true
            }
        }
    }
}
