package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Angelic Overseer (ISD #3) — {3}{W}{W} 5/3 Angel.
 *
 *   Flying
 *   As long as you control a Human, this creature has hexproof and indestructible.
 */
class AngelicOverseerScenarioTest : ScenarioTestBase() {

    init {
        test("controlling a Human grants hexproof and indestructible") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Angelic Overseer")
                .withCardOnBattlefield(1, "Elite Vanguard")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val angel = game.findPermanent("Angelic Overseer")!!
            game.state.projectedState.hasKeyword(angel, Keyword.FLYING) shouldBe true
            game.state.projectedState.hasKeyword(angel, Keyword.HEXPROOF) shouldBe true
            game.state.projectedState.hasKeyword(angel, Keyword.INDESTRUCTIBLE) shouldBe true
        }

        test("without a Human of yours it has neither — an opponent's Human doesn't count") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Angelic Overseer")
                .withCardOnBattlefield(2, "Elite Vanguard")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val angel = game.findPermanent("Angelic Overseer")!!
            game.state.projectedState.hasKeyword(angel, Keyword.FLYING) shouldBe true
            game.state.projectedState.hasKeyword(angel, Keyword.HEXPROOF) shouldBe false
            game.state.projectedState.hasKeyword(angel, Keyword.INDESTRUCTIBLE) shouldBe false
        }

        test("with a Human it survives a destroy effect") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Angelic Overseer")
                .withCardOnBattlefield(1, "Elite Vanguard")
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val angel = game.findPermanent("Angelic Overseer")!!
            // Hexproof only stops opponents, so its controller may still target it.
            val cast = game.castSpell(1, "Doom Blade", angel)
            withClue("Doom Blade should cast: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.isOnBattlefield("Angelic Overseer") shouldBe true
        }
    }
}
