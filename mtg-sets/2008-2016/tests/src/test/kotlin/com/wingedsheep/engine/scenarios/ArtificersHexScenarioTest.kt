package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Artificer's Hex — {B} Enchantment — Aura (M14 #85).
 *
 * "Enchant Equipment. At the beginning of your upkeep, if enchanted Equipment is attached to a
 * creature, destroy that creature."
 *
 * "That creature" is the host of the Aura's host — reached through `StatePredicate.HasAttached`.
 */
class ArtificersHexScenarioTest : ScenarioTestBase() {

    /** Hex (player 1's) on Bonesplitter; [equipped] says whether the Bonesplitter is on the Bears. */
    private fun hexOnBonesplitter(equipped: Boolean, hexActivePlayer: Boolean = true): TestGame {
        val builder = scenario()
            .withPlayers("Hex", "Opponent")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
        if (equipped) builder.withCardAttachedTo(2, "Bonesplitter", "Grizzly Bears")
        else builder.withCardOnBattlefield(2, "Bonesplitter")
        val game = builder
            .withCardAttachedTo(1, "Artificer's Hex", "Bonesplitter")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Forest")
            // Start in the other player's turn, so the next upkeep is the one under test.
            .withActivePlayer(if (hexActivePlayer) 2 else 1)
            .withPriorityPlayer(if (hexActivePlayer) 2 else 1)
            .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            .build()
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        return game
    }

    init {
        test("your upkeep destroys the creature the enchanted Equipment is attached to") {
            val game = hexOnBonesplitter(equipped = true)

            withClue("the equipped creature is destroyed") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }
            withClue("the Equipment, the Hex and the unequipped creature stay") {
                game.isOnBattlefield("Bonesplitter") shouldBe true
                game.isOnBattlefield("Artificer's Hex") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }

        test("nothing happens while the enchanted Equipment is unattached") {
            val game = hexOnBonesplitter(equipped = false)

            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
        }

        test("it does not trigger in the opponent's upkeep") {
            val game = hexOnBonesplitter(equipped = true, hexActivePlayer = false)

            game.state.activePlayerId shouldBe game.player2Id
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
