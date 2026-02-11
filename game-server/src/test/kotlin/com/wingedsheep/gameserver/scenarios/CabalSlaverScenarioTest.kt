package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Cabal Slaver.
 *
 * Card reference:
 * - Cabal Slaver ({2}{B}): Creature — Human Cleric, 2/1
 *   "Whenever a Goblin deals combat damage to a player, that player discards a card."
 */
class CabalSlaverScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.chooseCreatureType(typeName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val options = (decision as ChooseOptionDecision).options
        val index = options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options $options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        context("Cabal Slaver combat damage trigger") {

            test("opponent discards when a Goblin you control deals combat damage") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Cabal Slaver")
                    .withCardOnBattlefield(1, "Goblin Sledder")  // 1/1 Goblin
                    .withCardInHand(2, "Forest")  // Only card - auto-discard
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Attack with Goblin Sledder (not Cabal Slaver)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Goblin Sledder" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Combat damage + trigger resolution
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Opponent should have discarded their card") {
                    game.handSize(2) shouldBe 0
                }
                withClue("Opponent should have taken 1 combat damage") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }

            test("no trigger when a non-Goblin creature deals combat damage") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Cabal Slaver")
                    .withCardOnBattlefield(1, "Glory Seeker")  // 2/2 Human Soldier (not Goblin)
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Attack with Glory Seeker
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Opponent should NOT have discarded (not a Goblin)") {
                    game.handSize(2) shouldBe 1
                }
                withClue("Opponent should have taken 2 combat damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("multiple Goblins trigger multiple discards") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Cabal Slaver")
                    .withCardOnBattlefield(1, "Goblin Sledder")     // 1/1 Goblin
                    .withCardOnBattlefield(1, "Festering Goblin")   // 1/1 Goblin
                    .withCardInHand(2, "Forest")
                    .withCardInHand(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Attack with both Goblins
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf(
                    "Goblin Sledder" to 2,
                    "Festering Goblin" to 2
                ))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Both triggers fire; with 2 cards in hand, opponent needs to choose each time
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Opponent should have discarded 2 cards (one per Goblin)") {
                    game.handSize(2) shouldBe 0
                }
                withClue("Opponent should have taken 2 combat damage total") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("Cabal Slaver itself does not trigger (it's not a Goblin)") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Cabal Slaver")  // Human Cleric, not Goblin
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Attack with Cabal Slaver itself
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Cabal Slaver" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Opponent should NOT have discarded (Cabal Slaver is not a Goblin)") {
                    game.handSize(2) shouldBe 1
                }
                withClue("Opponent should have taken 2 combat damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("Goblin blocked by larger creature does not trigger") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Cabal Slaver")
                    .withCardOnBattlefield(1, "Goblin Sledder")  // 1/1 Goblin
                    .withCardOnBattlefield(2, "Glory Seeker")    // 2/2 blocker
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Goblin Sledder" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Glory Seeker" to listOf("Goblin Sledder")))

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Opponent should NOT have discarded (Goblin was blocked)") {
                    game.handSize(2) shouldBe 1
                }
                withClue("Opponent life should be unchanged") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }

        context("Cabal Slaver with Artificial Evolution") {

            test("Artificial Evolution changes Goblin to Elf - Elves trigger discard") {
                val game = scenario()
                    .withPlayers("Blue Black Mage", "Defender")
                    .withCardOnBattlefield(1, "Cabal Slaver")
                    .withCardOnBattlefield(1, "Elvish Warrior")  // 2/3 Elf
                    .withCardInHand(1, "Artificial Evolution")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(2, "Forest")  // Only card - auto-discard
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Artificial Evolution targeting Cabal Slaver
                val cabalSlaver = game.findPermanent("Cabal Slaver")!!
                game.castSpell(1, "Artificial Evolution", cabalSlaver)
                game.resolveStack()

                // Choose FROM: Goblin → TO: Elf
                game.chooseCreatureType("Goblin")
                game.chooseCreatureType("Elf")

                // Now attack with Elvish Warrior - should trigger discard
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Elvish Warrior" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Opponent should have discarded (Elf triggers after Evolution)") {
                    game.handSize(2) shouldBe 0
                }
                withClue("Opponent should have taken 2 combat damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }

            test("Artificial Evolution changes Goblin to Elf - Goblins no longer trigger") {
                val game = scenario()
                    .withPlayers("Blue Black Mage", "Defender")
                    .withCardOnBattlefield(1, "Cabal Slaver")
                    .withCardOnBattlefield(1, "Goblin Sledder")  // 1/1 Goblin
                    .withCardInHand(1, "Artificial Evolution")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Artificial Evolution on Cabal Slaver, changing Goblin to Elf
                val cabalSlaver = game.findPermanent("Cabal Slaver")!!
                game.castSpell(1, "Artificial Evolution", cabalSlaver)
                game.resolveStack()

                game.chooseCreatureType("Goblin")
                game.chooseCreatureType("Elf")

                // Attack with Goblin Sledder - should NOT trigger (now watches for Elves)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Goblin Sledder" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Opponent should NOT have discarded (Goblin no longer triggers)") {
                    game.handSize(2) shouldBe 1
                }
            }
        }
    }
}
