package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Glyph of Life (LEG #15) — "Choose target Wall creature. Whenever that creature is dealt damage by
 * an attacking creature this turn, you gain that much life."
 *
 * Player 2 attacks with Grizzly Bears (2 power) and Hill Giant (3 power); player 1's Wall of Wood
 * blocks the Bears and Wall of Stone blocks the Giant. Only damage dealt to the targeted Wall gains
 * life, and the amount is the damage that Wall was dealt.
 */
class GlyphOfLifeScenarioTest : ScenarioTestBase() {

    init {
        context("Glyph of Life") {

            fun setup(): TestGame {
                val builder = scenario()
                    .withPlayers("Wall Player", "Attacker")
                    .withCardOnBattlefield(1, "Wall of Wood")
                    .withCardOnBattlefield(1, "Wall of Stone")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInHand(1, "Glyph of Life")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(10) {
                    builder.withCardInLibrary(1, "Plains")
                    builder.withCardInLibrary(2, "Mountain")
                }
                return builder.build()
            }

            fun TestGame.attackAndBlock() {
                passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                declareAttackers(mapOf("Grizzly Bears" to 1, "Hill Giant" to 1)).error shouldBe null
                passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                declareBlockers(
                    mapOf("Wall of Wood" to listOf("Grizzly Bears"), "Wall of Stone" to listOf("Hill Giant"))
                ).error shouldBe null
            }

            fun TestGame.castGlyphOn(wallName: String) {
                // The attacking player holds priority first; hand it to the Wall player.
                if (state.priorityPlayerId == player2Id) passPriority()
                castSpell(1, "Glyph of Life", findPermanent(wallName)!!).error shouldBe null
                resolveStack()
            }

            test("gains life equal to the damage the attacker deals to the targeted Wall") {
                val game = setup()
                game.attackAndBlock()
                game.castGlyphOn("Wall of Wood")
                withClue("no life is gained before damage") { game.getLifeTotal(1) shouldBe 20 }

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Grizzly Bears dealt 2 to Wall of Wood; Hill Giant's 3 to Wall of Stone doesn't count") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }

            test("targeting the other Wall reads that Wall's damage instead") {
                val game = setup()
                game.attackAndBlock()
                game.castGlyphOn("Wall of Stone")
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.getLifeTotal(1) shouldBe 23
            }

            test("can only target a Wall") {
                val game = setup()
                game.attackAndBlock()
                if (game.state.priorityPlayerId == game.player2Id) game.passPriority()
                game.castSpell(1, "Glyph of Life", game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
            }
        }
    }
}
