package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.daynight.DayNightService
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.DayNight
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Baneblade Scoundrel // Baneclaw Marauder (MID #85).
 *
 * Front (4/3, daybound): whenever it becomes blocked, each creature blocking it gets -1/-1 until end
 * of turn. Back (5/4, nightbound): the same, plus "whenever a creature blocking this creature dies,
 * that creature's controller loses 1 life" — gated on the dead blocker's last-known combat pairing.
 */
class BanebladeScoundrelScenarioTest : ScenarioTestBase() {

    private fun nightGame(vararg p2Creatures: String, p1Extra: String? = null): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Baneblade Scoundrel")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        p1Extra?.let { builder.withCardOnBattlefield(1, it) }
        p2Creatures.forEach { builder.withCardOnBattlefield(2, it) }
        val game = builder.build()
        game.state = DayNightService.set(game.state.copy(dayNight = DayNight.DAY), cardRegistry, DayNight.NIGHT, "test").first
        withClue("night transforms it into Baneclaw Marauder") {
            game.findPermanent("Baneclaw Marauder") shouldNotBe null
        }
        return game
    }

    init {
        context("Baneblade Scoundrel — day face") {
            test("each blocker gets -1/-1; a blocker it kills costs nobody life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Baneblade Scoundrel")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.state = game.state.copy(dayNight = DayNight.DAY)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Baneblade Scoundrel" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(
                    mapOf("Hill Giant" to listOf("Baneblade Scoundrel"), "Savannah Lions" to listOf("Baneblade Scoundrel"))
                ).error shouldBe null
                game.resolveStack()

                withClue("Savannah Lions (2/1) dies to the -1/-1") { game.isInGraveyard(2, "Savannah Lions") shouldBe true }
                val giant = game.findPermanent("Hill Giant")!!
                withClue("Hill Giant is 2/2") {
                    game.state.projectedState.getPower(giant) shouldBe 2
                    game.state.projectedState.getToughness(giant) shouldBe 2
                }
                withClue("the day face has no death trigger") { game.getLifeTotal(2) shouldBe 20 }
            }
        }

        context("Baneclaw Marauder — night face") {
            test("a blocker killed by the -1/-1 makes its controller lose 1 life") {
                val game = nightGame("Savannah Lions")

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Baneclaw Marauder" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Savannah Lions" to listOf("Baneclaw Marauder"))).error shouldBe null
                game.resolveStack()

                withClue("the Lions died blocking") { game.isInGraveyard(2, "Savannah Lions") shouldBe true }
                withClue("its controller loses 1") { game.getLifeTotal(2) shouldBe 19 }
            }

            test("a blocker killed by combat damage makes its controller lose 1 life") {
                val game = nightGame("Hill Giant")

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Baneclaw Marauder" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Baneclaw Marauder"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("the 2/2 Giant took 5 and died") { game.isInGraveyard(2, "Hill Giant") shouldBe true }
                withClue("the Marauder (5/4) survives 2 damage") { game.findPermanent("Baneclaw Marauder") shouldNotBe null }
                withClue("its controller loses 1") { game.getLifeTotal(2) shouldBe 19 }
            }

            test("the Marauder and its blocker killing each other still costs a life") {
                val game = nightGame("Craw Wurm")

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Baneclaw Marauder" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Craw Wurm" to listOf("Baneclaw Marauder"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("the shrunken 5/3 Wurm and the 5/4 Marauder trade") {
                    game.isInGraveyard(2, "Craw Wurm") shouldBe true
                    game.findPermanent("Baneclaw Marauder") shouldBe null
                }
                withClue("the Marauder sees its blocker die (leaves-the-battlefield look-back)") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }

            test("a creature dying while blocking a different attacker doesn't trigger it") {
                val game = nightGame("Savannah Lions", p1Extra = "Hill Giant")

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Baneclaw Marauder" to 2, "Hill Giant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Savannah Lions" to listOf("Hill Giant"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("the Lions died blocking the Giant") { game.isInGraveyard(2, "Savannah Lions") shouldBe true }
                withClue("only the unblocked Marauder's 5 damage — no extra life loss") {
                    game.getLifeTotal(2) shouldBe 15
                }
            }
        }
    }
}
