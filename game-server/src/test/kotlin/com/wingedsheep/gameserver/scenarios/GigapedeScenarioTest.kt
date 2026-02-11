package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * E2E scenario test for Gigapede's graveyard-based triggered ability.
 *
 * Card reference:
 * - Gigapede (3GG): 6/1 Creature - Insect
 *   "Shroud. At the beginning of your upkeep, if Gigapede is in your graveyard,
 *   you may discard a card. If you do, return Gigapede to its owner's hand."
 */
class GigapedeScenarioTest : ScenarioTestBase() {

    /**
     * Advance from UNTAP to UPKEEP and resolve the trigger on the stack,
     * stopping when a pending decision appears (the YesNo for the optional discard).
     */
    private fun ScenarioTestBase.TestGame.advanceToGigapedeTrigger() {
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

        // The trigger is now on the stack. Pass priority to resolve it,
        // stopping when the YesNo decision appears.
        var iterations = 0
        while (!hasPendingDecision() && iterations < 20) {
            val p = state.priorityPlayerId ?: break
            execute(PassPriority(p))
            iterations++
        }
    }

    init {
        context("Gigapede graveyard trigger") {
            test("discard returns Gigapede from graveyard to hand - auto discard with 1 card") {
                // When hand has exactly 1 card, discard is automatic (no card selection)
                val game = scenario()
                    .withPlayers("Gigapede Player", "Opponent")
                    .withCardInGraveyard(1, "Gigapede")
                    .withCardInHand(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                // Verify initial state
                withClue("Gigapede should start in graveyard") {
                    game.isInGraveyard(1, "Gigapede") shouldBe true
                }

                // Advance to upkeep and resolve the trigger
                game.advanceToGigapedeTrigger()

                // The ReflexiveTriggerEffect should present a YesNo decision
                withClue("Should have a pending yes/no decision for the optional discard") {
                    game.hasPendingDecision() shouldBe true
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }

                // Accept - with only 1 card in hand, the discard auto-completes
                game.answerYesNo(true)

                // Verify results - discard was automatic, Gigapede returned to hand
                withClue("Gigapede should now be in hand (returned from graveyard)") {
                    game.isInHand(1, "Gigapede") shouldBe true
                }
                withClue("Gigapede should NOT be in graveyard anymore") {
                    game.isInGraveyard(1, "Gigapede") shouldBe false
                }
                withClue("Forest should be in graveyard (discarded)") {
                    game.isInGraveyard(1, "Forest") shouldBe true
                }
                withClue("Hand should have 1 card (Gigapede)") {
                    game.handSize(1) shouldBe 1
                }
            }

            test("discard returns Gigapede to hand - player chooses which card to discard") {
                // When hand has multiple cards, player must choose which to discard
                val game = scenario()
                    .withPlayers("Gigapede Player", "Opponent")
                    .withCardInGraveyard(1, "Gigapede")
                    .withCardInHand(1, "Forest")
                    .withCardInHand(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                withClue("Hand should have 2 cards") {
                    game.handSize(1) shouldBe 2
                }

                game.advanceToGigapedeTrigger()

                withClue("Should have yes/no decision") {
                    game.hasPendingDecision() shouldBe true
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }

                // Accept the discard
                game.answerYesNo(true)

                // Now we should choose which card to discard
                withClue("Should have a pending card selection for discard") {
                    game.hasPendingDecision() shouldBe true
                    game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                }

                // Discard the Forest, keep the Swamp
                val forestInHand = game.findCardsInHand(1, "Forest")
                game.selectCards(forestInHand)

                // Verify results
                withClue("Gigapede should now be in hand") {
                    game.isInHand(1, "Gigapede") shouldBe true
                }
                withClue("Gigapede should NOT be in graveyard") {
                    game.isInGraveyard(1, "Gigapede") shouldBe false
                }
                withClue("Forest should be in graveyard (discarded)") {
                    game.isInGraveyard(1, "Forest") shouldBe true
                }
                withClue("Swamp should still be in hand") {
                    game.isInHand(1, "Swamp") shouldBe true
                }
                withClue("Hand should have 2 cards (Gigapede + Swamp)") {
                    game.handSize(1) shouldBe 2
                }
            }

            test("declining the discard leaves Gigapede in graveyard") {
                val game = scenario()
                    .withPlayers("Gigapede Player", "Opponent")
                    .withCardInGraveyard(1, "Gigapede")
                    .withCardInHand(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.advanceToGigapedeTrigger()

                withClue("Should have yes/no decision") {
                    game.hasPendingDecision() shouldBe true
                    game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                }

                // Decline the optional discard
                game.answerYesNo(false)

                // Verify Gigapede stays in graveyard
                withClue("Gigapede should still be in graveyard") {
                    game.isInGraveyard(1, "Gigapede") shouldBe true
                }
                withClue("Forest should still be in hand") {
                    game.isInHand(1, "Forest") shouldBe true
                }
                withClue("Hand size should be 1 (Forest unchanged)") {
                    game.handSize(1) shouldBe 1
                }
            }

            test("no trigger when Gigapede is on the battlefield") {
                val game = scenario()
                    .withPlayers("Gigapede Player", "Opponent")
                    .withCardOnBattlefield(1, "Gigapede")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                // Advance past upkeep to draw step - no trigger should fire
                game.passUntilPhase(Phase.BEGINNING, Step.DRAW)

                withClue("Gigapede on battlefield should NOT trigger graveyard ability") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("Gigapede should still be on the battlefield") {
                    game.isOnBattlefield("Gigapede") shouldBe true
                }
            }

            test("client state shows sourceZone for graveyard trigger on stack") {
                val game = scenario()
                    .withPlayers("Gigapede Player", "Opponent")
                    .withCardInGraveyard(1, "Gigapede")
                    .withCardInHand(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                // Advance to upkeep where the trigger fires
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // The trigger should be on the stack now. Get client state.
                val clientState = game.getClientState(1)

                // Find the triggered ability on the stack (name is "Gigapede trigger")
                val stackCards = clientState.cards.values.filter { card ->
                    card.name.contains("Gigapede") && card.sourceZone != null
                }

                withClue("Stack should contain Gigapede trigger with sourceZone GRAVEYARD") {
                    stackCards.isNotEmpty() shouldBe true
                    stackCards.first().sourceZone shouldBe "GRAVEYARD"
                }
            }
        }
    }
}
