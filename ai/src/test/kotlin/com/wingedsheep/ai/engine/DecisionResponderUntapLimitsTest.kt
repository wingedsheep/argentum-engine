package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.UntapLimitPerStep
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DecisionResponderUntapLimitsTest : FunSpec({
    val landCap = card("AI Land Untap Cap") { typeLine = "Artifact"
        staticAbility { ability = UntapLimitPerStep(GameObjectFilter.Land, 1) } }
    val artifactCap = card("AI Artifact Untap Cap") { typeLine = "Enchantment"
        staticAbility { ability = UntapLimitPerStep(GameObjectFilter.Artifact, 1) } }
    val artifact = card("AI Untap Widget") { typeLine = "Artifact" }
    val artifactLand = card("AI Untap Artifact Land") { typeLine = "Artifact Land" }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(landCap, artifactCap, artifact, artifactLand))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    for (overlap in listOf(false, true)) {
        test("AI satisfies ${if (overlap) "overlapping" else "disjoint"} untap caps instead of submitting the lower bound") {
            val d = driver(); val me = d.activePlayer!!; val opponent = d.getOpponent(me)
            d.putPermanentOnBattlefield(me, landCap.name)
            d.putPermanentOnBattlefield(me, artifactCap.name)
            val land = d.putPermanentOnBattlefield(opponent, "Forest").also(d::tapPermanent)
            val widget = d.putPermanentOnBattlefield(opponent, artifact.name).also(d::tapPermanent)
            val additional = if (overlap) {
                listOf(d.putPermanentOnBattlefield(opponent, artifactLand.name).also(d::tapPermanent))
            } else {
                listOf(d.putPermanentOnBattlefield(opponent, "Forest").also(d::tapPermanent),
                    d.putPermanentOnBattlefield(opponent, artifact.name).also(d::tapPermanent))
            }
            d.passPriorityUntil(Step.UNTAP)
            val decision = d.pendingDecision as SelectCardsDecision
            val responder = DecisionResponder(GameSimulator(d.cardRegistry), AIPlayer.defaultEvaluator())
            val answer = responder.respond(d.state, decision, opponent) as CardsSelectedResponse
            d.submitCardSelection(opponent, answer.selectedCards).error shouldBe null
            // The AI must actually release at least one permanent, rather than keep everything tapped.
            (listOf(land, widget) + additional).any { !d.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
        }
    }
})
