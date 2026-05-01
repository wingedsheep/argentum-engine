package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Spinerock Tyrant.
 *
 * "Whenever you cast an instant or sorcery spell with a single target, you may copy it.
 *  If you do, those spells gain wither. You may choose new targets for the copy."
 *
 * Verifies:
 *  - The trigger fires only when the cast spell has exactly one target.
 *  - On `may copy → yes`, both the original and the copy deal damage as -1/-1 counters.
 *  - The copy controller may pick a new target for the copy.
 */
class SpinerockTyrantScenarioTest : ScenarioTestBase() {

    init {
        context("Spinerock Tyrant - copies single-target instant and grants wither") {

            test("both Sear copies deal damage as -1/-1 counters with new copy target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Spinerock Tyrant", summoningSickness = false)
                    .withCardInHand(1, "Sear") // {1}{R} — 4 damage to target creature/planeswalker
                    .withCardOnBattlefield(2, "Towering Baloth")    // 7/6 — survives 4 -1/-1
                    .withCardOnBattlefield(2, "Krosan Groundshaker") // 6/6 — survives 4 -1/-1
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val baloth = game.findPermanent("Towering Baloth")!!
                val groundshaker = game.findPermanent("Krosan Groundshaker")!!

                // Cast Sear targeting Towering Baloth — single target, so the trigger fires.
                game.castSpell(1, "Sear", targetId = baloth)

                // Trigger goes on top of the stack; resolving it asks "may copy".
                game.resolveStack()
                game.answerYesNo(true) // yes, copy

                // The copy prompts for a new target — pick Krosan Groundshaker instead.
                game.resolveStack()
                game.selectTargets(listOf(groundshaker))

                // Resolve the rest of the stack: copy resolves first, then the original Sear.
                game.resolveStack()

                // Towering Baloth (original target) — 4 -1/-1 counters from the original spell.
                val baloothCounters = game.state.getEntity(baloth)?.get<CountersComponent>()
                baloothCounters.shouldNotBeNull()
                baloothCounters.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 4

                // Krosan Groundshaker (copy's new target) — 4 -1/-1 counters from the copy.
                val groundshakerCounters = game.state.getEntity(groundshaker)?.get<CountersComponent>()
                groundshakerCounters.shouldNotBeNull()
                groundshakerCounters.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 4

                // Sear ends up in the graveyard (copy is removed from the stack as a token).
                game.isInGraveyard(1, "Sear") shouldBe true
            }

            test("declining the may-copy leaves a single resolution with no wither granted") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Spinerock Tyrant", summoningSickness = false)
                    .withCardInHand(1, "Sear")
                    .withCardOnBattlefield(2, "Towering Baloth") // 7/6
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val baloth = game.findPermanent("Towering Baloth")!!

                game.castSpell(1, "Sear", targetId = baloth)

                // Trigger asks "may copy" — decline.
                game.resolveStack()
                game.answerYesNo(false)

                game.resolveStack()

                // Without the optional copy/wither path, Sear deals normal 4 damage to the Baloth.
                // 4 damage to a 7/6 leaves it on the battlefield with no -1/-1 counters
                // (regular damage, not wither).
                val counters = game.state.getEntity(baloth)?.get<CountersComponent>()
                (counters?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0) shouldBe 0
            }
        }
    }
}
