package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Peerless Samurai (NEO #156) — "Whenever a Samurai or Warrior you control attacks alone, the next
 * spell you cast this turn costs {1} less to cast."
 *
 * One Forest pays for a postcombat Grizzly Bears ({1}{G}) only with the discount; the second Bears
 * finds it already spent.
 */
class PeerlessSamuraiScenarioTest : ScenarioTestBase() {

    init {
        context("Peerless Samurai") {

            test("attacking alone discounts only the next spell cast this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Peerless Samurai")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Peerless Samurai" to 2)).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.state.spellCostModifiers.single().nextSpellOnly shouldBe true

                withClue("{1}{G} costs {G}") {
                    game.castSpell(1, "Grizzly Bears").error shouldBe null
                }
                game.resolveStack()
                withClue("the first spell spent the discount") {
                    game.state.spellCostModifiers shouldBe emptyList()
                }
            }

            test("a Warrior you control attacking alone triggers it too") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Peerless Samurai")
                    .withCardOnBattlefield(1, "Centaur Courser") // Centaur Warrior
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Centaur Courser" to 2)).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.state.spellCostModifiers.size shouldBe 1
            }

            test("it doesn't trigger when the Samurai attacks with another creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Peerless Samurai")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Peerless Samurai" to 2, "Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.state.spellCostModifiers shouldBe emptyList()
            }

            test("a non-Samurai, non-Warrior attacking alone doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Peerless Samurai")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.state.spellCostModifiers shouldBe emptyList()
            }
        }
    }
}
