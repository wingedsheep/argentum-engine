package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.enduringstory.EnduringStoryService
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Fíli the Pathfinder — a conditional anthem plus "Whenever Fíli or another nontoken Dwarf you
 * control enters, create a 2/2 red Dwarf creature token."
 *
 * Two things worth pinning. The anthem says "creatures you control", not "other creatures", so Fíli
 * pumps himself — an `excludeSelf` slipped into the filter would be invisible except on his own
 * stats. And the trigger's `nontoken()` is load-bearing in the other direction: the tokens it makes
 * are themselves Dwarves you control, so without it every trigger would spawn a token that
 * re-triggered, and the ability would loop forever.
 */
class FiliThePathfinderScenarioTest : ScenarioTestBase() {

    init {

        test("without an enduring story the anthem is off") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Fíli the Pathfinder")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val fili = game.findPermanent("Fíli the Pathfinder")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            EnduringStoryService.has(game.state, game.player1Id) shouldBe false
            game.state.projectedState.getPower(fili) shouldBe 2
            game.state.projectedState.getToughness(fili) shouldBe 2
            game.state.projectedState.getPower(bears) shouldBe 2
        }

        test("with an enduring story every creature you control gets +1/+1 — Fíli included") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Fíli the Pathfinder")
                .withCardOnBattlefield(1, "Óin the Brave")
                .withCardOnBattlefield(1, "Thorin Oakenshield")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val fili = game.findPermanent("Fíli the Pathfinder")!!
            val myBears = game.findPermanent("Grizzly Bears")!!
            val theirGiant = game.findPermanent("Hill Giant")!!

            EnduringStoryService.has(game.state, game.player1Id) shouldBe true

            withClue("\"creatures you control\" has no excludeSelf — Fíli pumps himself") {
                game.state.projectedState.getPower(fili) shouldBe 3
                game.state.projectedState.getToughness(fili) shouldBe 3
            }
            game.state.projectedState.getPower(myBears) shouldBe 3
            withClue("the opponent's 3/3 Hill Giant is untouched — it would be 4/4 if it were pumped") {
                game.state.projectedState.getPower(theirGiant) shouldBe 3
                game.state.projectedState.getToughness(theirGiant) shouldBe 3
            }
        }

        test("another nontoken Dwarf entering makes a 2/2 red Dwarf token, and the token doesn't loop") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Fíli the Pathfinder")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInHand(1, "Dwarven Mauler")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Dwarven Mauler").error shouldBe null
            game.resolveStack()

            val tokens = game.findPermanents("Dwarf Token")
            withClue("exactly one token — the token is a Dwarf too, but nontoken() stops the loop") {
                tokens.size shouldBe 1
            }
            game.state.projectedState.getPower(tokens.first()) shouldBe 2
            game.state.projectedState.getToughness(tokens.first()) shouldBe 2
        }
    }
}
