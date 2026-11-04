package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

class OwnCreatureDamageSupportReadingAiTest : ScenarioTestBase() {
    init {
        test("own-side damage with a gift is flexible support, not removal") {
            val rile = cardRegistry.getCard("Rile")!!
            TargetPolarityAnalyzer.forSpell(rile) shouldBe listOf(TargetPolarity.FLEXIBLE)
            CardIntentAnalyzer.analyze(rile).tags shouldContain IntentTag.DRAW
            CardIntentAnalyzer.analyze(rile).tags shouldNotContain IntentTag.REMOVAL
            LimitedCardRater.explain(rile).heuristic.single { it.part == "Spell" }.value shouldBeLessThan 0.8
        }

        test("unrestricted mixed damage stays unread and ordinary burn stays removal") {
            val mixed = card("Unrestricted damage support reader fixture") {
                manaCost = "{R}"
                typeLine = "Sorcery"
                spell {
                    val creature = target(TargetFilter.Creature)
                    effect = Effects.DealDamage(1, creature) then Effects.GrantKeyword(Keyword.TRAMPLE, creature)
                }
            }
            TargetPolarityAnalyzer.forSpell(mixed) shouldBe listOf(TargetPolarity.UNKNOWN)
            CardIntentAnalyzer.analyze(mixed).tags shouldContain IntentTag.REMOVAL
            val shock = cardRegistry.getCard("Shock")!!
            TargetPolarityAnalyzer.forSpell(shock) shouldBe listOf(TargetPolarity.HARMFUL)
            CardIntentAnalyzer.analyze(shock).tags shouldContain IntentTag.REMOVAL
        }

        test("a pure own-side damage drawback remains understood without removal intent") {
            val bully = cardRegistry.getCard("Spiteful Bully")!!
            CardIntentAnalyzer.analyze(bully).tags shouldNotContain IntentTag.REMOVAL
            AiReadingAudit.read(bully).abilities.flatMap { it.findings }.filter { !it.read } shouldBe emptyList()
        }

        test("modal rating uses each mode's target restriction") {
            val own = cardRegistry.getCard("Rile")!!
            val unrestricted = card("Modal unrestricted damage reader fixture") {
                manaCost = "{R}"
                typeLine = "Sorcery"
                spell {
                    val creature = target(TargetFilter.Creature)
                    effect = Effects.DealDamage(1, creature) then
                        Effects.GrantKeyword(Keyword.TRAMPLE, creature) then Effects.DrawCards(1)
                }
            }
            fun modalValue(def: com.wingedsheep.sdk.model.CardDefinition): Double =
                LimitedCardRater.effectValue(ModalEffect(listOf(Mode(
                    effect = def.script.spellEffect!!,
                    targetRequirements = def.script.targetRequirements,
                ))))
            modalValue(own) shouldBeLessThan modalValue(unrestricted)
        }

        test("temporary theft costs less than permanent theft in the heuristic") {
            val theft = GainControlEffect(EffectTarget.ContextTarget(0))
            LimitedCardRater.effectValue(theft.copy(duration = Duration.EndOfTurn)) shouldBeLessThan
                LimitedCardRater.effectValue(theft)
        }

        test("Rile chooses a surviving creature over a one-toughness creature") {
            val game = scenario().withPlayers().withActivePlayer(1).withTurnNumber(5)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Rile")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(1, "Craw Wurm")
                .apply { repeat(8) { withCardInLibrary(1, "Mountain"); withCardInLibrary(2, "Plains") } }
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val action = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).chooseAction(game.state)
            withClue("chose $action") {
                (action is CastSpell) shouldBe true
                (action as CastSpell).targets shouldBe listOf(ChosenTarget.Permanent(game.findPermanent("Craw Wurm")!!))
            }
        }

        test("Rile does not kill its only creature merely to draw") {
            val game = scenario().withPlayers().withActivePlayer(1).withTurnNumber(5)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Rile")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .apply { repeat(8) { withCardInLibrary(1, "Mountain"); withCardInLibrary(2, "Plains") } }
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val action = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).chooseAction(game.state)
            withClue("chose $action") { (action is CastSpell) shouldBe false }
        }
    }
}
