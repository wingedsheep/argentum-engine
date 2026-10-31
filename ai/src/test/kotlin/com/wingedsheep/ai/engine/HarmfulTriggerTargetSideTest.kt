package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.Sandworm
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A target slot the effect reads as [TargetPolarity.HARMFUL] breaks a simulation tie toward an
 * opponent's permanent — noncreature permanents included.
 *
 * 2026-10-31 server review, game a28f7a90 turn 11: Sandworm's "when this creature enters, destroy
 * target land" destroyed the AI's own Plains with five opposing lands (one a nonbasic) on offer.
 * Every land was tapped and the destroyed land's controller fetches a basic that enters tapped, so
 * the simulation scored every target the same; the last tie-break ranked a noncreature permanent
 * at 0 whichever side it was on, and the first legal target — ours — won.
 */
class HarmfulTriggerTargetSideTest : FunSpec({

    val cards = TestCards.all + listOf(Sandworm)
    val registry = CardRegistry().apply { register(cards) }

    test("Sandworm's enters trigger destroys an opponent's land, not ours") {
        val d = GameTestDriver().apply {
            registerCards(cards)
            initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        repeat(2) { d.putLandOnBattlefield(d.player1, "Plains") }
        repeat(3) { d.putLandOnBattlefield(d.player1, "Mountain") }
        // As in the game: the opponent's lands are tapped, ours get tapped paying for Sandworm,
        // and the destroyed land's controller fetches a basic that enters tapped — so no land the
        // trigger can hit changes the board the evaluator sees.
        repeat(2) { d.tapPermanent(d.putLandOnBattlefield(d.player2, "Plains")) }
        repeat(2) { d.tapPermanent(d.putLandOnBattlefield(d.player2, "Island")) }
        val worm = d.putCardInHand(d.player1, "Sandworm")
        d.castSpell(d.player1, worm)
        d.bothPass()

        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        TargetSelection.decisionPolarities(d.state, decision, IntentCatalog.of(registry)) shouldBe
            mapOf(0 to TargetPolarity.HARMFUL)
        // The misplay's precondition: our own lands are legal, and listed first.
        val legal = decision.legalTargets.getValue(0)
        d.state.projectedState.getController(legal.first()) shouldBe d.player1

        val response = AIPlayer.create(registry, d.player1, AiProfile.LIVE).respondToDecision(d.state, decision)
        val target = response.shouldBeInstanceOf<TargetsResponse>().selectedTargets.values.flatten()
        target.map { d.state.projectedState.getController(it) } shouldContainExactly listOf(d.player2)
    }
})
