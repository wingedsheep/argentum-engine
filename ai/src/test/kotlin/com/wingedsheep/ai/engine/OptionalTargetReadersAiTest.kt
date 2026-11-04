package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class OptionalTargetReadersAiTest : ScenarioTestBase() {
    init {
        test("graveyard to library chooses the valuable creature instead of removal ranking") {
            val cards = listOf("Forest", "Mortuary Mire", "Grizzly Bears", "Serra Angel").map { cardRegistry.getCard(it)!! }
            val d = GameTestDriver().apply {
                registerCards(cards)
                initMirrorMatch(Deck.of("Forest" to 30), skipMulligans = true, startingPlayer = 0)
                passPriorityUntil(Step.PRECOMBAT_MAIN)
            }
            d.putCardInGraveyard(d.player1, "Grizzly Bears")
            val angel = d.putCardInGraveyard(d.player1, "Serra Angel")
            val mire = d.putCardInHand(d.player1, "Mortuary Mire")
            d.playLand(d.player1, mire).error shouldBe null
            val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            val answer = AIPlayer.create(d.cardRegistry, d.player1, AiProfile.LIVE)
                .respondToDecision(d.state, decision).shouldBeInstanceOf<TargetsResponse>()
            answer.selectedTargets.values.flatten().single() shouldBe angel
        }

        test("graveyard topdeck effects read as recursion rather than removal") {
            for (name in listOf("Aphetto Vulture", "Mortuary Mire", "Mystic Sanctuary")) {
                val definition = cardRegistry.getCard(name)!!
                withClue(name) {
                    (IntentTag.RECURSION in CardIntentAnalyzer.analyze(definition).tags) shouldBe true
                    (IntentTag.REMOVAL in CardIntentAnalyzer.analyze(definition).tags) shouldBe false
                    AiReadingAudit.read(definition).abilities.flatMap { it.findings }.filter { !it.read } shouldBe emptyList()
                }
            }
        }

        test("controller chosen tap or untap reads both sides without confusing sequential operations") {
            for (name in listOf("Stinging Lionfish", "Inverted Iceberg")) {
                withClue(name) {
                    AiReadingAudit.read(cardRegistry.getCard(name)!!).abilities.flatMap { it.findings }
                        .filter { !it.read } shouldBe emptyList()
                }
            }
            val sequential = card("Sequential tap untap reader control") {
                manaCost = "{U}"
                typeLine = "Instant"
                spell {
                    val target = target(TargetFilter.Creature)
                    effect = Effects.Tap(target) then Effects.Untap(target)
                }
            }
            TargetPolarityAnalyzer.forSpell(sequential) shouldBe listOf(TargetPolarity.UNKNOWN)
        }

        test("dynamic base replacement stays flexible because it can grow or shrink its target") {
            val ability = cardRegistry.getCard("Exuberant Wolfbear")!!.script.triggeredAbilities.single()
            TargetPolarityAnalyzer.forRequirements(ability.effect, ability.allTargetRequirements) shouldBe
                listOf(TargetPolarity.FLEXIBLE)
        }

        test("own token exile drawback does not become an offensive removal intent") {
            val shaun = cardRegistry.getCard("Shaun, Father of Synths")!!
            val leaves = shaun.script.triggeredAbilities.last()
            CardIntentAnalyzer.analyzeEffect(leaves.effect, leaves.allTargetRequirements).tags shouldBe emptySet()
            AiReadingAudit.read(shaun).abilities.flatMap { it.findings }.filter { !it.read } shouldBe emptyList()
        }
    }
}
