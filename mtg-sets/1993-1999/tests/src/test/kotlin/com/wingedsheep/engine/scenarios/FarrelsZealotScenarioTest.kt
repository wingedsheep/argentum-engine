package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Farrel's Zealot (Fallen Empires).
 *
 * The card is the corpus's first user of `AbilityFlag.ASSIGNS_NO_COMBAT_DAMAGE`, so what these
 * tests are really about is that flag: an unblocked 2/2 that takes the trade deals its 3 damage to
 * a creature and *none* to the defending player, while declining leaves normal combat damage
 * intact. "Assigns no damage" is not prevention — nothing is dealt at all.
 */
class FarrelsZealotScenarioTest : ScenarioTestBase() {

    init {
        context("Farrel's Zealot — the unblocked trade") {

            test("taking the trade deals 3 to a creature and nothing to the player") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Farrel's Zealot", summoningSickness = false)
                    // A 3-toughness blocker that stays home, so it can be the damage target.
                    .withCardOnBattlefield(2, "Elvish Warrior")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Farrel's Zealot" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // The gate asks whether to take the trade first; targets are locked in only on
                // the "yes" branch, so a decline never asks for one.
                val warrior = game.findPermanent("Elvish Warrior")!!
                game.answerYesNo(true)
                game.selectTargets(listOf(warrior))
                game.resolveStack()

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("3 damage kills the 2/3 Warrior") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe false
                }
                withClue("the Zealot assigned no combat damage, so the defender is untouched") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("declining leaves ordinary combat damage") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Farrel's Zealot", summoningSickness = false)
                    .withCardOnBattlefield(2, "Elvish Warrior")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Farrel's Zealot" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                game.answerYesNo(false)
                game.resolveStack()

                withClue("the Warrior was never damaged") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe true
                }

                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                withClue("a declined trade is an ordinary unblocked 2/2") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }
        }
    }
}
