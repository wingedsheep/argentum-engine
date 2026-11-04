package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.DeckSynergy
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ChosenTribeAiTest : ScenarioTestBase() {
    private fun card(name: String) = cardRegistry.getCard(name)!!
    private fun read(name: String) = SynergyAxes.of(card(name))

    init {
        test("a broad chosen type grant is an enabler, not an anthem or a changeling") {
            for (name in listOf("Arcane Adaptation", "Leyline of Transformation")) {
                val intent = CardIntentAnalyzer.analyze(card(name))
                intent.tags shouldContain IntentTag.TRIBAL_ENABLER
                intent.tags shouldNotContain IntentTag.ANTHEM
                intent.staticPriorValue shouldBeGreaterThan 0.5
                read(name).grantsChosenTribe shouldBe true
                read(name).everyTribe shouldBe false
                LimitedCardRater.explain(card(name)).heuristic.filter { it.part.startsWith("Static") }
                    .sumOf { it.value } shouldBeGreaterThan 0.0
            }
            for (name in listOf("Thran Portal", "Metallic Mimic", "Roaming Throne")) {
                read(name).grantsChosenTribe shouldBe false
                CardIntentAnalyzer.analyze(card(name)).tags shouldNotContain IntentTag.TRIBAL_ENABLER
            }
        }

        test("a converter needs off-tribe bodies and a payoff and does not feed itself") {
            val enabler = read("Arcane Adaptation")
            fun support(vararg names: String): Double = DeckSynergy.fedShare(
                enabler, DeckSynergy.counts(names.map(::read) + enabler)
            )
            support("Goblin King", "Grizzly Bears", "Grizzly Bears") shouldBe 0.4
            support("Grizzly Bears", "Grizzly Bears") shouldBe 0.0
            support("Goblin King", "Goblin Piker", "Goblin Piker") shouldBe 0.0
            support() shouldBe 0.0
            // Adding a second tribe does not let a single choice pretend to enable both tribes.
            support("Goblin King", "Lord of Atlantis", "Grizzly Bears") shouldBe 0.4
        }

        test("the AI chooses a payoff's absent type instead of the majority creature type") {
            val d = GameTestDriver()
            d.registerCards(TestCards.all + listOf(card("Arcane Adaptation"), card("Dragon Tempest")))
            d.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true)
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val me = d.activePlayer!!
            repeat(6) { d.putCreatureOnBattlefield(me, "Grizzly Bears") }
            // The payoff is in hand, so the choice has no immediate P/T benefit for simulation.
            // No printed Dragon exists anywhere: majority-type shortlisting used to miss it.
            d.putCardInHand(me, "Dragon Tempest")
            val adaptation = d.putCardInHand(me, "Arcane Adaptation")
            d.giveMana(me, Color.BLUE, 3)
            d.castSpell(me, adaptation)
            d.bothPass()
            val decision = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
            val response = AIPlayer.create(cardRegistry, me, AiProfile.PRODUCTION_CHOICES)
                .respondToDecision(d.state, decision).shouldBeInstanceOf<OptionChosenResponse>()
            decision.options[response.optionIndex] shouldBe "Dragon"
        }
    }
}
