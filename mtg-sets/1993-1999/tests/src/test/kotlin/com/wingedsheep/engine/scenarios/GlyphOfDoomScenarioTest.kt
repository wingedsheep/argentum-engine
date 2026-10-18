package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Glyph of Doom (LEG #100) — "Choose target Wall creature. At this turn's next end of combat,
 * destroy all creatures that were blocked by that creature this turn."
 *
 * Player 2 attacks with Grizzly Bears and Hill Giant; player 1's Wall of Wood blocks the Bears and
 * Wall of Stone blocks the Giant, and every creature survives the damage. Only the creature the
 * targeted Wall blocked is destroyed — not the Wall itself, nor the creature another Wall blocked.
 */
class GlyphOfDoomScenarioTest : ScenarioTestBase() {

    init {
        context("Glyph of Doom") {

            fun setup(): TestGame {
                val builder = scenario()
                    .withPlayers("Wall Player", "Attacker")
                    .withCardOnBattlefield(1, "Wall of Wood")
                    .withCardOnBattlefield(1, "Wall of Stone")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInHand(1, "Glyph of Doom")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(10) {
                    builder.withCardInLibrary(1, "Swamp")
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
                castSpell(1, "Glyph of Doom", findPermanent(wallName)!!).error shouldBe null
                resolveStack()
            }

            test("destroys the creature the targeted Wall blocked, at end of combat") {
                val game = setup()
                game.attackAndBlock()
                game.castGlyphOn("Wall of Wood")

                withClue("nothing is destroyed before end of combat") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                withClue("the creature a different Wall blocked survives") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
                withClue("the Wall itself was never blocked by itself") {
                    game.isOnBattlefield("Wall of Wood") shouldBe true
                }
            }

            test("cast before blockers, the creature the Wall blocks afterwards is destroyed") {
                val game = setup()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1, "Hill Giant" to 1)).error shouldBe null
                game.castGlyphOn("Wall of Stone")
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(
                    mapOf("Wall of Wood" to listOf("Grizzly Bears"), "Wall of Stone" to listOf("Hill Giant"))
                ).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("cast after the turn's last combat, there is no next end of combat and nothing dies") {
                val game = setup()
                game.attackAndBlock()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.castGlyphOn("Wall of Wood")
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.state.delayedTriggers.size shouldBe 0
            }

            test("can only target a Wall") {
                val game = setup()
                game.attackAndBlock()
                if (game.state.priorityPlayerId == game.player2Id) game.passPriority()
                game.castSpell(1, "Glyph of Doom", game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
            }
        }
    }
}
