package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Kraul Whipcracker (MKM) — "Reach. When this creature enters, destroy target token an opponent
 * controls."
 *
 * "Token" is not a card type, so the filter is a token predicate rather than a type predicate: any
 * token permanent qualifies, and a nontoken creature never does. The target is mandatory, so with
 * no opposing token the trigger simply has no legal target and is removed from the stack — the
 * Whipcracker still enters.
 */
class KraulWhipcrackerScenarioTest : ScenarioTestBase() {

    init {
        context("Kraul Whipcracker — a token-only removal trigger") {

            test("destroys an opponent's creature token on entry") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Kraul Whipcracker")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears", isToken = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val token = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Kraul Whipcracker").error shouldBe null
                game.resolveStack()
                if (game.getPendingDecision() is ChooseTargetsDecision) {
                    game.selectTargets(listOf(token)).error shouldBe null
                    game.resolveStack()
                }

                withClue("the only legal token is destroyed and the Whipcracker sticks around") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isOnBattlefield("Kraul Whipcracker") shouldBe true
                }
            }

            test("a nontoken creature is not a legal target, so the trigger does nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Kraul Whipcracker")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears") // a real card, not a token
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Kraul Whipcracker").error shouldBe null
                game.resolveStack()

                withClue("Kraul Whipcracker still enters; the Bears is untouched") {
                    game.isOnBattlefield("Kraul Whipcracker") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("your own token is not a legal target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Kraul Whipcracker")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Kraul Whipcracker").error shouldBe null
                game.resolveStack()

                withClue("'an opponent controls' excludes your own tokens") {
                    game.isOnBattlefield("Kraul Whipcracker") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("enters as a 3/2 with reach") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Kraul Whipcracker")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Kraul Whipcracker").error shouldBe null
                game.resolveStack()

                val id = game.findPermanent("Kraul Whipcracker")!!
                val projected = StateProjector().project(game.state)
                projected.getPower(id) shouldBe 3
                projected.getToughness(id) shouldBe 2
                projected.hasKeyword(id, Keyword.REACH) shouldBe true
            }
        }
    }
}
