package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Glaring Fleshraker (MH3) — "Whenever you cast a colorless spell, create a 0/1 colorless Eldrazi
 * Spawn creature token…" and "Whenever another colorless creature you control enters, this
 * creature deals 1 damage to each opponent."
 */
class GlaringFleshrakerScenarioTest : ScenarioTestBase() {

    init {
        context("Glaring Fleshraker") {
            test("casting a colorless creature makes a Spawn; both entering pings the opponent") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Glaring Fleshraker")
                    .withCardInHand(1, "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Ornithopter").error shouldBe null
                game.resolveStack()

                withClue("One Eldrazi Spawn from the cast trigger") {
                    game.findPermanents("Eldrazi Spawn") shouldHaveSize 1
                }
                game.findPermanent("Ornithopter") shouldNotBe null
                withClue("Spawn and Ornithopter each entered: 2 damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
                game.getLifeTotal(1) shouldBe 20
            }

            test("a colored spell and a colored creature trigger nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Glaring Fleshraker")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                game.findPermanent("Grizzly Bears") shouldNotBe null
                game.findPermanents("Eldrazi Spawn") shouldHaveSize 0
                game.getLifeTotal(2) shouldBe 20
            }

            test("its own entry does not trigger it — the creature must be another") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Glaring Fleshraker")
                    .withLandsOnBattlefield(1, "Rogue's Passage", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Glaring Fleshraker").error shouldBe null
                game.resolveStack()

                game.findPermanent("Glaring Fleshraker") shouldNotBe null
                game.findPermanents("Eldrazi Spawn") shouldHaveSize 0
                game.getLifeTotal(2) shouldBe 20
            }

            test("an opponent's colorless creature entering does not trigger") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Glaring Fleshraker")
                    .withCardInHand(2, "Ornithopter")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Ornithopter").error shouldBe null
                game.resolveStack()

                game.findPermanents("Eldrazi Spawn") shouldHaveSize 0
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }
        }
    }
}
