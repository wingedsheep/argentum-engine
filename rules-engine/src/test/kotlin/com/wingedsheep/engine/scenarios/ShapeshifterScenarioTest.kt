package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Shapeshifter (ATQ #64).
 *
 * {6} Artifact Creature — Shapeshifter, star / 7-star
 * "As this creature enters, choose a number between 0 and 7. At the beginning of your upkeep, you
 *  may choose a number between 0 and 7. Its power is the last chosen number and its toughness is 7
 *  minus that number."
 *
 * Verifies the as-it-enters choice fixes P/T as power = chosen, toughness = 7 − chosen (so they
 * always sum to 7), across multiple chosen values.
 */
class ShapeshifterScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        fun castAndChoose(chosen: Int): Pair<TestGame, com.wingedsheep.sdk.model.EntityId> {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Shapeshifter")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Shapeshifter").error shouldBe null
            game.resolveStack()
            withClue("entering Shapeshifter prompts the number choice") {
                (game.getPendingDecision() != null) shouldBe true
            }
            game.chooseNumber(chosen)
            game.resolveStack()
            return game to game.findPermanent("Shapeshifter")!!
        }

        // Cast Shapeshifter choosing [entry] as it enters, then walk to player 1's next upkeep and
        // surface the optional "you may choose a number" trigger. Returns the game paused on that
        // trigger's YesNo "may" decision and the Shapeshifter permanent.
        fun castThenReachUpkeepChoice(entry: Int): Pair<TestGame, com.wingedsheep.sdk.model.EntityId> {
            val builder = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Shapeshifter")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            // Stock both libraries so the draw steps across the extra turns don't deck a player.
            repeat(15) {
                builder.withCardInLibrary(1, "Mountain")
                builder.withCardInLibrary(2, "Mountain")
            }
            val game = builder.build()

            game.castSpell(1, "Shapeshifter").error shouldBe null
            game.resolveStack()
            game.chooseNumber(entry)          // as-enters choice
            game.resolveStack()
            val shifter = game.findPermanent("Shapeshifter")!!

            // Advance to player 1's next upkeep (skipping the opponent's upkeep, which doesn't fire
            // "your upkeep"), then resolve up to the optional trigger's "may" prompt.
            var guard = 0
            while (guard++ < 12) {
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                if (game.state.activePlayerId == game.player1Id) break
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            }
            game.resolveStack()
            return game to shifter
        }

        context("Shapeshifter") {

            test("choosing 5 makes it a 5/2") {
                val (game, shifter) = castAndChoose(5)
                val p = stateProjector.project(game.state)
                withClue("power = 5") { p.getPower(shifter) shouldBe 5 }
                withClue("toughness = 7 - 5 = 2") { p.getToughness(shifter) shouldBe 2 }
            }

            test("choosing 6 makes it a 6/1") {
                val (game, shifter) = castAndChoose(6)
                val p = stateProjector.project(game.state)
                withClue("power = 6") { p.getPower(shifter) shouldBe 6 }
                withClue("toughness = 7 - 6 = 1") { p.getToughness(shifter) shouldBe 1 }
            }

            test("choosing 7 makes it a 7/0, which dies to state-based actions (0 toughness)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shapeshifter")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(1, "Shapeshifter").error shouldBe null
                game.resolveStack()
                game.chooseNumber(7)
                game.resolveStack()
                withClue("a 7/0 has 0 toughness and is put into the graveyard by SBAs") {
                    game.findPermanent("Shapeshifter") shouldBe null
                }
            }

            test("choosing 0 makes it a 0/7, and power+toughness always sums to 7") {
                val (game, shifter) = castAndChoose(0)
                val p = stateProjector.project(game.state)
                withClue("power = 0") { p.getPower(shifter) shouldBe 0 }
                withClue("toughness = 7") { p.getToughness(shifter) shouldBe 7 }
                ((p.getPower(shifter) ?: 0) + (p.getToughness(shifter) ?: 0)) shouldBe 7
            }

            // The number is chosen as a true "As ~ enters" replacement (CR 614.1c), not an ETB
            // trigger: the choice is presented while the spell resolves, BEFORE the permanent is on
            // the battlefield — so it never briefly sits at its default P/T as a real permanent.
            test("the number choice is an as-enters replacement: prompted before the creature is on the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shapeshifter")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Shapeshifter").error shouldBe null
                game.resolveStack()

                withClue("the number choice is pending") { (game.getPendingDecision() != null) shouldBe true }
                withClue("the creature has NOT entered yet — the choice precedes entry") {
                    game.findPermanent("Shapeshifter") shouldBe null
                }

                game.chooseNumber(4)
                game.resolveStack()

                val shifter = game.findPermanent("Shapeshifter")!!
                val p = stateProjector.project(game.state)
                withClue("once it enters it is already 4/3") {
                    p.getPower(shifter) shouldBe 4
                    p.getToughness(shifter) shouldBe 3
                }
            }

            // The upkeep re-choice is the behavior that distinguishes ChooseNumberForSource from a
            // one-shot as-enters choice: each upkeep re-chooses, and the CDA reads the *last* chosen
            // number, so P/T changes turn over turn.
            test("re-choosing at the upkeep makes the last chosen number the new P/T") {
                val (game, shifter) = castThenReachUpkeepChoice(entry = 2)

                val before = stateProjector.project(game.state)
                withClue("still a 2/5 from the entry choice before re-choosing") {
                    before.getPower(shifter) shouldBe 2
                    before.getToughness(shifter) shouldBe 5
                }

                // The upkeep trigger surfaces the number choice directly (the `optional = true` flag
                // raises no separate decline prompt for this no-target trigger shape; see below).
                game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
                game.chooseNumber(6)
                game.resolveStack()

                val after = stateProjector.project(game.state)
                withClue("the last chosen number (6) drives P/T: 6/1") {
                    after.getPower(shifter) shouldBe 6
                    after.getToughness(shifter) shouldBe 1
                }
            }

            // The card prints "you **may** choose a number"; mechanically this trigger always
            // resolves the choice (the optional flag is inert for a no-target/no-else trigger), and
            // needs no decline path — re-selecting the current number is equivalent to keeping it, so
            // the previous P/T is retained.
            test("re-choosing the same number at the upkeep keeps the P/T") {
                val (game, shifter) = castThenReachUpkeepChoice(entry = 3)

                game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
                game.chooseNumber(3)
                game.resolveStack()

                val p = stateProjector.project(game.state)
                withClue("re-picking the entry value (3) keeps 3/4") {
                    p.getPower(shifter) shouldBe 3
                    p.getToughness(shifter) shouldBe 4
                }
            }
        }
    }
}
