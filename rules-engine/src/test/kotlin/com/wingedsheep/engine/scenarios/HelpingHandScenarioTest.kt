package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Helping Hand (LCI #17, {W} Sorcery).
 *
 * "Return target creature card with mana value 3 or less from your graveyard to the battlefield tapped."
 *
 * Tests:
 *  1. A creature card with mana value ≤ 3 (Centaur Courser, MV 3) moves from the graveyard
 *     to the battlefield and enters tapped.
 *  2. A creature card with mana value > 3 (Force of Nature, MV 5) is not a legal target —
 *     the CastSpell action is rejected and the card remains in the graveyard.
 */
class HelpingHandScenarioTest : ScenarioTestBase() {
    init {
        context("Helping Hand") {

            // ------------------------------------------------------------------
            // Test 1: Valid target (MV = 3, exactly at the cap) — moved from
            //         graveyard to battlefield, enters tapped.
            // ------------------------------------------------------------------
            test("returns a creature with mana value 3 from graveyard to battlefield tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Helping Hand")
                    .withCardInGraveyard(1, "Centaur Courser") // {2}{G}, MV 3 — exactly at the cap
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingGraveyardCard(1, "Helping Hand", 1, "Centaur Courser")
                game.resolveStack()

                withClue("Centaur Courser should be on the battlefield after reanimation") {
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                }
                withClue("Centaur Courser should no longer be in the graveyard") {
                    game.isInGraveyard(1, "Centaur Courser") shouldBe false
                }
                val id = game.findPermanent("Centaur Courser")
                    ?: error("Centaur Courser not found on battlefield")
                withClue("Centaur Courser must enter the battlefield tapped") {
                    (game.state.getEntity(id)?.has<TappedComponent>() ?: false) shouldBe true
                }
            }

            // ------------------------------------------------------------------
            // Test 2: Invalid target (MV = 5, above the cap) — CastSpell
            //         is rejected; card stays in graveyard.
            // ------------------------------------------------------------------
            test("cannot target a creature card with mana value greater than 3") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Helping Hand")
                    .withCardInGraveyard(1, "Force of Nature") // {3}{G}{G}, MV 5 — above the cap
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.castSpellTargetingGraveyardCard(1, "Helping Hand", 1, "Force of Nature")

                withClue("Targeting a MV-5 creature should be rejected") {
                    result.isSuccess shouldBe false
                }
                withClue("Force of Nature should remain in the graveyard") {
                    game.isInGraveyard(1, "Force of Nature") shouldBe true
                }
            }
        }
    }
}
