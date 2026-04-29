package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Doran, Besieged by Time.
 *
 * Card reference:
 * - Doran, Besieged by Time ({1}{W}{B}{G}): 0/5 Legendary Creature — Treefolk Druid
 *   Each creature spell you cast with toughness greater than its power costs {1} less to cast.
 *   Whenever a creature you control attacks or blocks, it gets +X/+X until end of turn,
 *   where X is the difference between its power and toughness.
 */
class DoranBesiegedByTimeScenarioTest : ScenarioTestBase() {

    private val toughDefender = CardDefinition.creature(
        name = "Tough Defender",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype("Treefolk")),
        power = 1, toughness = 4
    )

    private val balancedBeast = CardDefinition.creature(
        name = "Balanced Beast",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 3, toughness = 3
    )

    private val highPowerBeast = CardDefinition.creature(
        name = "High Power Beast",
        manaCost = ManaCost.parse("{2}{R}"),
        subtypes = setOf(Subtype("Beast")),
        power = 5, toughness = 3
    )

    init {
        cardRegistry.register(toughDefender)
        cardRegistry.register(balancedBeast)
        cardRegistry.register(highPowerBeast)

        context("Doran, Besieged by Time cost reduction") {

            test("creature spell with toughness greater than power costs {1} less") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Doran, Besieged by Time")
                    .withCardInHand(1, "Tough Defender") // 1/4, normally costs {2}{G}
                    .withLandsOnBattlefield(1, "Forest", 2) // Only 2 mana — needs reduction
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Tough Defender")
                withClue("Cast should succeed with cost reduction: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()
                game.isOnBattlefield("Tough Defender") shouldBe true
            }

            test("creature spell with equal power and toughness does not cost less") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Doran, Besieged by Time")
                    .withCardInHand(1, "Balanced Beast") // 3/3, costs {2}{G}
                    .withLandsOnBattlefield(1, "Forest", 2) // Insufficient — needs full {2}{G}
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Balanced Beast")
                withClue("Cast should fail without enough mana — Doran does not reduce equal-stats creatures") {
                    castResult.error shouldNotBe null
                }
                game.isOnBattlefield("Balanced Beast") shouldBe false
            }

            test("creature spell with power greater than toughness does not cost less") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Doran, Besieged by Time")
                    .withCardInHand(1, "High Power Beast") // 5/3, costs {2}{R}
                    .withLandsOnBattlefield(1, "Mountain", 2) // Insufficient — needs full {2}{R}
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "High Power Beast")
                withClue("Cast should fail without enough mana — Doran does not reduce when power > toughness") {
                    castResult.error shouldNotBe null
                }
            }
        }

        context("Doran, Besieged by Time attack/block trigger") {

            test("attacking creature with toughness > power gets +X/+X for the difference") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Doran, Besieged by Time") // 0/5
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .build()

                game.declareAttackers(mapOf("Doran, Besieged by Time" to 2))
                game.resolveStack()

                val doran = game.findPermanent("Doran, Besieged by Time")!!
                val projected = game.state.projectedState
                withClue("Doran (0/5) attacking should be 5/10 — diff = 5") {
                    projected.getPower(doran) shouldBe 5
                    projected.getToughness(doran) shouldBe 10
                }
            }

            test("attacking creature with equal power and toughness gets +0/+0") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Doran, Besieged by Time")
                    .withCardOnBattlefield(1, "Balanced Beast") // 3/3
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .build()

                game.declareAttackers(mapOf("Balanced Beast" to 2))
                game.resolveStack()

                val beast = game.findPermanent("Balanced Beast")!!
                val projected = game.state.projectedState
                withClue("3/3 attacker stays 3/3 — diff is 0") {
                    projected.getPower(beast) shouldBe 3
                    projected.getToughness(beast) shouldBe 3
                }
            }

            test("attacking creature with power > toughness gets +X/+X using absolute difference") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Doran, Besieged by Time")
                    .withCardOnBattlefield(1, "High Power Beast") // 5/3
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .build()

                game.declareAttackers(mapOf("High Power Beast" to 2))
                game.resolveStack()

                val beast = game.findPermanent("High Power Beast")!!
                val projected = game.state.projectedState
                withClue("5/3 attacker should become 7/5 — diff is 2") {
                    projected.getPower(beast) shouldBe 7
                    projected.getToughness(beast) shouldBe 5
                }
            }

            test("blocking creature you control gets +X/+X") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Doran, Besieged by Time")
                    .withCardOnBattlefield(1, "Tough Defender") // 1/4
                    .withCardOnBattlefield(2, "Balanced Beast") // 3/3 attacker for opponent
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .build()

                game.declareAttackers(mapOf("Balanced Beast" to 1))
                // Opponent's attack does not trigger Doran (only "creature you control")
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                game.declareBlockers(mapOf("Tough Defender" to listOf("Balanced Beast")))
                game.resolveStack()

                val defender = game.findPermanent("Tough Defender")!!
                val projected = game.state.projectedState
                withClue("Tough Defender (1/4) blocking should be 4/7 — diff = 3") {
                    projected.getPower(defender) shouldBe 4
                    projected.getToughness(defender) shouldBe 7
                }
            }

            test("opponent's attacking creature is not buffed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Doran, Besieged by Time")
                    .withCardOnBattlefield(2, "Tough Defender") // opponent's 1/4
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .build()

                game.declareAttackers(mapOf("Tough Defender" to 1))
                game.resolveStack()

                val defender = game.findPermanent("Tough Defender")!!
                val projected = game.state.projectedState
                withClue("Opponent's Tough Defender (1/4) attacking should remain 1/4 — Doran's trigger is for creatures you control") {
                    projected.getPower(defender) shouldBe 1
                    projected.getToughness(defender) shouldBe 4
                }
            }
        }
    }
}
