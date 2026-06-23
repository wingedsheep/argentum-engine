package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Feature test for the "choose a number, store it durably on the source, derive P/T from it as a
 * characteristic-defining ability" mechanism (powers Shapeshifter). Uses an inline test card that
 * runs `ChooseNumberForSource(0..7)` from an `EntersBattlefield` trigger and from an optional
 * upkeep trigger that re-chooses, with a `SetBasePowerToughnessDynamicStatic` CDA reading the
 * stored number (power = n, toughness = 7 - n).
 *
 * (The real Shapeshifter uses the `EntersWithChoice(ChoiceType.NUMBER)` as-enters replacement for
 * the entry choice — see `ShapeshifterScenarioTest`; here an ETB trigger drives the same effect to
 * keep the inline card minimal.)
 */
class ChooseNumberForSourceCdaScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    private val testShapeshifter = card("Test Numbershaper") {
        manaCost = "{6}"
        colorIdentity = ""
        typeLine = "Artifact Creature — Shapeshifter"
        power = 0
        toughness = 0
        oracleText = "As this enters and at the beginning of your upkeep, choose a number between 0 and 7. " +
            "Its power is the last chosen number and its toughness is 7 minus that number."

        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            effect = Effects.ChooseNumberForSource(minValue = 0, maxValue = 7)
        }

        triggeredAbility {
            trigger = Triggers.YourUpkeep
            optional = true
            effect = Effects.ChooseNumberForSource(minValue = 0, maxValue = 7)
        }

        staticAbility {
            ability = SetBasePowerToughnessDynamicStatic(
                power = DynamicAmount.CastChoice(ChoiceSlot.CHOSEN_NUMBER),
                toughness = DynamicAmount.Subtract(
                    DynamicAmount.Fixed(7),
                    DynamicAmount.CastChoice(ChoiceSlot.CHOSEN_NUMBER)
                ),
                filter = GroupFilter.source()
            )
        }
    }

    init {
        cardRegistry.register(testShapeshifter)

        context("Choose-a-number CDA") {

            test("ETB choice sets power to chosen number and toughness to 7 minus it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Numbershaper")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Numbershaper").error shouldBe null
                game.resolveStack()

                // The ETB OnEnterRunEffect pauses for the number choice.
                val decision = game.getPendingDecision()
                withClue("ETB should prompt for a number choice") { (decision != null) shouldBe true }
                game.chooseNumber(5)
                game.resolveStack()

                val shifter = game.findPermanent("Test Numbershaper")!!
                val projected = stateProjector.project(game.state)
                withClue("power = chosen 5") { projected.getPower(shifter) shouldBe 5 }
                withClue("toughness = 7 - 5") { projected.getToughness(shifter) shouldBe 2 }
            }

            test("choosing 0 derives 0/7 — the derivation holds across the range and always sums to 7") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Test Numbershaper")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Numbershaper").error shouldBe null
                game.resolveStack()
                game.chooseNumber(0)
                game.resolveStack()

                val shifter = game.findPermanent("Test Numbershaper")!!
                val projected = stateProjector.project(game.state)
                withClue("0 chosen -> 0/7") {
                    projected.getPower(shifter) shouldBe 0
                    projected.getToughness(shifter) shouldBe 7
                }
                withClue("power + toughness always sums to 7") {
                    ((projected.getPower(shifter) ?: 0) + (projected.getToughness(shifter) ?: 0)) shouldBe 7
                }
            }
        }
    }
}
