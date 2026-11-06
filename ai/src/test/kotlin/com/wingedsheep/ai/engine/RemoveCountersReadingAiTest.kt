package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.akh.cards.DefiantGreatmaw
import com.wingedsheep.mtg.sets.definitions.akh.cards.ExemplarOfStrength
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A counter of a named kind taken off a target (`RemoveCountersEffect`, `RemoveAllCountersOfTypeEffect`)
 * does to its holder the opposite of putting it on: Defiant Greatmaw's "remove a -1/-1 counter from
 * another target creature you control" is a gift — a pump that lasts. And the -1/-1 counters
 * Greatmaw and Exemplar of Strength put on a creature *you control* are their drawback, not removal.
 */
class RemoveCountersReadingAiTest : FunSpec({
    test("removing a -1/-1 counter reads as a gift, and -1/-1 counters on our own creature as no removal") {
        val trigger = DefiantGreatmaw.script.triggeredAbilities[1]
        TargetPolarityAnalyzer.forRequirements(trigger.effect, trigger.allTargetRequirements) shouldBe
            listOf(TargetPolarity.BENEFICIAL)
        CardIntentAnalyzer.analyzeEffect(trigger.effect, trigger.allTargetRequirements).tags shouldBe setOf(IntentTag.PUMP)
        for (card in listOf(DefiantGreatmaw, ExemplarOfStrength)) {
            withClue(card.name) { CardIntentAnalyzer.analyze(card).tags shouldNotContain IntentTag.REMOVAL }
        }
        val reading = AiReadingAudit.read(DefiantGreatmaw)
        withClue("$reading") { reading.readFully shouldBe true }
    }

    test("Defiant Greatmaw's trigger takes the -1/-1 counter off our wounded creature") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + DefiantGreatmaw)
            initMirrorMatch(Deck.of("Forest" to 30))
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val player = d.activePlayer!!
        val wounded = d.putCreatureOnBattlefield(player, "Centaur Courser")
        d.putCreatureOnBattlefield(player, "Savannah Lions")
        d.replaceState(d.state.updateEntity(wounded) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withCounters(CounterType.MINUS_ONE_MINUS_ONE, 2))
        })
        val greatmaw = d.putCardInHand(player, "Defiant Greatmaw")
        d.giveMana(player, Color.GREEN, 3)
        d.castSpell(player, greatmaw).error shouldBe null
        d.bothPass()
        // The enters trigger: its two -1/-1 counters go on the Greatmaw itself, which fires the second trigger.
        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitTargetSelection(player, listOf(greatmaw)).error shouldBe null
        d.bothPass()

        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val simulator = GameSimulator(d.cardRegistry)
        val responder = DecisionResponder(simulator, AIPlayer.defaultEvaluator())
        simulator.decisionResolver = { state, next -> responder.respond(state, next, next.playerId) }
        val response = responder.respond(d.state, decision, player).shouldBeInstanceOf<TargetsResponse>()
        withClue("Savannah Lions has no counter to lose; the Courser gets back to 2/2") {
            response.selectedTargets.values.flatten() shouldBe listOf(wounded)
        }
    }
})
