package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.UlamogsDespoiler
import com.wingedsheep.mtg.sets.definitions.dft.cards.MimeoplasmReveredOne
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The as-enters processor — `EntersWithExileCounters` moving opponents' exiled cards to their
 * graveyards (Ulamog's Despoiler). The synergy reading is this feature's AI change; the decision
 * test pins that the generic yes/no and card-pick responders already answer the new two-step prompt
 * (simulating the yes through the pick) rather than declining it.
 */
class AsEntersProcessorAiTest : FunSpec({

    test("an as-enters processor rewards opponents' exiled cards; a linked exile doesn't") {
        SynergyAxes.of(UlamogsDespoiler).rewards shouldContain SynergyAxis.OPPONENT_EXILE
        SynergyAxes.of(MimeoplasmReveredOne).rewards shouldNotContain SynergyAxis.OPPONENT_EXILE
    }

    test("the AI processes two of the opponent's exiled cards so the Despoiler enters with four counters") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + UlamogsDespoiler)
            initMirrorMatch(Deck.of("Wastes" to 40), skipMulligans = true)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        repeat(3) { d.putCardInExile(opp, "Grizzly Bears") }
        val despoiler = d.putCardInHand(me, UlamogsDespoiler.name)
        d.giveColorlessMana(me, 6)
        d.castSpell(me, despoiler).error shouldBe null
        d.bothPass()

        val simulator = GameSimulator(d.cardRegistry)
        val responder = DecisionResponder(simulator, AIPlayer.defaultEvaluator())
        simulator.decisionResolver = { state, next -> responder.respond(state, next, next.playerId) }

        val ask = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        val yes = responder.respond(d.state, ask, me).shouldBeInstanceOf<YesNoResponse>()
        yes.choice shouldBe true
        d.submitDecision(me, yes).error shouldBe null

        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        val chosen = responder.respond(d.state, pick, me).shouldBeInstanceOf<CardsSelectedResponse>()
        chosen.selectedCards.size shouldBe 2
        d.submitDecision(me, chosen).error shouldBe null

        d.state.getEntity(despoiler)!!.get<CountersComponent>()!!
            .getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4
    }
})
