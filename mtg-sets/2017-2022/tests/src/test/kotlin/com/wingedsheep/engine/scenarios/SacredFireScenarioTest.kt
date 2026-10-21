package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Sacred Fire (MID #239) — "Sacred Fire deals 2 damage to any target and you gain 2 life.
 * Flashback {4}{R}{W}"
 */
class SacredFireScenarioTest : ScenarioTestBase() {

    init {
        context("Sacred Fire") {

            test("deals 2 damage to a creature and its caster gains 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sacred Fire")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Sacred Fire", bears).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.getLifeTotal(1) shouldBe 22
            }

            test("flashback from the graveyard hits a player, gains 2 life, and exiles the card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Sacred Fire")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val fire = game.state.getGraveyard(game.player1Id).single()
                game.execute(
                    CastSpell(game.player1Id, fire, targets = listOf(ChosenTarget.Player(game.player2Id)))
                ).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18
                game.getLifeTotal(1) shouldBe 22
                game.isInExile(1, "Sacred Fire") shouldBe true
            }
        }
    }
}
