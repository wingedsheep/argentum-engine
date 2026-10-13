package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Archangel of Wrath — "Kicker {B} and/or {R}" (two kicker abilities, CR 702.33b) with one enters
 * trigger for "if it was kicked" and another for "if it was kicked twice". Paying either kicker
 * fires only the first; paying both fires both, each with its own target.
 */
class ArchangelOfWrathScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Archangel of Wrath")
        .withLandsOnBattlefield(1, "Plains", 4)
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Cast through the server-offered action for [variant] (null = unkicked), then resolve everything, aiming every trigger at the opponent. */
    private fun TestGame.castAndResolve(variant: String?): Int {
        val cardId = findCardsInHand(1, "Archangel of Wrath").single()
        val actions = getLegalActions(1).filter { (it.action as? CastSpell)?.cardId == cardId }
        val chosen = if (variant == null) {
            actions.single { it.actionType == "CastSpell" }
        } else {
            actions.single { it.actionType == "CastWithKicker" && it.description.endsWith("($variant)") }
        }
        execute(chosen.action).error shouldBe null
        var triggersTargeted = 0
        var guard = 0
        while (guard++ < 50) {
            when (val decision = getPendingDecision()) {
                is ChooseTargetsDecision -> {
                    decision.legalTargets.getValue(0).contains(player2Id) shouldBe true
                    submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(player2Id)))).error shouldBe null
                    triggersTargeted++
                }
                is OrderObjectsDecision ->
                    submitDecision(OrderedResponse(decision.id, decision.objects)).error shouldBe null
                null -> if (state.stack.isEmpty()) return triggersTargeted else passPriority().error shouldBe null
                else -> error("Unexpected decision $decision")
            }
        }
        error("stack did not settle")
    }

    init {
        test("offers one kicked cast per kicker combination") {
            val game = board()
            game.getLegalActions(1).filter { it.actionType == "CastWithKicker" }.map { it.description } shouldContainExactly
                listOf(
                    "Cast Archangel of Wrath (Kicked {B})",
                    "Cast Archangel of Wrath (Kicked {R})",
                    "Cast Archangel of Wrath (Kicked {B} + {R})",
                )
        }

        test("unkicked: no damage trigger") {
            val game = board()
            game.castAndResolve(null) shouldBe 0
            game.isOnBattlefield("Archangel of Wrath") shouldBe true
            game.getLifeTotal(2) shouldBe 20
            game.getLifeTotal(1) shouldBe 20
        }

        test("kicked with {B} only: one trigger deals 2") {
            val game = board()
            game.castAndResolve("Kicked {B}") shouldBe 1
            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 22
        }

        test("kicked with {R} only: one trigger deals 2") {
            val game = board()
            game.castAndResolve("Kicked {R}") shouldBe 1
            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 22
        }

        test("kicked twice: both triggers fire, 2 damage each") {
            val game = board()
            game.castAndResolve("Kicked {B} + {R}") shouldBe 2
            game.getLifeTotal(2) shouldBe 16
            game.getLifeTotal(1) shouldBe 24
            game.isOnBattlefield("Archangel of Wrath") shouldBe true
        }
    }
}
