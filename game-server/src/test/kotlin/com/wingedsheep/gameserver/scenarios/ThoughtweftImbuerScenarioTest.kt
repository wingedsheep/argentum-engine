package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Thoughtweft Imbuer.
 *
 * {3}{W}, 0/5 Creature — Kithkin Advisor.
 * Whenever a creature you control attacks alone, it gets +X/+X until end of turn,
 * where X is the number of Kithkin you control.
 */
class ThoughtweftImbuerScenarioTest : ScenarioTestBase() {

    init {
        context("Thoughtweft Imbuer attack-alone trigger") {

            test("lone attacker gets +X/+X for each Kithkin you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thoughtweft Imbuer")     // Kithkin Advisor (0/5)
                    .withCardOnBattlefield(1, "Goldmeadow Nomad")        // Kithkin Scout (1/2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Goldmeadow Nomad" to 2))
                game.resolveStack()

                val nomad = game.findPermanent("Goldmeadow Nomad")!!
                val projected = game.state.projectedState
                withClue("Goldmeadow Nomad (1/2) attacking alone with 2 Kithkin out should be 3/4") {
                    projected.getPower(nomad) shouldBe 3
                    projected.getToughness(nomad) shouldBe 4
                }
            }

            test("non-Kithkin lone attacker still gets buffed by the Imbuer's own Kithkin count") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thoughtweft Imbuer")     // Kithkin Advisor (0/5)
                    .withCardOnBattlefield(1, "Llanowar Elves")          // Elf Druid (1/1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Llanowar Elves" to 2))
                game.resolveStack()

                val elves = game.findPermanent("Llanowar Elves")!!
                val projected = game.state.projectedState
                withClue("Llanowar Elves (1/1) attacking alone with 1 Kithkin (the Imbuer) should be 2/2") {
                    projected.getPower(elves) shouldBe 2
                    projected.getToughness(elves) shouldBe 2
                }
            }

            test("attacking with multiple creatures does not trigger the ability") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thoughtweft Imbuer")
                    .withCardOnBattlefield(1, "Goldmeadow Nomad")
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf(
                    "Goldmeadow Nomad" to 2,
                    "Llanowar Elves" to 2
                ))
                game.resolveStack()

                val nomad = game.findPermanent("Goldmeadow Nomad")!!
                val elves = game.findPermanent("Llanowar Elves")!!
                val projected = game.state.projectedState
                withClue("Two attackers — neither attacks alone, so no buff applies") {
                    projected.getPower(nomad) shouldBe 1
                    projected.getToughness(nomad) shouldBe 2
                    projected.getPower(elves) shouldBe 1
                    projected.getToughness(elves) shouldBe 1
                }
            }

            test("opponent's lone attacker is not buffed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Thoughtweft Imbuer")     // controlled by P1
                    .withCardOnBattlefield(2, "Goldmeadow Nomad")        // attacker controlled by P2
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Goldmeadow Nomad" to 1))
                game.resolveStack()

                val nomad = game.findPermanent("Goldmeadow Nomad")!!
                val projected = game.state.projectedState
                withClue("Opponent's Goldmeadow Nomad attacking alone should remain 1/2") {
                    projected.getPower(nomad) shouldBe 1
                    projected.getToughness(nomad) shouldBe 2
                }
            }
        }
    }
}
