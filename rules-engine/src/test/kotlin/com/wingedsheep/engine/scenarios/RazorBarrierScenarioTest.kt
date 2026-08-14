package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Razor Barrier (MRD #17).
 *
 * {1}{W} Instant
 * "Target permanent you control gains protection from artifacts or from the color of your choice
 *  until end of turn."
 *
 * The spell is *not* modal (no printed "Choose one —"), so the artifacts-or-a-color pick is made on
 * resolution via `Effects.ChooseAction`. These tests pin both branches:
 *
 *  - the artifacts branch grants `PROTECTION_FROM_CARDTYPE_ARTIFACT`, the new
 *    [com.wingedsheep.sdk.dsl.Effects.GrantProtectionFromCardType] facade's projected keyword —
 *    the same one the printed `Protection(ProtectionScope.CardType(...))` static produces, which is
 *    what targeting, blocking, and combat damage all read;
 *  - the colour branch nests `ChooseColorThen` → `GrantProtectionFromChosenColor`, so a *second*
 *    prompt appears only when that branch is taken;
 *  - and both are until-end-of-turn grants, not permanent ones.
 */
class RazorBarrierScenarioTest : ScenarioTestBase() {

    init {
        context("Razor Barrier") {

            test("the artifacts branch grants protection from artifacts") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInHand(1, "Razor Barrier")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Razor Barrier", bears).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseOptionDecision>()
                withClue("Two branches are offered: artifacts, or a colour of your choice") {
                    decision.options.size shouldBe 2
                }

                // Option [0] is the artifacts branch.
                game.submitDecision(OptionChosenResponse(decision.id, 0))

                withClue("Grizzly Bears has protection from artifacts") {
                    game.state.projectedState
                        .hasKeyword(bears, "PROTECTION_FROM_CARDTYPE_ARTIFACT") shouldBe true
                }
                withClue("No colour prompt follows the artifacts branch") {
                    game.hasPendingDecision() shouldBe false
                }
            }

            test("the colour branch prompts for a colour and grants protection from it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInHand(1, "Razor Barrier")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Razor Barrier", bears).error shouldBe null
                game.resolveStack()

                val branch = game.getPendingDecision()
                branch.shouldBeInstanceOf<ChooseOptionDecision>()
                // Option [1] is the "colour of your choice" branch.
                game.submitDecision(OptionChosenResponse(branch.id, 1))

                val colour = game.getPendingDecision()
                colour.shouldBeInstanceOf<ChooseColorDecision>()
                game.submitDecision(ColorChosenResponse(colour.id, Color.RED))

                val projected = game.state.projectedState
                withClue("Protection from the chosen colour, and only that colour") {
                    projected.hasKeyword(bears, "PROTECTION_FROM_RED") shouldBe true
                    projected.hasKeyword(bears, "PROTECTION_FROM_BLUE") shouldBe false
                }
                withClue("The colour branch does not also grant protection from artifacts") {
                    projected.hasKeyword(bears, "PROTECTION_FROM_CARDTYPE_ARTIFACT") shouldBe false
                }
            }

            test("the grant wears off at end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInHand(1, "Razor Barrier")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Razor Barrier", bears).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseOptionDecision>()
                game.submitDecision(OptionChosenResponse(decision.id, 0))

                game.state.projectedState
                    .hasKeyword(bears, "PROTECTION_FROM_CARDTYPE_ARTIFACT") shouldBe true

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                withClue("Until end of turn — gone by the next turn's upkeep") {
                    game.state.projectedState
                        .hasKeyword(bears, "PROTECTION_FROM_CARDTYPE_ARTIFACT") shouldBe false
                }
            }

            test("a noncreature permanent you control is a legal target") {
                // "Target permanent you control", not "target creature" — protecting a land or an
                // artifact is a printed use of the card.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bonesplitter")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInHand(1, "Razor Barrier")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bonesplitter = game.findPermanent("Bonesplitter")!!
                game.castSpell(1, "Razor Barrier", bonesplitter).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseOptionDecision>()
                game.submitDecision(OptionChosenResponse(decision.id, 0))

                withClue("The Equipment itself now has protection from artifacts") {
                    game.state.projectedState
                        .hasKeyword(bonesplitter, "PROTECTION_FROM_CARDTYPE_ARTIFACT") shouldBe true
                }
            }
        }
    }
}
