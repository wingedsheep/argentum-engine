package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Rankle, Master of Pranks (ELD #101) — "Whenever Rankle deals combat damage to a player, choose
 * any number — • Each player discards a card. • Each player loses 1 life and draws a card. • Each
 * player sacrifices a creature of their choice."
 *
 * Pins the rulings: modes are chosen as the trigger goes on the stack (none is legal), the discard
 * mode has every player choose in APNAP order before any card leaves a hand, and the chosen modes
 * resolve in printed order.
 */
class RankleMasterOfPranksScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Rankle, Master of Pranks", summoningSickness = false)
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
        .withCardInHand(1, "Hill Giant")
        .withCardInHand(1, "Forest")
        .withCardInHand(2, "Grizzly Bears")
        .withCardInHand(2, "Forest")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.hitWithRankle() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Rankle, Master of Pranks" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
    }

    /**
     * Picks the modes whose description contains any of [modes], declining the rest. Every card
     * selection takes Hill Giant / Grizzly Bears (never Rankle or a Forest), and [onSelect] sees
     * each selection first.
     */
    private fun TestGame.resolveTrigger(
        modes: List<String>,
        onSelect: TestGame.(SelectCardsDecision) -> Unit = {},
    ) {
        val remaining = modes.toMutableList()
        var guard = 0
        while (guard++ < 30) {
            val decision = state.pendingDecision
            when {
                decision is ChooseOptionDecision -> {
                    val wanted = remaining.firstOrNull { m -> decision.options.any { it.contains(m) } }
                    val index = if (wanted != null) {
                        remaining.remove(wanted)
                        decision.options.indexOfFirst { it.contains(wanted) }
                    } else {
                        decision.options.indexOfFirst { it.contains("Don't choose", ignoreCase = true) }
                    }
                    submitDecision(OptionChosenResponse(decision.id, optionIndex = index)).error shouldBe null
                }
                decision is SelectCardsDecision -> {
                    onSelect(decision)
                    val pick = decision.options.filter {
                        state.getEntity(it)?.get<CardComponent>()?.name in setOf("Hill Giant", "Grizzly Bears")
                    }.take(decision.minSelections)
                    selectCards(pick).error shouldBe null
                }
                decision != null -> error("unexpected decision: $decision")
                state.stack.isNotEmpty() -> resolveStack()
                else -> return
            }
        }
        error("did not settle")
    }

    init {
        test("all three modes: each player discards, loses 1 and draws, then sacrifices") {
            val game = board()
            game.hitWithRankle()
            game.resolveTrigger(listOf("discards", "loses 1 life", "sacrifices"))

            withClue("each player discarded the chosen card, then drew the Island") {
                game.isInGraveyard(1, "Hill Giant") shouldBe true
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.handSize(1) shouldBe 2
                game.handSize(2) shouldBe 2
            }
            withClue("each player lost 1 life on top of Rankle's 3 combat damage") {
                game.getLifeTotal(1) shouldBe 19
                game.getLifeTotal(2) shouldBe 16
            }
            withClue("each player sacrificed a creature of their choice") {
                game.isOnBattlefield("Rankle, Master of Pranks") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isOnBattlefield("Hill Giant") shouldBe false
            }
        }

        test("the discard mode collects every choice before any card is discarded") {
            val game = board()
            game.hitWithRankle()
            val choosers = mutableListOf<com.wingedsheep.sdk.model.EntityId>()
            game.resolveTrigger(listOf("discards")) { decision ->
                choosers += decision.playerId
                withClue("no card has left a hand while choices are still being made") {
                    handSize(1) shouldBe 2
                    handSize(2) shouldBe 2
                }
            }

            withClue("the active player chose first, then the opponent") {
                choosers shouldBe listOf(game.player1Id, game.player2Id)
            }
            game.isInGraveyard(1, "Hill Giant") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.handSize(1) shouldBe 1
            game.handSize(2) shouldBe 1
            withClue("no other mode was chosen") {
                game.getLifeTotal(1) shouldBe 20
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }

        test("choosing no modes does nothing") {
            val game = board()
            game.hitWithRankle()
            game.resolveTrigger(emptyList())

            game.getLifeTotal(2) shouldBe 17
            game.handSize(1) shouldBe 2
            game.handSize(2) shouldBe 2
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
