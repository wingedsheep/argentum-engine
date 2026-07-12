package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Shattered Sanctum (VOW #264) — Land.
 *
 *   This land enters tapped unless you control two or more other lands.
 *   {T}: Add {W} or {B}.
 *
 * One of the "slow lands" — exercises the EntersTapped replacement effect gated on controlling
 * three or more lands total (the entering land plus two others).
 */
class ShatteredSanctumScenarioTest : ScenarioTestBase() {

    init {
        context("Shattered Sanctum enters-tapped condition") {

            test("enters tapped with fewer than two other lands") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Shattered Sanctum")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    PlayLand(
                        game.player1Id,
                        game.findCardsInHand(1, "Shattered Sanctum").single()
                    )
                ).error shouldBe null

                val sanctum = game.findPermanent("Shattered Sanctum")!!
                withClue("Enters tapped with only one other land") {
                    game.state.getEntity(sanctum)?.has<TappedComponent>() shouldBe true
                }
            }

            test("enters untapped with two or more other lands") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Shattered Sanctum")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    PlayLand(
                        game.player1Id,
                        game.findCardsInHand(1, "Shattered Sanctum").single()
                    )
                ).error shouldBe null

                val sanctum = game.findPermanent("Shattered Sanctum")!!
                withClue("Enters untapped with two other lands already in play") {
                    game.state.getEntity(sanctum)?.has<TappedComponent>() shouldBe false
                }
            }
        }
    }
}
