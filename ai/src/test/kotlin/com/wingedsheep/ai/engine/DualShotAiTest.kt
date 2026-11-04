package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DualShotAiTest : ScenarioTestBase() {
    init {
        test("the atomic multi-target damage loop is understood as removal and a spell feeder") {
            val card = cardRegistry.getCard("Dual Shot")!!
            TargetPolarityAnalyzer.forSpell(card) shouldBe listOf(TargetPolarity.HARMFUL)
            CardIntentAnalyzer.analyze(card).tags shouldContain IntentTag.REMOVAL
            AiReadingAudit.read(card).abilities.flatMap { it.findings }.filter { !it.read } shouldBe emptyList()
            LimitedCardRater.explain(card).heuristic.single { it.part == "Spell" }.value shouldBeGreaterThan 0.0
            SynergyAxes.of(card).feeds shouldContain SynergyAxis.SPELLS
        }

        test("Dual Shot takes both opposing one-toughness creatures and leaves its own creature alone") {
            val game = scenario().withPlayers().withActivePlayer(1).withTurnNumber(5)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Dual Shot")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardOnBattlefield(2, "Birds of Paradise")
                .apply { repeat(8) { withCardInLibrary(1, "Mountain"); withCardInLibrary(2, "Plains") } }
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val cast = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
                .chooseAction(game.state).shouldBeInstanceOf<CastSpell>()
            cast.targets.shouldContainExactlyInAnyOrder(
                ChosenTarget.Permanent(game.findPermanent("Savannah Lions")!!),
                ChosenTarget.Permanent(game.findPermanent("Birds of Paradise")!!),
            )
        }

        test("Dual Shot leaves its second target unused when only its own creature could fill it") {
            val game = scenario().withPlayers().withActivePlayer(1).withTurnNumber(5)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInHand(1, "Dual Shot")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(2, "Savannah Lions")
                .apply { repeat(8) { withCardInLibrary(1, "Mountain"); withCardInLibrary(2, "Plains") } }
                .build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val cast = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
                .chooseAction(game.state).shouldBeInstanceOf<CastSpell>()
            cast.targets shouldBe listOf(ChosenTarget.Permanent(game.findPermanent("Savannah Lions")!!))
        }
    }
}
