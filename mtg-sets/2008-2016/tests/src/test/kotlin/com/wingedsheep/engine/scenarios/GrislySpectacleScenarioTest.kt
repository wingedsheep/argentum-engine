package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Grisly Spectacle: "Destroy target nonartifact creature. Its controller mills cards equal to that
 * creature's power." The count is the creature's power as it last existed on the battlefield
 * (ruling 2017-11-17) — an anthem's bonus counts even though the card is in the graveyard by the
 * time the spell finishes resolving.
 */
class GrislySpectacleScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Grisly Spectacle")
        .withLandsOnBattlefield(1, "Swamp", 4)
        .withCardOnBattlefield(2, "Hill Giant") // 3/3, 4/4 under the anthem
        .withCardOnBattlefield(2, "Glorious Anthem")
        .withCardOnBattlefield(2, "Ornithopter")
        .apply { repeat(6) { withCardInLibrary(2, "Grizzly Bears") } }
        .withCardInLibrary(1, "Grizzly Bears")
        .withActivePlayer(1)
        .build()

    init {
        test("destroys the creature and its controller mills its last-known power") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Grisly Spectacle", giant).error shouldBe null
            game.resolveStack()

            withClue("Hill Giant is destroyed") {
                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
            withClue("its controller milled 4 — the anthem-pumped power, not the printed 3") {
                game.librarySize(2) shouldBe 2
            }
            withClue("the caster mills nothing") {
                game.librarySize(1) shouldBe 1
            }
        }

        test("can't target an artifact creature") {
            val game = board()
            val thopter = game.findPermanent("Ornithopter")!!
            game.castSpell(1, "Grisly Spectacle", thopter).error shouldNotBe null
        }
    }
}
