package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Chrome Replicator (M21 #229) — "When this creature enters, if you control two or more nonland,
 * nontoken permanents with the same name as one another, create a 4/4 colorless Construct
 * artifact creature token."
 *
 * The intervening "if" counts only nonland, nontoken permanents grouped by name, so a pair of
 * lands or a card plus a token copy of it doesn't qualify.
 */
class ChromeReplicatorScenarioTest : ScenarioTestBase() {

    init {
        context("Chrome Replicator's enters trigger") {

            test("two nontoken permanents sharing a name make a Construct") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Chrome Replicator")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Chrome Replicator").error shouldBe null
                game.resolveStack()

                withClue("exactly one Construct token was created") {
                    game.findPermanents("Construct Token").size shouldBe 1
                }
            }

            test("a card and a token sharing its name don't count") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Chrome Replicator")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Chrome Replicator").error shouldBe null
                game.resolveStack()

                withClue("the token Bears isn't a nontoken namesake") {
                    game.findPermanents("Construct Token").size shouldBe 0
                }
            }

            test("same-named lands don't count") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Chrome Replicator")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Chrome Replicator").error shouldBe null
                game.resolveStack()

                withClue("five Islands share a name, but lands are excluded") {
                    game.findPermanents("Construct Token").size shouldBe 0
                }
            }
        }
    }
}
