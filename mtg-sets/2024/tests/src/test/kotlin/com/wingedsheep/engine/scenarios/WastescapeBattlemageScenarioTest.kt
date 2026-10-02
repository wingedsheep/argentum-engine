package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Wastescape Battlemage — {1}{C} 2/2, "Kicker {G} and/or {1}{U}", with one "when you cast this
 * spell" trigger linked to each kicker (CR 702.33b / 702.33f). Covers all four declarations: none,
 * {G} only, {1}{U} only, and both — each offered by the server as its own cast.
 */
class WastescapeBattlemageScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Wastescape Battlemage")
        .withLandsOnBattlefield(1, "Snow-Covered Wastes", 1)
        .withLandsOnBattlefield(1, "Forest", 1)
        .withLandsOnBattlefield(1, "Island", 3)
        // Own enchantment: never a legal "an opponent controls" target.
        .withCardOnBattlefield(1, "Glorious Anthem")
        .withCardOnBattlefield(2, "Glorious Anthem")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.opponentsAnthem() =
        state.getBattlefield().single { id ->
            state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Glorious Anthem" &&
                state.projectedState.getController(id) == player2Id
        }

    /** Cast through the server-offered action whose label names [variant], then answer its prompts. */
    private fun TestGame.castVariant(variant: String?) {
        val actions = getLegalActions(1).filter {
            (it.action as? CastSpell)?.cardId == findCardsInHand(1, "Wastescape Battlemage").single()
        }
        val chosen = if (variant == null) {
            actions.single { it.actionType == "CastSpell" }
        } else {
            actions.single { it.actionType == "CastWithKicker" && it.description.endsWith("($variant)") }
        }
        execute(chosen.action).error shouldBe null
        while (true) {
            when (val decision = getPendingDecision()) {
                is OrderObjectsDecision -> submitDecision(OrderedResponse(decision.id, decision.objects)).error shouldBe null
                is ChooseTargetsDecision -> {
                    // Each trigger has exactly one legal target: the opponent's permanent.
                    val legal = decision.legalTargets.getValue(0)
                    legal.size shouldBe 1
                    submitDecision(TargetsResponse(decision.id, mapOf(0 to legal))).error shouldBe null
                }
                null -> return
                else -> error("Unexpected decision $decision")
            }
        }
    }

    init {
        test("offers a cast per kicker combination, each priced with exactly the kickers it pays") {
            val game = board()
            val kicked = game.getLegalActions(1).filter { it.actionType == "CastWithKicker" }
            kicked.map { it.description } shouldContainExactly listOf(
                "Cast Wastescape Battlemage (Kicked {G})",
                "Cast Wastescape Battlemage (Kicked {1}{U})",
                "Cast Wastescape Battlemage (Kicked {G} + {1}{U})",
            )
            kicked.map { (it.action as CastSpell).declaredCostIndices } shouldContainExactly
                listOf(setOf(0), setOf(1), setOf(0, 1))
            kicked.map { it.manaCostString } shouldContainExactly
                listOf("{1}{C}{G}", "{2}{C}{U}", "{2}{C}{G}{U}")
        }

        test("unkicked: neither cast trigger fires") {
            val game = board()
            game.castVariant(null)
            game.state.stack.size shouldBe 1
            game.resolveStack()
            game.isOnBattlefield("Wastescape Battlemage") shouldBe true
            game.findPermanents("Glorious Anthem").size shouldBe 2
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("kicked with its {G} kicker only: exiles the opponent's artifact or enchantment, no bounce") {
            val game = board()
            val anthem = game.opponentsAnthem()
            game.castVariant("Kicked {G}")
            game.state.stack.size shouldBe 2
            game.resolveStack()
            game.isInExile(2, "Glorious Anthem") shouldBe true
            game.state.getBattlefield().contains(anthem) shouldBe false
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Wastescape Battlemage") shouldBe true
            // The permanent remembers which kicker it was kicked with (CR 702.33f), and only that one.
            game.state.getEntity(game.findPermanent("Wastescape Battlemage")!!)
                ?.get<com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent>()
                ?.chosen?.keys?.filter { it == ChoiceSlot.FIRST_KICKER || it == ChoiceSlot.SECOND_KICKER } shouldBe
                listOf(ChoiceSlot.FIRST_KICKER)
        }

        test("kicked with its {1}{U} kicker only: bounces the opponent's creature, no exile") {
            val game = board()
            game.castVariant("Kicked {1}{U}")
            game.state.stack.size shouldBe 2
            game.resolveStack()
            game.isInHand(2, "Grizzly Bears") shouldBe true
            game.findPermanents("Glorious Anthem").size shouldBe 2
            game.isOnBattlefield("Wastescape Battlemage") shouldBe true
        }

        test("kicked with both: both linked triggers fire") {
            val game = board()
            game.castVariant("Kicked {G} + {1}{U}")
            game.state.stack.size shouldBe 3
            game.resolveStack()
            game.isInExile(2, "Glorious Anthem") shouldBe true
            game.isInHand(2, "Grizzly Bears") shouldBe true
            game.findPermanents("Glorious Anthem").size shouldBe 1
            game.isOnBattlefield("Wastescape Battlemage") shouldBe true
        }
    }
}
