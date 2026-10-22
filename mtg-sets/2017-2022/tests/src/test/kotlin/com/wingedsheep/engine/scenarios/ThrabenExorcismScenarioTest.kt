package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Thraben Exorcism (MID #39) — {1}{W} Instant.
 *
 *   Exile target Spirit, creature with disturb, or enchantment.
 *
 * The interesting branch is "creature with disturb": a non-Spirit front face (Baithook Angler,
 * a Human Peasant) is only a legal target because the DISTURB keyword derived from its
 * `disturb(...)` ability reaches the battlefield permanent's projected keywords.
 */
class ThrabenExorcismScenarioTest : ScenarioTestBase() {

    private fun board(targetName: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Thraben Exorcism")
        .withCardOnBattlefield(2, targetName)
        .withLandsOnBattlefield(1, "Plains", 2)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Thraben Exorcism") {

            listOf("Baithook Angler", "Phantom Carriage", "Glorious Anthem").forEach { name ->
                test("exiles $name") {
                    val game = board(name)
                    val cast = game.castSpell(1, "Thraben Exorcism", targetId = game.findPermanent(name)!!)
                    withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                    game.resolveStack()

                    game.isOnBattlefield(name) shouldBe false
                    game.isInExile(2, name) shouldBe true
                }
            }

            test("can't target a creature that is neither a Spirit nor has disturb") {
                val game = board("Grizzly Bears")
                val cast = game.castSpell(1, "Thraben Exorcism", targetId = game.findPermanent("Grizzly Bears")!!)
                withClue("Grizzly Bears is not a legal target") { cast.error shouldNotBe null }
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
