package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Breeches, Eager Pillager — "Whenever a Pirate you control attacks, choose one that hasn't been
 * chosen this turn — • Create a Treasure token. • Target creature can't block this turn. • Exile
 * the top card of your library. You may play it this turn."
 *
 * Exercises the new turn-scoped modal memory ([ModalEffect.chooseOneNotYetChosenThisTurn] /
 * ChosenModesThisTurnComponent):
 *  - each attacking Pirate triggers once;
 *  - a mode chosen earlier THIS TURN is excluded from a later trigger's offer;
 *  - the exclusion resets on the controller's next turn (cleanup clears the memory).
 */
class BreechesEagerPillagerScenarioTest : ScenarioTestBase() {

    private val treasureMode = "Create a Treasure token"
    private val cantBlockMode = "Target creature can't block this turn"
    private val impulseMode = "Exile the top card of your library. You may play it this turn"

    /** Resolve the stack until a modal ChooseOptionDecision surfaces. */
    private fun TestGame.resolveToModeChoice(): ChooseOptionDecision {
        var guard = 0
        while (getPendingDecision() !is ChooseOptionDecision && guard++ < 20) {
            resolveStack()
        }
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        return decision as ChooseOptionDecision
    }

    private fun TestGame.chooseMode(decision: ChooseOptionDecision, description: String) {
        val index = decision.options.indexOf(description)
        check(index >= 0) { "Mode '$description' not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    private fun TestGame.treasureCount(): Int =
        state.getBattlefield()
            .mapNotNull { state.getEntity(it) }
            .count { it.get<CardComponent>()?.name == "Treasure" }

    init {
        test("each attacking Pirate triggers; a mode chosen this turn is excluded from the next trigger") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Breeches, Eager Pillager", tapped = false, summoningSickness = false)
                .withCardOnBattlefield(1, "Kitesail Corsair", tapped = false, summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears", tapped = false, summoningSickness = false)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .withLifeTotal(2, 20)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            // Two Pirates attack → two independent triggers this turn.
            game.declareAttackers(
                mapOf("Breeches, Eager Pillager" to 2, "Kitesail Corsair" to 2)
            ).error shouldBe null

            // First trigger: all three modes are offered; take the Treasure mode.
            val first = game.resolveToModeChoice()
            first.options.size shouldBe 3
            first.options shouldContain treasureMode
            game.chooseMode(first, treasureMode)
            withClue("Treasure mode created a Treasure token") {
                game.treasureCount() shouldBe 1
            }

            // Second trigger (the other Pirate): the Treasure mode was already chosen THIS TURN,
            // so only the remaining two modes are offered.
            val second = game.resolveToModeChoice()
            second.options.size shouldBe 2
            second.options shouldNotContain treasureMode
            second.options shouldContain cantBlockMode

            // The can't-block mode targets a creature — pick the opponent's Bears.
            game.chooseMode(second, cantBlockMode)
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("still only one Treasure — the second trigger did not re-create one") {
                game.treasureCount() shouldBe 1
            }
        }

        test("the per-turn memory resets — all three modes are available again next turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Breeches, Eager Pillager", tapped = false, summoningSickness = false)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Swamp")
                .withLifeTotal(2, 40)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val startTurn = game.state.turnNumber

            // Turn 1: Breeches attacks, choose the Treasure mode.
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Breeches, Eager Pillager" to 2)).error shouldBe null
            val t1 = game.resolveToModeChoice()
            t1.options.size shouldBe 3
            game.chooseMode(t1, treasureMode)
            game.resolveStack()

            // Advance through cleanup, the opponent's turn, and back to Breeches's next combat.
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN) // leave turn 1 combat
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)        // opponent's turn combat
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN) // leave opponent combat
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)        // my next turn combat

            withClue("should now be Player1's later turn") {
                game.state.activePlayerId shouldBe game.player1Id
                (game.state.turnNumber > startTurn) shouldBe true
            }

            // Turn 3: attack again — the Treasure mode is offered once more (memory was cleared).
            game.declareAttackers(mapOf("Breeches, Eager Pillager" to 2)).error shouldBe null
            val t3 = game.resolveToModeChoice()
            withClue("all three modes available again next turn") {
                t3.options.size shouldBe 3
                t3.options shouldContain treasureMode
                t3.options shouldContain impulseMode
            }
        }
    }
}
