package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Celestial Messenger (M20 #287) — {2}{U}{U} Creature — Bird Spirit 3/2, flash, flying.
 *
 *   This creature gets +1/+1 as long as you control a Yanling planeswalker.
 */
class CelestialMessengerScenarioTest : ScenarioTestBase() {

    private val yanling = card("Test Yanling") {
        manaCost = "{3}{U}"
        typeLine = "Legendary Planeswalker — Yanling"
        startingLoyalty = 3
        oracleText = ""
    }

    private val otherWalker = card("Test Other Walker") {
        manaCost = "{3}{U}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }

    init {
        cardRegistry.register(yanling)
        cardRegistry.register(otherWalker)

        context("Celestial Messenger") {

            test("is a 4/3 flier while you control a Yanling planeswalker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Celestial Messenger")
                    .withCardOnBattlefield(1, "Test Yanling")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val messenger = game.findPermanent("Celestial Messenger")!!
                val projected = game.state.projectedState
                projected.getPower(messenger) shouldBe 4
                projected.getToughness(messenger) shouldBe 3
                projected.hasKeyword(messenger, Keyword.FLYING) shouldBe true
            }

            test("stays 3/2 with a non-Yanling planeswalker or an opponent's Yanling") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Celestial Messenger")
                    .withCardOnBattlefield(1, "Test Other Walker")
                    .withCardOnBattlefield(2, "Test Yanling")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val messenger = game.findPermanent("Celestial Messenger")!!
                val projected = game.state.projectedState
                withClue("no bonus without a Yanling planeswalker under your control") {
                    projected.getPower(messenger) shouldBe 3
                    projected.getToughness(messenger) shouldBe 2
                }
            }
        }
    }
}
