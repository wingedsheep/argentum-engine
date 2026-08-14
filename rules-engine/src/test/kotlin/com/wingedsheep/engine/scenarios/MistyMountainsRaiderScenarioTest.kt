package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Misty Mountains Raider (HOB) — {4}{R} Creature — Goblin Soldier 4/4.
 *
 * "Whenever you attack, amass Goblins 2."
 *
 * "Whenever you attack" is the once-per-declare-attackers trigger: it fires exactly once no matter
 * how many creatures were declared, and it fires even when the Raider itself stays home.
 */
class MistyMountainsRaiderScenarioTest : ScenarioTestBase() {

    init {
        context("Misty Mountains Raider") {

            test("it is a 4/4") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Misty Mountains Raider")
                    .build()

                val raider = game.findPermanent("Misty Mountains Raider")!!
                game.state.projectedState.getPower(raider) shouldBe 4
                game.state.projectedState.getToughness(raider) shouldBe 4
            }

            test("attacking amasses Goblins 2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Misty Mountains Raider", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Misty Mountains Raider" to 2)).error shouldBe null
                game.resolveStack()

                val army = game.findPermanent("Goblin Army")
                    ?: error("attacking should have amassed a Goblin Army")
                withClue("two +1/+1 counters on a fresh 0/0 Army") {
                    game.state.projectedState.getPower(army) shouldBe 2
                    game.state.projectedState.getToughness(army) shouldBe 2
                }
            }

            test("it fires once for the whole attack, not once per attacker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Misty Mountains Raider", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf(
                        "Misty Mountains Raider" to 2,
                        "Grizzly Bears" to 2,
                        "Hill Giant" to 2,
                    )
                ).error shouldBe null
                game.resolveStack()

                val army = game.findPermanent("Goblin Army")!!
                withClue("three attackers, still a single 'whenever you attack' trigger") {
                    game.findAllPermanents("Goblin Army").size shouldBe 1
                    game.state.projectedState.getPower(army) shouldBe 2
                }
            }

            test("it triggers even when the Raider itself doesn't attack") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Misty Mountains Raider", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()

                withClue("'whenever you attack' is a player trigger, not a self-attack one") {
                    val army = game.findPermanent("Goblin Army")
                        ?: error("the Raider should still have amassed")
                    game.state.projectedState.getPower(army) shouldBe 2
                }
            }
        }
    }
}
