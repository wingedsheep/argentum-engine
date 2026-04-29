package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Gathering Stone.
 *
 * Card reference:
 * - Gathering Stone ({4}): Artifact
 *   "As this artifact enters, choose a creature type.
 *    Spells you cast of the chosen type cost {1} less to cast.
 *    When this artifact enters and at the beginning of your upkeep, look at the top
 *    card of your library. If it's a card of the chosen type, you may reveal it and
 *    put it into your hand. If you don't put the card into your hand, you may put
 *    it into your graveyard."
 *
 * Focus: cost-reduction-by-chosen-subtype is the new mechanic exercised here.
 * The CostCalculator previously stubbed `HasChosenSubtype` to `true`; this test
 * pins down that chosen-type spells get the {1} discount and others don't.
 */
class GatheringStoneScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseCreatureType(typeName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    /**
     * Drive past the look-at-top ETB trigger by alternating between resolving the
     * stack and skipping any SelectFromCollection prompts that appear. This handles
     * the case where the trigger goes on the stack first and needs both players to
     * pass priority before its decisions appear.
     */
    private fun TestGame.skipLookAtTopChain() {
        var safety = 0
        while (safety < 8) {
            if (state.pendingDecision != null) {
                skipSelection()
            } else if (state.stack.isNotEmpty()) {
                resolveStack()
            } else {
                return
            }
            safety++
        }
    }

    init {
        context("Gathering Stone - cost reduction for chosen-type spells") {

            test("spells of the chosen creature type cost {1} less") {
                // 5 Forests: {4} for Gathering Stone leaves 1 Forest for the Elf.
                // Elvish Vanguard is {1}{G}; with the {1} discount it costs {G},
                // which the remaining Forest can produce.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Gathering Stone")
                    .withCardInHand(1, "Elvish Vanguard")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castStone = game.castSpell(1, "Gathering Stone")
                withClue("Gathering Stone should cast: ${castStone.error}") {
                    castStone.error shouldBe null
                }
                game.resolveStack()
                game.chooseCreatureType("Elf")
                // ETB look-at-top: drain trigger + any "may put in graveyard" prompts.
                game.skipLookAtTopChain()

                // {1}{G} Elvish Vanguard should now cost only {G}
                val castElf = game.castSpell(1, "Elvish Vanguard")
                withClue("Elvish Vanguard should be castable for {G} after Gathering Stone discount: ${castElf.error}") {
                    castElf.error shouldBe null
                }
            }

            test("spells of a non-chosen creature type do not get the discount") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Gathering Stone")
                    .withCardInHand(1, "Goblin Sky Raider") // {2}{R} Goblin
                    .withLandsOnBattlefield(1, "Forest", 4) // {4} for Stone
                    .withLandsOnBattlefield(1, "Mountain", 2) // not enough for full {2}{R}
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castStone = game.castSpell(1, "Gathering Stone")
                castStone.error shouldBe null
                game.resolveStack()
                game.chooseCreatureType("Elf")
                game.skipLookAtTopChain()

                // Goblin (not Elf) is not discounted; player has only 2 mountains
                // so the Goblin's red+generic cost cannot all be paid from the open
                // mountains alone. We verify the player still has not cast it by
                // checking the legal action is rejected.
                val castGoblin = game.castSpell(1, "Goblin Sky Raider")
                withClue("Goblin Sky Raider should NOT be castable without enough mana (no discount applies)") {
                    (castGoblin.error != null) shouldBe true
                }
            }
        }
    }
}
