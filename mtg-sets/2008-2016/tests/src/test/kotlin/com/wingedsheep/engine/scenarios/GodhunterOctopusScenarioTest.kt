package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Godhunter Octopus (JOU #39): "This creature can't attack unless defending player controls an
 * enchantment or an enchanted permanent."
 *
 * The restriction reads the *defending player's* board, and its second branch is about the host,
 * not the Aura: a creature the defender controls counts as an enchanted permanent even when the
 * Aura on it is the attacker's. My own enchantments, and Auras on my own creatures, don't count.
 */
class GodhunterOctopusScenarioTest : ScenarioTestBase() {

    init {
        context("Godhunter Octopus — the attack restriction") {

            test("can't attack a defender with no enchantment and no enchanted permanent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Godhunter Octopus", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Godhunter Octopus" to 2)).error shouldNotBe null
            }

            test("the attacker's own enchantments and enchanted permanents don't count") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Godhunter Octopus", summoningSickness = false)
                    .withCardOnBattlefield(1, "Glorious Anthem")
                    .withCardAttachedTo(1, "Holy Strength", "Godhunter Octopus")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Godhunter Octopus" to 2)).error shouldNotBe null
            }

            test("can attack a defender who controls an enchantment") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Godhunter Octopus", summoningSickness = false)
                    .withCardOnBattlefield(2, "Glorious Anthem")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Godhunter Octopus" to 2)).error shouldBe null
            }

            test("can attack a defender whose creature carries the attacker's Aura") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Godhunter Octopus", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    // The Aura is mine, so the defender controls no enchantment — only the
                    // enchanted Grizzly Bears satisfies the restriction.
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Godhunter Octopus" to 2)).error shouldBe null
            }
        }
    }
}
