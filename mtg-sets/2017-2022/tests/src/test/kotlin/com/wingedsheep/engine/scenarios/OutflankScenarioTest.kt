package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Outflank (ELD #23) — {W} Instant.
 *
 * "Outflank deals damage to target attacking or blocking creature equal to the number of
 * creatures you control."
 *
 * Pins the count to creatures *you* control (the opponent's creatures don't add to it) and the
 * target to a creature in combat.
 */
class OutflankScenarioTest : ScenarioTestBase() {

    init {
        context("Outflank") {

            fun blockedByWurm(myExtraCreatures: List<String>) = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Outflank")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .apply { myExtraCreatures.forEach { withCardOnBattlefield(1, it) } }
                .withCardOnBattlefield(2, "Craw Wurm") // 6/4 blocker
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
                .also { game ->
                    game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                    game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                    game.declareBlockers(mapOf("Craw Wurm" to listOf("Grizzly Bears"))).error shouldBe null
                    game.passPriority() // blocking player passes; active player gets priority
                }

            test("with four creatures it deals 4 damage and kills a 6/4 blocker") {
                val game = blockedByWurm(listOf("Savannah Lions", "Savannah Lions", "Hill Giant"))
                val wurm = game.findPermanent("Craw Wurm")!!

                game.castSpell(1, "Outflank", wurm).error shouldBe null
                game.resolveStack()

                withClue("4 damage to a toughness-4 Craw Wurm is lethal") {
                    game.isInGraveyard(2, "Craw Wurm") shouldBe true
                }
            }

            test("counts only creatures you control — three of yours is not lethal to a 6/4") {
                // Player controls 3 creatures; the opponent controls 3 more. Counting every creature
                // would deal 6 and kill the Wurm; counting only yours deals 3.
                val game = blockedByWurm(listOf("Savannah Lions", "Hill Giant"))
                val wurm = game.findPermanent("Craw Wurm")!!

                game.castSpell(1, "Outflank", wurm).error shouldBe null
                game.resolveStack()

                withClue("3 damage leaves the Craw Wurm alive") {
                    game.isOnBattlefield("Craw Wurm") shouldBe true
                }
            }

            test("cannot target a creature that is not attacking or blocking") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Outflank")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lions = game.findPermanent("Savannah Lions")!!
                game.castSpell(1, "Outflank", lions).error shouldNotBe null
            }
        }
    }
}
