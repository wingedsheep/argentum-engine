package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * The payoff behind a gate ([com.wingedsheep.sdk.scripting.effects.GatedEffect]): "you may pay {E}.
 * If you do, …", "you may discard a card. If you do, …", "you may sacrifice a Food. If you do, …
 * Otherwise, …", and a reflexive "when you do, …".
 *
 * The shared effect walk stops at these gates, so every card built on one read as doing nothing:
 * Pia's tutor had no intent, and Pippin's Bravery — a pump either way the Food goes — had no target
 * polarity, so its slot kept the removal ranking and the heuristic fill pumped the opponent's best
 * creature. The readers now read the gate's `then` and `otherwise`; the gate's own cost is the
 * price, not the point, and stays unread.
 */
class GatedEffectReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("an if-you-do payoff is what the card is for; the cost before it is not") {
            withClue("Pia: you may discard a card; if you do, search for an enchantment") {
                val tags = CardIntentAnalyzer.analyzeEffect(card("Pia, Aether Ascetic").script.triggeredAbilities.single().effect).tags
                tags shouldContain IntentTag.TUTOR
                tags shouldNotContain IntentTag.DISCARD
            }
            withClue("Entrust the Spark: you may sacrifice a planeswalker; if you do, search for one") {
                intents.forName("Entrust the Spark")!!.tags shouldContain IntentTag.TUTOR
            }
            withClue("Pippin's Bravery: +4/+4 with a Food, +2/+2 without — an instant pump either way") {
                intents.forName("Pippin's Bravery")!!.tags shouldContain IntentTag.COMBAT_TRICK
            }
        }

        test("a may-pay payoff aimed at a target gives that target its polarity") {
            withClue("Pippin's Bravery's slot reads off both branches, which agree") {
                TargetPolarityAnalyzer.forSpell(card("Pippin's Bravery")) shouldBe listOf(TargetPolarity.BENEFICIAL)
            }
            withClue("Serene Steward: you may pay {W}; if you do, a +1/+1 counter on target creature") {
                val trigger = card("Serene Steward").script.triggeredAbilities.single()
                TargetPolarityAnalyzer.forRequirements(trigger.effect, listOfNotNull(trigger.targetRequirement)) shouldBe
                    listOf(TargetPolarity.BENEFICIAL)
            }
        }

        test("a punisher's branches are the opponent's call: only what both do is read") {
            // Risk Factor: "target opponent may have it deal 4 damage to them; if they don't, you
            // draw three cards" — which branch happens is the opponent's call, and only one of them
            // touches the slot, so it stays unread.
            TargetPolarityAnalyzer.forSpell(card("Risk Factor")) shouldBe listOf(TargetPolarity.UNKNOWN)
            // Clash of Elements: "its owner may put it on top of their library … if they didn't,
            // the bottom" — either branch tucks it (PunisherGateReadingAiTest).
            TargetPolarityAnalyzer.forSpell(card("Clash of Elements")) shouldBe listOf(TargetPolarity.HARMFUL)
        }

        // The heuristic fill is the AI's cheap path: the baseline every committed cast starts from,
        // and the whole decision wherever there is no budget to simulate each candidate.
        test("the heuristic fill aims Pippin's Bravery at our creature, not theirs") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(6)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardInHand(1, "Pippin's Bravery")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Serra Angel")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val enumerator = LegalActionEnumerator(
                services.cardRegistry, services.manaSolver, services.costCalculator,
                services.predicateEvaluator, services.conditionEvaluator, services.turnManager,
            )
            val cast = enumerator.enumerate(game.state, game.player1Id).first {
                (it.action as? CastSpell)?.let { c -> game.state.getEntity(c.cardId)?.get<CardComponent>()?.name } == "Pippin's Bravery"
            }

            val filled = TargetSelection.fillHeuristically(
                game.state, cast, game.player1Id, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = true,
            ) as CastSpell
            val pumped = filled.targets.filterIsInstance<ChosenTarget.Permanent>().single().entityId
            withClue("the pump behind the Food gate reads as a gift, so it goes on our Grizzly Bears") {
                game.state.getEntity(pumped)?.get<CardComponent>()?.name shouldBe "Grizzly Bears"
            }
        }
    }
}
