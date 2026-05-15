package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Starfield Vocalist.
 *
 * Card reference:
 * - Starfield Vocalist ({3}{U}): 3/4 Creature — Human Bard
 *   If a permanent entering the battlefield causes a triggered ability of a permanent
 *   you control to trigger, that ability triggers an additional time.
 *   Warp {1}{U}
 *
 * Differs from Naban / Traveling Chocobo in that the entering permanent does NOT have to
 * be under your control — opponent ETBs that tickle your "whenever a creature enters"
 * triggers also get doubled. This exercises the `enteringMustBeYouControl = false` path
 * on `AdditionalETBTriggers`.
 */
class StarfieldVocalistScenarioTest : ScenarioTestBase() {

    init {
        context("Starfield Vocalist") {

            test("opponent's creature ETB doubles your Wretched Anurid trigger") {
                // Vocalist + Wretched Anurid on my battlefield. Opponent casts Grizzly Bears.
                // Wretched Anurid says "whenever a creature enters the battlefield, you lose 1 life"
                // — it triggers regardless of who controls the entering creature. With Vocalist,
                // it should trigger twice (losing 2 life total).
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Starfield Vocalist")
                    .withCardOnBattlefield(1, "Wretched Anurid")
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(2, "Grizzly Bears")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve creature spell — Anurid trigger fires (twice, because of Vocalist)
                game.resolveStack()

                withClue("Player 1 should lose 2 life (Anurid trigger doubled by Vocalist)") {
                    game.getLifeTotal(1) shouldBe 18
                }
            }

            test("without Starfield Vocalist, opponent's creature triggers Wretched Anurid only once") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wretched Anurid")
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(2, "Grizzly Bears")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Player 1 should lose 1 life (no doubler)") {
                    game.getLifeTotal(1) shouldBe 19
                }
            }

            test("Vocalist sees its own ETB — Anurid trigger from Vocalist's own entrance is doubled") {
                // Per the EOE ruling: "If a permanent entering the battlefield at the same time as
                // Starfield Vocalist (including Starfield Vocalist itself) causes a triggered ability
                // of a permanent you control to trigger, that ability triggers an additional time."
                //
                // Setup: Wretched Anurid in play. Cast Starfield Vocalist. Anurid says "whenever
                // another creature enters, you lose 1 life" — Vocalist is "another" relative to
                // Anurid, so Anurid triggers off Vocalist's own ETB. Vocalist's static ability is
                // active by the time triggers are detected (it's on the battlefield), so the trigger
                // gets doubled → 2 life lost.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Wretched Anurid")
                    .withCardInHand(1, "Starfield Vocalist")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Starfield Vocalist")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Player 1 should lose 2 life (Vocalist's own ETB doubles Anurid trigger)") {
                    game.getLifeTotal(1) shouldBe 18
                }
            }

            test("Naban does NOT double a Wizard entering under opponent's control (enteringMustBeYouControl default preserved)") {
                // Tightly isolates the `enteringMustBeYouControl = true` default. Opponent casts
                // Fleeting Aven — a Bird Wizard whose only triggered ability fires on cycling,
                // so its ETB adds no noise. Naban's `enteringFilter` accepts it (it IS a Wizard),
                // so the ONLY thing keeping Naban from doubling the Anurid trigger is the default
                // controller restriction. If a future change flipped the default to false, this
                // test would fail: Anurid would fire twice for 2 life lost.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Naban, Dean of Iteration")
                    .withCardOnBattlefield(1, "Wretched Anurid")
                    .withCardInHand(2, "Fleeting Aven")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(2, "Fleeting Aven")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Player 1 should lose only 1 life — Naban does not double opponent's Wizard ETB") {
                    game.getLifeTotal(1) shouldBe 19
                }
            }
        }
    }
}
