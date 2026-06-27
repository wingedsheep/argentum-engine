package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Don & Leo, Problem Solvers (TMT).
 *
 * "At the beginning of your end step, exile up to one target artifact you control and up to one
 *  target creature you control. Then return them to the battlefield under their owners' control."
 *
 * Regression guard for the two-optional-target fizzle: declining the (slot 0) artifact target and
 * choosing only the (slot 1) creature target must NOT fizzle the ability. Before the fix, the
 * resumer compacted the chosen-targets list to `[creature]` while keeping the full
 * `[artifact, creature]` requirement list, so resolution-time validation (CR 608.2b) checked the
 * creature against the *artifact* requirement, found it illegal, and fizzled with "all targets
 * invalid" — the creature was never blinked.
 */
class DonAndLeoProblemSolversScenarioTest : ScenarioTestBase() {

    init {
        context("Don & Leo, Problem Solvers end-step blink") {

            test("exiling only a creature (declining the artifact) blinks the creature instead of fizzling") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Don & Leo, Problem Solvers")
                    .withCardOnBattlefield(1, "Centaur Courser") // p1's creature; no artifact in play
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creatureBefore = game.findPermanent("Centaur Courser")
                    ?: error("Centaur Courser should start on the battlefield")

                // Advance to the end step; the trigger pauses for target selection.
                game.passUntilPhase(Phase.ENDING, Step.END)

                val decision = game.getPendingDecision() as? ChooseTargetsDecision
                    ?: error("Don & Leo's end-step trigger should prompt for targets")

                // Choose ONLY the creature slot (the artifact slot is left declined). Find the slot
                // whose legal targets include the creature so we don't depend on requirement ordering.
                val creatureSlot = decision.legalTargets.entries.first { creatureBefore in it.value }.key
                val result = game.submitDecision(
                    TargetsResponse(decision.id, mapOf(creatureSlot to listOf(creatureBefore)))
                )
                withClue("Choosing only a creature must not error: ${result.error}") {
                    result.error shouldBe null
                }
                val resolveResults = game.resolveStack()
                val events = resolveResults.flatMap { it.events }.map { it::class.simpleName }

                // The ability must RESOLVE (exile + return), not fizzle. Before the fix it fizzled with
                // "all targets invalid": the creature was validated against the artifact requirement.
                // A successful resolution emits the exile/return ZoneChangeEvents; a fizzle emits an
                // AbilityFizzledEvent and no zone changes.
                withClue("Don & Leo's ability must not fizzle when only a creature is chosen (events=$events)") {
                    events shouldNotContain "AbilityFizzledEvent"
                }
                withClue("The exile-and-return should have moved the creature (events=$events)") {
                    events shouldContain "ZoneChangeEvent"
                }
                withClue("Centaur Courser is on the battlefield after the blink (events=$events)") {
                    (game.findPermanent("Centaur Courser") != null) shouldBe true
                }
            }
        }
    }
}
