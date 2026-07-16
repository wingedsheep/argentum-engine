package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Lantern Flare (VOW #23) — {1}{W} Instant with Cleave {X}{R}{W}.
 *
 * "Lantern Flare deals X damage to target creature or planeswalker and you gain X life.
 *  [X is the number of creatures you control.]"
 *
 * Cleave (CR 702.148) removes the bracketed words when the alternative cost is paid. Uniquely, the
 * cleave cost itself carries {X}, so the two modes read X from different sources:
 *  - Printed cast ({1}{W}, brackets present): X = the number of creatures you control
 *    ([DynamicAmounts.creaturesYouControl]).
 *  - Cleaved cast ({X}{R}{W}, brackets removed): X = the value paid for the {X} in the cleave cost,
 *    threaded through `castSpellWithCleave(xValue = …)` to `DynamicAmount.XValue`.
 *
 * X is observed two ways per mode: the life gained (read exactly off the life total) and the death
 * of a target whose toughness sits right at the X boundary — the two modes are set up so the board
 * creature-count and the paid X differ, proving each mode reads X from its own source.
 */
class LanternFlareScenarioTest : ScenarioTestBase() {

    init {
        context("Lantern Flare — printed cast (brackets present): X = creatures you control") {

            test("X equals the three creatures you control: kills a 3/3 and gains 3 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lantern Flare")
                    .withLandsOnBattlefield(1, "Plains", 2) // {1}{W}
                    // Three creatures I control → printed X = 3.
                    .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false)
                    .withCardOnBattlefield(1, "Goblin Guide", summoningSickness = false)
                    .withCardOnBattlefield(1, "Llanowar Elves", summoningSickness = false)
                    // Opponent's 3/3 target: dies to exactly 3, survives 2 — pins X = 3.
                    .withCardOnBattlefield(2, "Centaur Courser", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.findPermanent("Centaur Courser")!!
                val lifeBefore = game.getLifeTotal(1)

                val cast = game.castSpell(1, "Lantern Flare", targetId = target)
                withClue("printed cast is legal: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("3 damage (creatures I control) destroyed the 3/3") {
                    game.isOnBattlefield("Centaur Courser") shouldBe false
                    game.isInGraveyard(2, "Centaur Courser") shouldBe true
                }
                withClue("gained 3 life (equal to X)") {
                    game.getLifeTotal(1) shouldBe lifeBefore + 3
                }
            }
        }

        context("Lantern Flare — cleaved cast (brackets removed): X = the amount paid") {

            test("X equals the value paid, ignoring the board: kills a 2/2 and gains 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lantern Flare")
                    // Cleave {X}{R}{W}; pay X=2 → {2}{R}{W}. Mountain gives R, Plains gives W.
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    // Only ONE creature I control — so the paid X (2) is distinct from the board count.
                    .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false)
                    // Opponent's 2/2 target: dies to 2, would survive the board-count of 1 — pins X = 2.
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.findPermanent("Grizzly Bears")!!
                val lifeBefore = game.getLifeTotal(1)

                val cast = game.castSpellWithCleave(1, "Lantern Flare", targetId = target, xValue = 2)
                withClue("cleaved cast with X=2 is legal: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("X=2 damage (the amount paid, not the 1 creature I control) destroyed the 2/2") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("gained X=2 life") {
                    game.getLifeTotal(1) shouldBe lifeBefore + 2
                }
            }
        }
    }
}
