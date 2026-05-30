package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain as stringShouldContain

/**
 * Scenario tests for the Flurry keyword (Tarkir: Dragonstorm, Jeskai).
 *
 * "Flurry — Whenever you cast your second spell each turn, [effect]." The `flurry { }`
 * DSL helper wires a triggered ability on the second-spell-cast event (n=2, you) plus a
 * display-only [Keyword.FLURRY] tag. These tests exercise the trigger timing on
 * Cori Mountain Stalwart, whose Flurry effect ("deal 2 damage to each opponent and gain
 * 2 life") needs no player decisions, keeping the life accounting unambiguous.
 */
class FlurryScenarioTest : ScenarioTestBase() {

    init {
        context("Flurry — second-spell trigger") {

            test("casting only the first spell does not trigger Flurry") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cori Mountain Stalwart")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shock = game.castSpellTargetingPlayer(1, "Shock", 2)
                withClue("Shock should cast: ${shock.error}") { shock.error shouldBe null }
                game.resolveStack()

                withClue("Only Shock resolved (2 damage) — Flurry must not have fired") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("Flurry's life gain must not have happened on the first spell") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("casting a second spell triggers Flurry (2 damage to each opponent + gain 2 life)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cori Mountain Stalwart")
                    .withCardInHand(1, "Shock")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // First spell of the turn — no Flurry yet.
                val first = game.castSpellTargetingPlayer(1, "Shock", 2)
                withClue("First Shock should cast: ${first.error}") { first.error shouldBe null }
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 18
                game.getLifeTotal(1) shouldBe 20

                // Second spell — Flurry fires on the cast, resolving above the spell.
                val second = game.castSpellTargetingPlayer(1, "Shock", 2)
                withClue("Second Shock should cast: ${second.error}") { second.error shouldBe null }
                game.resolveStack()

                withClue("Opponent: 18 − 2 (Flurry) − 2 (second Shock) = 14") {
                    game.getLifeTotal(2) shouldBe 14
                }
                withClue("Controller gained 2 from Flurry — only Flurry gains life here") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }
        }

        context("Flurry — keyword + reminder text wiring") {

            test("flurry helper tags FLURRY and prefixes the second-spell reminder text") {
                val card = cardRegistry.getCard("Devoted Duelist")
                    ?: error("Devoted Duelist not registered")

                withClue("flurry { } adds the display-only FLURRY keyword") {
                    card.keywords shouldContain Keyword.FLURRY
                }

                val flurryAbility = card.script.triggeredAbilities.single { ability ->
                    ability.description.startsWith("Flurry —")
                }
                flurryAbility.description stringShouldContain
                    "Flurry — Whenever you cast your second spell each turn,"
            }
        }
    }
}
