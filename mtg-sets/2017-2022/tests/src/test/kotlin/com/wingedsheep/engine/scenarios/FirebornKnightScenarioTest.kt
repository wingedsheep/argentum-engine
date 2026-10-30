package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.eld.cards.FirebornKnight
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Fireborn Knight (ELD #210) — Double strike. {R/W}{R/W}{R/W}{R/W}: This creature gets +1/+1
 * until end of turn.
 */
class FirebornKnightScenarioTest : ScenarioTestBase() {

    init {
        context("Fireborn Knight") {

            test("hybrid pump paid with a mix of red and white mana gives +1/+1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Fireborn Knight")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val knight = game.findPermanent("Fireborn Knight")!!
                game.state.projectedState.hasKeyword(knight, Keyword.DOUBLE_STRIKE) shouldBe true

                game.execute(
                    ActivateAbility(game.player1Id, knight, FirebornKnight.activatedAbilities.first().id)
                ).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(knight) shouldBe 3
                game.state.projectedState.getToughness(knight) shouldBe 4
            }
        }
    }
}
